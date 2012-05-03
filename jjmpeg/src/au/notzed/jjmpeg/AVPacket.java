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
		setNative(new AVPacketNative(this, p));
	}

	public static AVPacket create() {
		return AVPacketNative.allocatePacket();
	}

	public void setData(ByteBuffer data, int size) {
		n.setData(data, size);
	}

	/**
	 * Consumes 'len' bytes by incrementing the data pointer and decrementing the
	 * length of the packet.
	 * @param len
	 * @return
	 */
	public int consume(int len) {
		return n.consume(len);
	}
}

class AVPacketNative extends AVPacketNativeAbstract {
	int p;

	AVPacketNative(AVObject o, int p) {
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

	native int consume(int len);

	static native AVPacket allocatePacket();

	native void freePacket();

	public native void setData(ByteBuffer b, int size);
}
