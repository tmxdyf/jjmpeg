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

/**
 * High level player implementation API.
 *
 * Not sure I need this, MediaReader does all the work.
 * @author notzed
 */
public interface MediaPlayer {

	public enum MediaState {

		/**
		 * No file opened.
		 */
		Idle,
		/**
		 * Initial/initialising state
		 */
		Init,
		/**
		 * Ready to start playing
		 */
		Ready,
		/**
		 * Playing
		 */
		Playing,
		/**
		 * Paused
		 */
		Paused,
		/**
		 * Quit and finished, no further operations possible.
		 */
		Quit;
	};

	public enum Whence {

		Start {
			@Override
			long getPosition(long now, long target) {
				return target;
			}
		},
		Here {
			@Override
			long getPosition(long now, long target) {
				return now + target;
			}
		};

		abstract long getPosition(long now, long target);
	}

	public interface MediaListener {

		void mediaMoved(MediaPlayer player, long newpos);

		void mediaError(MediaPlayer player, Exception ex);

		void mediaState(MediaPlayer player, MediaState newstate);
	};

	/**
	 * Set the listener
	 * @param l
	 */
	public void setListener(MediaListener l);

	/**
	 * Start playing moved to playing state
	 */
	public void play();

	/**
	 * Pause if playing, move to paused state.
	 */
	public void pause();

	/**
	 * Stop playing, move to Ready state
	 */
	public void stop();

	/**
	 * Seek to position in milliseconds
	 * @param ms
	 */
	public void seek(long ms, Whence whence);

	/**
	 * Get current play position in milliseconds
	 * @return
	 */
	public long getPosition();

	/**
	 * Get duration in milliseconds
	 * @return
	 */
	public long getDuration();

	/**
	 * Get current player state
	 * @return
	 */
	public MediaState getMediaState();
}
