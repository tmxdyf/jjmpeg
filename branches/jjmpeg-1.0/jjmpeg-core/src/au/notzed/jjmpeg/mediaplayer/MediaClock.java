/*
 * Copyright (c) 2013 Michael Zucchi
 *
 * This file is part of jjmpeg.
 *
 * jjmpeg is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jjmpeg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jjmpeg.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.notzed.jjmpeg.mediaplayer;

import java.util.ArrayList;

/**
 * Master timer for audio/video sync.
 *
 * @author notzed
 */
public class MediaClock {

	// True "zero" time.
	private long startms;
	private long audiosetms;
	private long audioms;
	private long audiolag = 10;
	private boolean paused;
	private long pausedPosition;
	private long seekmsa = -1;
	private long seekmsv = -1;
	private long seekms;
	private boolean seeking;
	private int sequence = 0;

	public interface MediaClockListener {

		public void clockPause();

		public void clockResume();

		public void clockSeekStart();

		public void clockSeekFinish(long pos);
	}
	MediaClockListener audio;
	MediaClockListener video;

	public MediaClock() {
		setAudioPosition(0);
	}

	public synchronized int getSequence() {
		return sequence;
	}

	public void setAudioOut(MediaClockListener l) {
		audio = l;
	}

	public void setVideoOut(MediaClockListener l) {
		video = l;
	}

	public void reset() {
		audiosetms = System.currentTimeMillis();
		audioms = 0;
		startms = audiosetms - 0;
		paused = false;
		seeking = false;
		seekms = 0;
	}

	public synchronized long getMediaPosition() {
		if (paused) {
			return pausedPosition;
		} else {
			return getAudioClock(System.currentTimeMillis());
		}
	}

	/**
	 * Estimate of audio lag from system
	 * @param ms
	 */
	public void setAudioLag(long ms) {
		System.out.println("Maximum audio lag: " + ms);
		audiolag = ms;
	}

	public synchronized void setAudioPosition(long ms) {
		long now = System.currentTimeMillis();
		long diff = Math.abs(getAudioClock(now) - ms);

		if (diff < 50)
			return;

		System.out.print("set audio clock: " + ms);
		System.out.println("  jitter/drift: " + (getAudioClock(now) - ms));

		audiosetms = System.currentTimeMillis();
		audioms = ms;
		startms = audiosetms - ms;

		//System.out.printf("Set audio position %d  setms %d startms = %d\n", ms, audiosetms, startms);
	}

	public void seekStart() {
		synchronized (this) {
			sequence++;
			seeking = true;
		}
		if (video != null)
			video.clockSeekStart();
		if (audio != null)
			audio.clockSeekStart();
	}

	public void seekFinish(long ms) {
		synchronized (this) {
			seekmsa = ms;
			seekmsv = ms;
			seekms = ms;
			seeking = false;
			// Force position until we get an update
			audioms = ms;
			audiosetms = System.currentTimeMillis();
			startms = audiosetms - ms;
			notifyAll();
		}
		if (video != null)
			video.clockSeekFinish(ms);
		if (audio != null)
			audio.clockSeekFinish(ms);
	}

	public synchronized void seek(long ms) {
		seekmsa = ms;
		seekmsv = ms;
		seekms = ms;
		throw new RuntimeException("don't call this now");
	}

	public long getSeek() {
		return seekms;
	}

	public boolean isPaused() {
		return paused;
	}

	public void pause() {
		synchronized (this) {
			paused = true;
			pausedPosition = getAudioClock(System.currentTimeMillis());
		}
		if (video != null)
			video.clockPause();
		if (audio != null)
			audio.clockPause();
	}

	public void resume() {
		if (paused) {
			synchronized (this) {
				// 're-sync' audio location
				audiosetms = System.currentTimeMillis();
				audioms = pausedPosition;
				startms = audiosetms - pausedPosition;

				paused = false;
				notifyAll();
			}
			if (audio != null)
				audio.clockResume();
			if (video != null)
				video.clockResume();
		}
	}

	synchronized long getAudioClock(long now) {
		return (audioms + (now - audiosetms));
	}

	/**
	 * Go to sleep while paused, and handle seek.
	 * @throws InterruptedException
	 * @return true if a seek was performed.
	 */
	public synchronized boolean checkPauseAudio() throws InterruptedException {
		boolean sucked = seekmsa != -1;

		/**
		 * Both audio and video renderers need individual tracking
		 * for seek; easiest way was just to double up on the seekms
		 * variable.
		 */
		if (seekmsa != -1) {
			//setAudioPosition(seekmsa);
			seekmsa = -1;
		}
		while (paused | seeking) {
			wait();
		}

		return sucked;
	}

	public synchronized boolean checkPauseVideo() throws InterruptedException {
		boolean sucked = seekmsv != -1;

		if (seekmsv != -1) {
			// FIXME: only do this if video-only stream
			//setAudioPosition(seekmsv);
			seekmsv = -1;
		}
		while (paused | seeking) {
			wait();
		}

		return sucked;
	}

	public synchronized long getVideoDelay(long pts) {
		long now = System.currentTimeMillis();
		long delay, targetms;

		// Adjust now for audio location
		//now = (audioms + (now - audiosetms));
		targetms = pts + startms;
		delay = targetms - now;

		//System.out.printf("get video delay for pos %d now  %d targetms %d dleay %d\n", pts, now, targetms, delay);

		if (delay > 500) {
			System.out.println("weird delay " + delay);
		}

		return delay;
	}
}
