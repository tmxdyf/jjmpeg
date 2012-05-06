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
public class AVOutputFormat extends AVOutputFormatAbstract {

	public static final int AVFMT_NOFILE = 0x0001;
	/** < Needs '%d' in filename. */
	public static final int AVFMT_NEEDNUMBER = 0x0002;
	/** < Show format stream IDs numbers. */
	public static final int AVFMT_SHOW_IDS = 0x0008;
	/** < Format wants AVPicture structure for raw picture data. */
	public static final int AVFMT_RAWPICTURE = 0x0020;
	/** < Format wants global header. */
	public static final int AVFMT_GLOBALHEADER = 0x0040;
	/** < Format does not need / have any timestamps. */
	public static final int AVFMT_NOTIMESTAMPS = 0x0080;
	/** < Use generic index building code. */
	public static final int AVFMT_GENERIC_INDEX = 0x0100;
	/** < Format allows timestamp discontinuities. Note, muxers always require valid (monotone) timestamps */
	public static final int AVFMT_TS_DISCONT = 0x0200;
	/** < Format allows variable fps. */
	public static final int AVFMT_VARIABLE_FPS = 0x0400;
	/** < Format does not need width/height */
	public static final int AVFMT_NODIMENSIONS = 0x0800;
	/** < Format does not require any streams */
	public static final int AVFMT_NOSTREAMS = 0x1000;

	protected AVOutputFormat(int p) {
		setNative(new AVOutputFormatNative32(this, p));
	}

	protected AVOutputFormat(long p) {
		setNative(new AVOutputFormatNative64(this, p));
	}
}

class AVOutputFormatNative extends AVOutputFormatNativeAbstract {

	public AVOutputFormatNative(AVObject o) {
		super(o);
	}
}

class AVOutputFormatNative32 extends AVOutputFormatNative {

	int p;

	AVOutputFormatNative32(AVObject o, int p) {
		super(o);
		this.p = p;
	}
}

class AVOutputFormatNative64 extends AVOutputFormatNative {

	long p;

	AVOutputFormatNative64(AVObject o, long p) {
		super(o);
		this.p = p;
	}
}
