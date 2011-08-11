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
 * Wrapper for ByteIOContext - allows custom streams to
 * be implemented from java.
 *
 * Or should, if it wasn't busted.
 * 
 * TODO: This is now deprecated in libavformat.  Should i just rename it?
 * @author notzed
 */
public abstract class ByteIOContext extends ByteIOContextAbstract {

	public final int AVSEEK_SIZE = 0x10000;
	//
	public final int SEEK_SET = 0;
	public final int SEEK_CUR = 1;
	public final int SEEK_END = 2;
	//
	ByteBuffer buffer;

	protected ByteIOContext(ByteBuffer p) {
		super(p);
	}

	protected ByteIOContext(ByteBuffer buffer, int write_flag) {
		super(create_put_byte(buffer, write_flag));
		bind();
	}

	static protected native ByteBuffer create_put_byte(ByteBuffer buffer, int write_flag);

	/**
	 * Bind ByteIOContext to java instance
	 */
	private native void bind();

	/**
	 * Unbind ByteIOContext from java instance, and free memory too.
	 */
	protected native void unbind();

	/**
	 * Release resources.  This must be called otherwise the object
	 * will not be garbage collected.
	 *
	 * TODO: still thinking of how to implement the lifecycle.
	 */
	public void release() {
		unbind();
	}

	public abstract int readPacket(ByteBuffer dst);

	public abstract int writePacket(ByteBuffer src);

	public abstract long seek(long offset, int whence);
}
