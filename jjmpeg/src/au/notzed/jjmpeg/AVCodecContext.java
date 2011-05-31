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
import java.nio.ByteOrder;

/**
 *
 * @author notzed
 */
abstract public class AVCodecContext extends AVCodecContextAbstract {

	public static final int AVMEDIA_TYPE_UNKNOWN = -1;
	public static final int AVMEDIA_TYPE_VIDEO = 0;
	public static final int AVMEDIA_TYPE_AUDIO = 1;
	public static final int AVMEDIA_TYPE_DATA = 2;
	public static final int AVMEDIA_TYPE_SUBTITLE = 3;
	public static final int AVMEDIA_TYPE_ATTACHMENT = 4;
	public static final int AVMEDIA_TYPE_NB = 5;

	protected AVCodecContext(ByteBuffer p) {
		super(p);
	}

	static AVCodecContext create(ByteBuffer p) {
		if (AVNative.is64) {
			return new AVCodecContext64(p);
		} else {
			return new AVCodecContext32(p);
		}
	}

	static {
		System.loadLibrary("jjmpeg");
	}

	private native int open(ByteBuffer context, ByteBuffer codec);

	private native int decode_video(ByteBuffer p, ByteBuffer frame, ByteBuffer finished, ByteBuffer data);
//

	private static native void register_all();

	public int open(AVCodec codec) {
		int res;

		res = open(p, codec.p);

		return res;
	}

	public boolean decodeVideo(AVFrame frame, AVPacket packet) {
		ByteBuffer fin = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder());
		int res;

		res = decode_video(p, frame.p, fin, packet.p);

		if (res < 0) {
			// FIXME: right exception
			throw new RuntimeException("Error decoding video");
		}

		return (fin.asIntBuffer().get(0) != 0);
	}

	public static void registerAll() {
		register_all();
	}
}
