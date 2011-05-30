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

class AVInputFormat {

	ByteBuffer struct;
}

class AVFormatParameters {

	ByteBuffer struct;
}

/**
 *
 * @author notzed
 */
abstract public class AVFormatContext extends AVFormatContextAbstract {

	public final int AVSEEK_FLAG_BACKWARD = 1; ///< seek backward
	public final int AVSEEK_FLAG_BYTE = 2; ///< seeking based on position in bytes
	public final int AVSEEK_FLAG_ANY = 4; ///< seek to any frame, even non-keyframes
	public final int AVSEEK_FLAG_FRAME = 8; ///< seeking based on frame number

	protected AVFormatContext(ByteBuffer p) {
		super(p);
	}

	static AVFormatContext create(ByteBuffer p) {
		return new AVFormatContext64(p);
	}

	static public AVFormatContext openInputFile(String name) {
		return openInputFile(name, null, 0, null);
	}

	static AVFormatContext openInputFile(String name, AVInputFormat fmt, int buf_size, AVFormatParameters ap) {
		ByteBuffer res = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
		ByteBuffer context;

		context = open_input_file(name, fmt != null ? fmt.struct : null, buf_size, ap != null ? ap.struct : null, res);
		if (context == null) {
			// throw new AVFormatException based on error id
			throw new RuntimeException("failed");
		}

		return create(context);
	}

	static native ByteBuffer open_input_file(String name, ByteBuffer fmt, int buf_size, ByteBuffer fmtParameters, ByteBuffer error_ptr);

	native int seek_frame(ByteBuffer p, int stream_index, long timestamp, int flags);

	native int find_stream_info(ByteBuffer p);

	native int read_frame(ByteBuffer p, ByteBuffer packet);

	public int findStreamInfo() {
		return find_stream_info(p);
	}

	public int readFrame(AVPacket packet) {
		int res = read_frame(p, packet.p);

		if (res != 0) {
			System.out.printf("error reading frame %d\n", res);
		}

		return res;
	}
	public int seekFrame(int stream_index, long timestamp, int flags) {
		return seek_frame(p, stream_index, timestamp, flags);
	}
}
