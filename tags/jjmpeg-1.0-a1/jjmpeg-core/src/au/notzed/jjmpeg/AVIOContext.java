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
import java.nio.IntBuffer;

/**
 * Wrapper for AVIOContext - allows custom streams to
 * be implemented from java.
 *
 * To implement a custom stream the following things must be done:
 *
 * Implement new(int p) and new(long p) to simply call the parent
 * constructor.
 *
 * Implement a create() method which invokes allocContext() with the
 * appropriate class, size and flags which will call the default
 * constructors above, the create method can then initialise any
 * local fields.
 *
 * @author notzed
 */
public class AVIOContext extends AVIOContextAbstract {

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
		setNative(new AVIOContextNative32(this, p, 0));
	}

	protected AVIOContext(long p) {
		setNative(new AVIOContextNative64(this, p, 0));
	}

	public static AVIOContext open(String url, int flags) throws AVIOException {
		IntBuffer ib = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();

		AVIOContext ct = AVIOContextNative.open(url, flags, ib);

		int errno = ib.get(0);
		if (errno < 0) {
			throw new AVIOException(errno);
		}

		return ct;
	}

	/**
	 * Initialise the aviocontext.  Implementations use this
	 * to create themselves.
	 *
	 * @param size
	 * @param flags Taken from ALLOC_* flags
	 */
	protected static AVIOContext allocContext(Class c, int size, int flags) {
		return AVIOContextNative.allocContext(c, size, flags);
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

	final int type;

	AVIOContextNative(AVObject o, int type) {
		super(o);

		this.type = type;
	}

	/**
	 * Allocate an avio context, and bind it to the given object class.
	 * @param oc
	 * @param size
	 * @param flags
	 * @return
	 */
	static native AVIOContext allocContext(Class oc, int size, int flags);

	native AVInputFormat probeInput(String filename, int offset, int max_probe_size);

	static native AVIOContext open(String url, int flags, IntBuffer error_buf);

	/**
	 * Bind AVIOContext to java instance
	 */
	static native void bind(AVIOContext self, AVIOContextNative p);

	/**
	 * Unbind AVIOContext from java instance, and free memory too.
	 */
	static native void unbind(AVIOContext self, AVIOContextNative p);
}

class AVIOContextNative32 extends AVIOContextNative {

	int p;

	AVIOContextNative32(AVObject o, int p, int type) {
		super(o, type);
		this.p = p;
	}

	@Override
	public void dispose() {
		if (p != 0) {
			if (type == 1) {
				AVIOContextNative.unbind((AVIOContext) this.get(), this);
			}
		}
		super.dispose();
	}
}

class AVIOContextNative64 extends AVIOContextNative {

	long p;

	AVIOContextNative64(AVObject o, long p, int type) {
		super(o, type);
		this.p = p;
	}

	@Override
	public void dispose() {
		if (p != 0) {
			if (type == 1) {
				AVIOContextNative.unbind((AVIOContext) this.get(), this);
			}
		}
		super.dispose();
	}
}
