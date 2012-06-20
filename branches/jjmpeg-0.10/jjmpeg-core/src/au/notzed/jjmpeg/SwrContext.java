/*
 * Copyright (c) 2012 Michael Zucchi
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
 * Used to convert between formats and optionally scale at the same time.
 *
 * @author notzed
 */
public class SwrContext extends SwrContextAbstract {

	protected SwrContext(int p) {
		setNative(new SwrContextNative32(this, p));
	}

	protected SwrContext(long p) {
		setNative(new SwrContextNative64(this, p));
	}

	static public SwrContext create(long dstLayout, SampleFormat dstFormat, int dstRate,
			long srcLayout, SampleFormat srcFormat, int srcRate) {

		return SwrContextNative.alloc(dstLayout, dstFormat.toC(), dstRate, srcLayout, srcFormat.toC(), srcRate);
	}

	public int convert(AVFrame dst, AVFrame src) {
		return n.convert(dst.n, src.n);
	}
}

class SwrContextNative extends SwrContextNativeAbstract {

	public SwrContextNative(AVObject o) {
		super(o);
	}

	static native SwrContext alloc(long dstLayout, int dstFormat, int dstRate, long srcLayout, int srcFormat, int srcRate);

	native int convert(AVFrameNative dst, AVFrameNative src);

	native void free();
}

class SwrContextNative32 extends SwrContextNative {

	int p;

	SwrContextNative32(AVObject o, int p) {
		super(o);
		this.p = p;
	}

	@Override
	public void dispose() {
		if (p != 0) {
			free();
			p = 0;
		}
		super.dispose();
	}
}

class SwrContextNative64 extends SwrContextNative {

	long p;

	SwrContextNative64(AVObject o, long p) {
		super(o);
		this.p = p;
	}

	@Override
	public void dispose() {
		if (p != 0) {
			free();
			p = 0;
		}
		super.dispose();
	}
}
