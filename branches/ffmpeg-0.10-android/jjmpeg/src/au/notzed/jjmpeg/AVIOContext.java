/*
 * Copyright (c) 2001 Fabrice Bellard
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

import au.notzed.jjmpeg.exception.AVIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Wrapper for AVIOContext - allows custom streams to
 * be implemented from java.
 *
 * @author notzed
 */
public abstract class AVIOContext extends AVIOContextAbstract {

	public static final int AVSEEK_SIZE = 0x10000;
	//
	public static final int SEEK_SET = 0;
	public static final int SEEK_CUR = 1;
	public static final int SEEK_END = 2;
	//
	public static final int ALLOC_WRITE = 1;
	public static final int ALLOC_STREAMED = 2;
	// flags for open
	public static final int URL_RDONLY;
	public static final int URL_WRONLY;
	public static final int URL_RDWR;

	static {
		// These flags changed at version 53

		// TODO: should be done at build time as part of genjjmpeg.pl
		int[] vers = AVNative.getVersions();
		if (vers[AVNative.LIBAVFORMAT_VERSION] >= 53) {
			URL_RDONLY = 1;
			URL_WRONLY = 2;
			URL_RDWR = 3;
		} else {
			URL_RDONLY = 0;
			URL_WRONLY = 1;
			URL_RDWR = 2;
		}
	}

	protected AVIOContext(int p) {
		setNative(new AVIOContextNative(this, p, 0));
	}

	//static AVIOContext create(ByteBuffer p) {
		// These could call back to the C versions
	//	return new AVIOContext(p) {

	//		@Override
	//		public int readPacket(ByteBuffer dst) {
	//			throw new UnsupportedOperationException("Not supported yet.");
	//		}

	//		@Override
	//		public int writePacket(ByteBuffer src) {
	//			throw new UnsupportedOperationException("Not supported yet.");
	//		}

	//		@Override
	//		public long seek(long offset, int whence) {
	//			throw new UnsupportedOperationException("Not supported yet.");
	//		}
	//	};
	//}

	public static AVIOContext open(String url, int flags) throws AVIOException {
		return AVIOContextNative.open(url, flags);
	}

	/**
	 * Initialise the aviocontext.
	 *
	 * @param size
	 * @param flags Taken from ALLOC_* flags
	 */
	protected static AVIOContext allocContext(int size, int flags) {
		//AVIOContext ac = AVIOContextNative.allocContext(size, flags);
		//AVIOContextNative.bind(ac, ac);

		return AVIOContextNative.allocContext(size, flags);
	}

	/**
	 * Call av_probe_input_buffer() on this stream
	 *
	 * @param filename
	 * @param offset
	 * @param max_probe_size
	 * @return
	 */
	public AVInputFormat probeInput(String filename, int offset, int max_probe_size) {
		return n.probeInput(filename, offset, max_probe_size);
	}

	public int readPacket(ByteBuffer dst) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int writePacket(ByteBuffer src) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public long seek(long offset, int whence) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}

class AVIOContextNative extends AVIOContextNativeAbstract {

	int p;
	private final int type;

	AVIOContextNative(AVObject o, int p, int type) {
		super(o);

		this.p = p;
		this.type = type;
	}

	@Override
	public void dispose() {
		if (p != 0) {
			if (type == 1) {
				AVIOContextNative.unbind((AVIOContext) this.get(), this);
			}
			super.dispose();
		}
	}

	static native AVIOContext allocContext(int size, int flags);

	native AVInputFormat probeInput(String filename, int offset, int max_probe_size);

	static native AVIOContext open(String url, int flags);

	/**
	 * Bind AVIOContext to java instance
	 */
	static native void bind(AVIOContext self, AVIOContextNative p);

	/**
	 * Unbind AVIOContext from java instance, and free memory too.
	 */
	static native void unbind(AVIOContext self, AVIOContextNative p);
}
