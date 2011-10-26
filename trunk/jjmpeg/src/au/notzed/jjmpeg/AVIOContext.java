/*
 * Copyright (c) 2001 Fabrice Bellard
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
 * Wrapper for AVIOContext - allows custom streams to
 * be implemented from java.
 *
 * Or should, if it wasn't busted.
 * 
 * TODO: This is now deprecated in libavformat.  Should i just rename it?
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

	protected AVIOContext(ByteBuffer p) {
		setNative(new AVIOContextNative(this, p));
	}

	/**
	 * Initialise the aviocontext.
	 * @param size
	 * @param flags Taken from ALLOC_* flags
	 */
	protected AVIOContext(int size, int flags) {
		this(AVIOContextNative.allocContext(size, flags));
		AVIOContextNative.bind(this, n.p);
	}

	/**
	 * Call av_probe_input_buffer() on this stream
	 * @param filename
	 * @param offset
	 * @param max_probe_size
	 * @return 
	 */
	public AVInputFormat probeInput(String filename, int offset, int max_probe_size) {
		ByteBuffer b = AVIOContextNative.probeInput(n.p, filename, offset, max_probe_size);

		if (b != null) {
			return AVInputFormat.create(b);
		}

		return null;
	}

	/**
	 * Release resources.  This must be called otherwise the object
	 * will not be garbage collected.
	 *
	 * TODO: still thinking of how to implement the lifecycle.
	 */
	@Override
	public void dispose() {
		super.dispose();
		AVIOContextNative.unbind(this, n.p);
	}

	public abstract int readPacket(ByteBuffer dst);

	public abstract int writePacket(ByteBuffer src);

	public abstract long seek(long offset, int whence);
}

class AVIOContextNative extends AVIOContextNativeAbstract {
	
	AVIOContextNative(AVObject o, ByteBuffer p) {
		super(o, p);
	}

	static native ByteBuffer allocContext(int size, int flags);

	static native ByteBuffer probeInput(ByteBuffer p, String filename, int offset, int max_probe_size);

	/**
	 * Bind AVIOContext to java instance
	 */
	static native void bind(AVIOContext self, ByteBuffer p);

	/**
	 * Unbind AVIOContext from java instance, and free memory too.
	 */
	static native void unbind(AVIOContext self, ByteBuffer p);
}
