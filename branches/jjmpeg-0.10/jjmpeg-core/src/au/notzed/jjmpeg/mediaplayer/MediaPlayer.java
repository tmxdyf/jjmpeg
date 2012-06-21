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
 * @author notzed
 */
public interface MediaPlayer {

	public enum State {

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
		Paused
	};

	public interface Listener {
		void positionChanged(MediaPlayer player, long newpos);
	};

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
	public void seek(long ms);

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
	public State getState();
}
