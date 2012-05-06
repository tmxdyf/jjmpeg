/*
 * Copyright (c) 2011 Michael Zucchi
 *
 * This file is part of jjmpeg, a java binding to ffmpeg's libraries.
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
package au.notzed.jjmpeg;

/**
 *
 * @author notzed
 */
public class AVCodec extends AVCodecAbstract {

	AVCodec(int p) {
		setNative(new AVCodecNative32(this, p));
	}

	AVCodec(long p) {
		setNative(new AVCodecNative64(this, p));
	}

	static public AVCodec findEncoder(int id) {
		return AVCodecNativeAbstract.find_encoder(id);
	}
}

abstract class AVCodecNative extends AVCodecNativeAbstract {

	public AVCodecNative(AVObject o) {
		super(o);
	}
}

class AVCodecNative32 extends AVCodecNative {

	int p;

	AVCodecNative32(AVObject o, int p) {
		super(o);
		this.p = p;
	}

	@Override
	public void dispose() {
		if (p != 0) {
			super.dispose();
			p = 0;
		}
	}
}

class AVCodecNative64 extends AVCodecNative {

	long p;

	AVCodecNative64(AVObject o, long p) {
		super(o);
		this.p = p;
	}

	@Override
	public void dispose() {
		if (p != 0) {
			super.dispose();
			p = 0;
		}
	}
}
