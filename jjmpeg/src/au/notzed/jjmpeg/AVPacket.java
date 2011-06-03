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
 *
 * @author notzed
 */
public class AVPacket extends AVPacketAbstract {

	AVPacket(ByteBuffer p) {
		super(p);
	}

	public static AVPacket create() {
		return new AVPacket(allocate());
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		free(p);
	}

	private native ByteBuffer get_data(ByteBuffer p);

	private static native ByteBuffer allocate();

	private static native void free(ByteBuffer p);

	public ByteBuffer getData() {
		return get_data(p);
	}
}
