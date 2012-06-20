/*
 * Copyright (C) 2001-2003 Michael Niedermayer <michaelni@gmx.at>
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
 * Used to convert between formats and optionally scale at the same time.
 *
 * @author notzed
 */
public class SwsContext extends SwsContextAbstract {

	int dstW;
	int dstH;
	PixelFormat dstFormat = PixelFormat.PIX_FMT_NONE;
	public static final int SWS_FAST_BILINEAR = 1;
	public static final int SWS_BILINEAR = 2;
	public static final int SWS_BICUBIC = 4;
	public static final int SWS_X = 8;
	public static final int SWS_POINT = 0x10;
	public static final int SWS_AREA = 0x20;
	public static final int SWS_BICUBLIN = 0x40;
	public static final int SWS_GAUSS = 0x80;
	public static final int SWS_SINC = 0x100;
	public static final int SWS_LANCZOS = 0x200;
	public static final int SWS_SPLINE = 0x400;

	protected SwsContext(int p) {
		setNative(new SwsContextNative32(this, p));
	}

	protected SwsContext(long p) {
		setNative(new SwsContextNative64(this, p));
	}

	static public SwsContext create(int srcW, int srcH, PixelFormat srcFormat, int dstW, int dstH, PixelFormat dstFormat, int flags) {
		SwsContext c = SwsContextNativeAbstract.getContext(srcW, srcH, srcFormat.toC(), dstW, dstH, dstFormat.toC(), flags, null, null, null);

		c.dstW = dstW;
		c.dstH = dstH;
		c.dstFormat = dstFormat;

		return c;
	}

	public int scale(AVFrame src, int srcSliceY, int srcSliceH, AVFrame dst) {
		return n.scale(src.n, srcSliceY, srcSliceH, dst.n);
	}

	public int scale(AVFrame src, int srcSliceY, int srcSliceH, int[] dst) {
		return n.scaleIntArray(src.n, srcSliceY, srcSliceH, dst, dstFormat.toC(), dstW, dstH);
	}

	public int scale(AVFrame src, int srcSliceY, int srcSliceH, byte[] dst) {
		return n.scaleByteArray(src.n, srcSliceY, srcSliceH, dst, dstFormat.toC(), dstW, dstH);
	}
}

class SwsContextNative extends SwsContextNativeAbstract {

	public SwsContextNative(AVObject o) {
		super(o);
	}

	native int scale(AVFrameNative srcFrame, int srcSliceY, int srcSliceH, AVFrameNative dstFrame);

	native int scaleIntArray(AVFrameNative srcFrame, int srcSliceY, int srcSliceH, int[] dst, int pixfmt, int width, int height);

	native int scaleByteArray(AVFrameNative srcFrame, int srcSliceY, int srcSliceH, byte[] dst, int pixfmt, int width, int height);
}

class SwsContextNative32 extends SwsContextNative {

	int p;

	SwsContextNative32(AVObject o, int p) {
		super(o);
		this.p = p;
	}

	@Override
	public void dispose() {
		if (p != 0) {
			freeContext();
		}
		super.dispose();
	}
}

class SwsContextNative64 extends SwsContextNative {

	long p;

	SwsContextNative64(AVObject o, long p) {
		super(o);
		this.p = p;
	}

	@Override
	public void dispose() {
		if (p != 0) {
			freeContext();
		}
		super.dispose();
	}
}
