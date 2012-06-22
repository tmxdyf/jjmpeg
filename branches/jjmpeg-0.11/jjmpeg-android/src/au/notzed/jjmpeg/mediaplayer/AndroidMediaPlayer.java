/*
 * Copyright (c) 2012 Michael Zucchi
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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import au.notzed.jjmpeg.AVRational;
import au.notzed.jjmpeg.SampleFormat;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Media player activity, obviously.
 *
 * @author notzed
 */
public class AndroidMediaPlayer extends Activity implements MediaSink {

	GLVideoView view;
	SeekBar seek;
	//
	String filename;
	MediaReader reader;
	//
	AndroidAudioRenderer aRenderer;
	GLESVideoRenderer vRenderer;
	//
	AVRational videoTB;
	long videoStart;
	//
	int audioSampleRate;
	//
	VideoDecoder vd;
	AudioDecoder ad;
	//
	AudioTrack track;
	//
	boolean fingerDown;
	boolean updateSeek;
	boolean haveVideo;
	boolean haveAudio;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		FrameLayout fl = new FrameLayout(this);
		FrameLayout.LayoutParams fp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		view = new GLVideoView(this);

		fl.addView(view, fp);
		setContentView(fl);

		vRenderer = view.renderer;
		aRenderer = new AndroidAudioRenderer();

		Intent it = getIntent();
		System.out.println("intent action = " + it.getAction());
		System.out.println("intent datas = " + it.getDataString());
		System.out.println("intent data  = " + it.getData());

		if (it.getData() != null) {
			if (it.getData().getScheme().equals("content"))
				filename = getRealPathFromURI(it.getData());
			else
				filename = it.getDataString();
		} else {
			filename = "/sdcard/trailer.mp4";
			filename = "http://radio1.internode.on.net:8000/126";
			filename = "/sdcard/Ministry.of.Sound.The.annual.2003.avi";
		}

		seek = new SeekBar(this);
		fp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
		fl.addView(seek, fp);

		seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (!updateSeek) {
					reader.seek(progress, 0);
				}
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				fingerDown = true;
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				fingerDown = false;
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (!isStarted) {
			try {
				open(filename);
				reader.start();
				if (haveAudio)
					aRenderer.play();
				if (!haveVideo)
					view.stop();
				seek.postDelayed(updatePosition, 100);
				isStarted = true;
			} catch (IOException ex) {
				Logger.getLogger(MediaPlayer.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
	/**
	 * Tracks if we've been through the postPause state.
	 */
	boolean isPaused = false;
	boolean isStarted = false;

	@Override
	protected void onPause() {
		super.onPause();

		reader.pause();
		isPaused = true;
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (isPaused) {
			isPaused = false;
			reader.unpause();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		// Should this close down the threads/etc?
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		System.out.println("Destroy jjplayer");

		reader.cancel();
		aRenderer.release();
		reader = null;
	}
	// Polls the play position and updates the scrollbar
	Runnable updatePosition = new Runnable() {

		public void run() {
			if (reader != null) {
				if (!fingerDown) {
					updateSeek = true;
					seek.setProgress((int) vRenderer.getPosition());
					updateSeek = false;
				}
				seek.postDelayed(updatePosition, 100);
			}
		}
	};

	public String getRealPathFromURI(Uri contentUri) {
		String[] proj = {MediaStore.Images.Media.DATA,};
		Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);

		if (cursor == null)
			return contentUri.getPath();

		cursor.moveToFirst();

		int path = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

		System.out.println("path " + path + ": " + cursor.getString(path));
		String res = cursor.getString(path);

		cursor.close();

		return res;
	}

	void open(String file) throws IOException {
		reader = new MediaReader(file);
		reader.createDefaultDecoders(this);

		initRenderers();

		seek.setMax((int) reader.getDuration());
	}

	public void initRenderers() {
		for (MediaDecoder md : reader.streamMap.values()) {
			if (md instanceof VideoDecoder) {
				vd = (VideoDecoder) md;
				videoTB = vd.stream.getTimeBase();
				videoStart = vd.stream.getStartTime();

				vRenderer.setVideoSize(vd.width, vd.height);
				haveVideo = true;
			} else if (md instanceof AudioDecoder) {
				ad = (AudioDecoder) md;

				// Force stereo output for multi-channel data
				int cc = Math.min(2, ad.cc.getChannels());

				ad.setOutputFormat(3, cc, SampleFormat.SAMPLE_FMT_S16, ad.cc.getSampleRate());
				aRenderer.setAudioFormat(ad.cc.getSampleRate(), cc, ad.cc.getSampleFmt());
				haveAudio = true;
			}
		}
	}

	public long getDisplayTime() {
		return Math.max(0, clock);
	}
	Integer dummy = Integer.valueOf(0);
	long clock;
	long startms = -1;
	long seekoffset = 0;

	public void postPlay() {
	}

	public void postSeek(long stamp) {
		// TODO: none of this is used yet
		clock = stamp;
		startms = -1;
		seekoffset = stamp;

		vRenderer.postSeek(stamp);

		runOnUiThread(doUpdateSeek);
	}
	Runnable doUpdateSeek = new Runnable() {

		public void run() {
			System.out.println("post seek gui thread seek to " + seekoffset);
			updateSeek = true;
			seek.setProgress((int) seekoffset);
			updateSeek = false;
		}
	};

	public void postPause() {
		// could do something so it pauses immediately?
		aRenderer.pause();
	}

	public void postUnpause() {
		// ensure we re-sync delay
		startms = -1;
		aRenderer.play();
	}

	public void postFinished() {
		if (vRenderer != null) {
			vRenderer.stop();
			view.stop();
			aRenderer.stop();
			long start = (vRenderer.threadLast - vRenderer.thread) / 1000;
			System.err.printf(" GL thread finished cpu time = %d.%06ds\n", start / 1000000L, start % 1000000L);
		}
	}

	public VideoFrame getVideoFrame() throws InterruptedException {
		return vRenderer.getFrame();
	}

	public AudioFrame getAudioFrame() throws InterruptedException {
		return aRenderer.getFrame();
	}
}
