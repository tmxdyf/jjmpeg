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

/**
 * Used to wrap a packet which is (potentially) consumed in parts.  i.e. an audio packet.
 *
 * This allocates its own AVPacket structure but copies the data from
 * the source packet.
 * 
 * @author notzed
 */
public class AVAudioPacket extends AVPacket {

	AVPacket src;

	AVAudioPacket(ByteBuffer p) {
		super(p);
	}

	public static AVAudioPacket create() {
		return new AVAudioPacket(AVPacket.allocate());
	}

	/**
	 * Create a shallow copy of a packet.
	 * @param src
	 * @return
	 */
	public static AVAudioPacket create(AVPacket src) {
		AVAudioPacket packet = new AVAudioPacket(AVPacket.allocate());

		// *this = *src;
		packet.p.put(src.p);
		src.p.rewind();
		// so it doesn't go away without us knowing
		packet.src = src;

		return packet;
	}

	/**
	 * On an existing packet, sets the source of
	 * the data, also resets the data pointers and length.
	 *
	 * Allows audiopacket reuse whilst avoiding memory allocations.
	 * @param src
	 */
	public void setSrc(AVPacket src) {
		p.put(src.p).rewind();
		src.p.rewind();
		this.src = src;
	}

	/**
	 * Consumes 'len' bytes by incrementing the data pointer and decrementing the
	 * length of the packet.
	 * @param len
	 * @return
	 */
	public native int consume(int len);

}
