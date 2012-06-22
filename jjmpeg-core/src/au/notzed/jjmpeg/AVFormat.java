/*
 * copyright (c) 2001 Fabrice Bellard
 *
 * This file is part of FFmpeg.
 *
 * FFmpeg is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * FFmpeg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with FFmpeg; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package au.notzed.jjmpeg;

/**
 * AVFormat constants
 *
 * @author notzed
 */
public interface AVFormat {

	public static final int AVFMT_NOFILE = 0x0001;
	/** Needs '%d' in filename. */
	public static final int AVFMT_NEEDNUMBER = 0x0002;
	/** Show format stream IDs numbers. */
	public static final int AVFMT_SHOW_IDS = 0x0008;
	/** Format wants AVPicture structure for raw picture data. */
	public static final int AVFMT_RAWPICTURE = 0x0020;
	/** Format wants global header. */
	public static final int AVFMT_GLOBALHEADER = 0x0040;
	/** Format does not need / have any timestamps. */
	public static final int AVFMT_NOTIMESTAMPS = 0x0080;
	/** Use generic index building code. */
	public static final int AVFMT_GENERIC_INDEX = 0x0100;
	/** Format allows timestamp discontinuities. Note, muxers always require valid (monotone) timestamps */
	public static final int AVFMT_TS_DISCONT = 0x0200;
	/** Format allows variable fps. */
	public static final int AVFMT_VARIABLE_FPS = 0x0400;
	/** Format does not need width/height */
	public static final int AVFMT_NODIMENSIONS = 0x0800;
	/** Format does not require any streams */
	public static final int AVFMT_NOSTREAMS = 0x1000;
	/** Format does not allow to fallback to binary search via read_timestamp */
	public static final int AVFMT_NOBINSEARCH = 0x2000;
	/** Format does not allow to fallback to generic search */
	public static final int AVFMT_NOGENSEARCH = 0x4000;
	/** Format does not allow seeking by bytes */
	public static final int AVFMT_NO_BYTE_SEEK = 0x8000;
	/** Format allows flushing. If not set, the muxer will not receive a NULL packet in the write_packet function. */
	public static final int AVFMT_ALLOW_FLUSH = 0x10000;
	/** Format does not require strictly increasing timestamps, but they must still be monotonic */
	public static final int AVFMT_TS_NONSTRICT = 0x8000000;
}
