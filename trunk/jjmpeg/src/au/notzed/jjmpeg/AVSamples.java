/*
 * Copyright (c) 2011 Michael Zucchi
 *
 * This file is part of jjmpeg, a java binding to ffmpeg's libraries.
 *
 * jjmpeg is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jjmpeg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with jjmpeg.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.notzed.jjmpeg;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

class AVSamplesNative extends AVNative {

	public AVSamplesNative(AVObject jobject, ByteBuffer p) {
		super(jobject, p);
	}

	@Override
	public void dispose() {
		if (p != null) {
			super.dispose();
			_free(p);
		}
	}
}

/**
 * Holder for audio samples memory, which has some specific memory
 * allocation requirements.
 * @author notzed
 */
public class AVSamples extends AVObject {

	ShortBuffer s;

	public AVSamples() {
		setNative(new AVSamplesNative(this, AVNative._malloc(AVCodecContext.AVCODEC_MAX_AUDIO_FRAME_SIZE * 2)));
		s = n.p.asShortBuffer();
	}

	public ByteBuffer getBuffer() {
		return n.p;
	}

	public ShortBuffer getSamples() {
		return s;
	}
}
