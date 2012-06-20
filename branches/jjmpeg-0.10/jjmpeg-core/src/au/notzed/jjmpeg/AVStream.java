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
public class AVStream extends AVStreamAbstract {

	protected AVStream(int p) {
		setNative(new AVStreamNative32(this, p));
	}

	protected AVStream(long p) {
		setNative(new AVStreamNative64(this, p));
	}
}

class AVStreamNative extends AVStreamNativeAbstract {

	public AVStreamNative(AVObject o) {
		super(o);
	}
}

class AVStreamNative32 extends AVStreamNative {

	int p;

	AVStreamNative32(AVObject o, int p) {
		super(o);
		this.p = p;
	}
}

class AVStreamNative64 extends AVStreamNative {

	long p;

	AVStreamNative64(AVObject o, long p) {
		super(o);
		this.p = p;
	}
}
