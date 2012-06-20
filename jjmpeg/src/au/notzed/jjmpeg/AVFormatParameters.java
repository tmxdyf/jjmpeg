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

import java.nio.ByteBuffer;

/**
 *
 * @author notzed
 */
public class AVFormatParameters extends AVFormatParametersAbstract {

	protected AVFormatParameters(int p) {
		setNative(new AVFormatParametersNative32(this, p));
	}

	protected AVFormatParameters(long p) {
		setNative(new AVFormatParametersNative64(this, p));
	}
}

class AVFormatParametersNative extends AVFormatParametersNativeAbstract {

	public AVFormatParametersNative(AVObject o) {
		super(o);
	}
}

class AVFormatParametersNative32 extends AVFormatParametersNative {

	int p;

	AVFormatParametersNative32(AVObject o, int p) {
		super(o);
		this.p = p;
	}
}

class AVFormatParametersNative64 extends AVFormatParametersNative {

	long p;

	AVFormatParametersNative64(AVObject o, long p) {
		super(o);
		this.p = p;
	}
}
