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

/**
 * This is to be an audio renderer for JavaSound api.
 *
 * Just a null one atm.
 * @author notzed
 */
public class JavaAudioRenderer {

	JavaAudioFrame dummy = new JavaAudioFrame();

	class JavaAudioFrame extends AudioFrame {

		@Override
		void enqueue() throws InterruptedException {
			// nop
		}

		@Override
		void recycle() {
			// nop
		}
	}

	public AudioFrame getFrame() {
		return dummy;
	}

	public void start() {
	}
}
