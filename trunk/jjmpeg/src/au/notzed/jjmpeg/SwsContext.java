/*
 * Copyright (C) 2001-2003 Michael Niedermayer <michaelni@gmx.at>
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

/**
 * Used to convert between formats and optionally scale at the same time.
 * @author notzed
 */
public class SwsContext extends SwsContextAbstract {

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

	protected SwsContext(ByteBuffer p) {
		super(p);
	}

	native int _scale(ByteBuffer srcFrame, int srcSliceY, int srcSliceH, ByteBuffer dstFrame);

	static SwsContext create(ByteBuffer p) {
		return new SwsContext(p);
	}

	static public SwsContext getContext(int srcW, int srcH, PixelFormat srcFormat, int dstW, int dstH, PixelFormat dstFormat, int flags) {
		return SwsContext.create(_getContext(srcW, srcH, srcFormat.toC(), dstW, dstH, dstFormat.toC(), flags, null, null, null));
	}

	public int scale(AVFrame src, int srcSliceY, int srcSliceH, AVFrame dst) {
		return _scale(src.p, srcSliceY, srcSliceH, dst.p);
	}

	public void dispose() {
		freeContext();
	}
}
