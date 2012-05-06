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
public class AVPacket extends AVPacketAbstract {

	public static final int AV_PKT_FLAG_KEY = 1;

	AVPacket(int p) {
		setNative(new AVPacketNative32(this, p));
	}

	AVPacket(long p) {
		setNative(new AVPacketNative64(this, p));
	}

	public static AVPacket create() {
		return AVPacketNative.allocatePacket();
	}

	public void setData(ByteBuffer data, int size) {
		n.setData(data, size);
	}
	AVPacket src;

	/**
	 * Create a shallow copy of 'src' into 'this'.
	 * Used for audio packet re-use, where the packet is consumed incrementally.
	 *
	 * @param src
	 */
	public void setSrc(AVPacket src) {
		n.copyPacket(src.n);
		this.src = src;
	}

	/**
	 * Consumes 'len' bytes by incrementing the data pointer and decrementing the
	 * length of the packet.
	 *
	 * @param len
	 * @return
	 */
	public int consume(int len) {
		return n.consume(len);
	}
}

class AVPacketNative extends AVPacketNativeAbstract {

	public AVPacketNative(AVObject o) {
		super(o);
	}

	native int consume(int len);

	static native AVPacket allocatePacket();

	native void freePacket();

	public native void setData(ByteBuffer b, int size);

	/**
	 * Shallow copy the content of the packet struct from src
	 * (i.e. in c *this = *src)
	 *
	 * @param src
	 */
	public native void copyPacket(AVPacketNative src);
}

class AVPacketNative32 extends AVPacketNative {

	int p;

	AVPacketNative32(AVObject o, int p) {
		super(o);
		this.p = p;
	}

	@Override
	public void dispose() {
		if (p != 0) {
			freePacket();
		}
		super.dispose();
	}
}

class AVPacketNative64 extends AVPacketNative {

	long p;

	AVPacketNative64(AVObject o, long p) {
		super(o);
		this.p = p;
	}

	@Override
	public void dispose() {
		if (p != 0) {
			freePacket();
		}
		super.dispose();
	}
}
