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
package au.notzed.jjmpeg.io;

import au.notzed.jjmpeg.ByteIOContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Read a file through Java.
 *
 * TODO: Not yet tested.
 * @author notzed
 */
public class JJFileInputStream extends ByteIOContext {

	FileInputStream fis;
	ByteBuffer buffer;

	protected JJFileInputStream(FileInputStream is, ByteBuffer buffer) {
		super(buffer, 0);
		this.fis = is;
		this.buffer = buffer;
	}

	public static JJFileInputStream create(FileInputStream is) {
		ByteBuffer b = ByteBuffer.allocateDirect(4096).order(ByteOrder.nativeOrder());

		return new JJFileInputStream(is, b);
	}

	public int readPacket(ByteBuffer dst) {
		try {
			return fis.getChannel().read(dst);
		} catch (IOException ex) {
			return -1;
		}
	}

	public int writePacket(ByteBuffer src) {
		try {
			return fis.getChannel().write(src);
		} catch (IOException ex) {
			return -1;
		}
	}

	public long seek(long offset, int whence) {
		long res = -1;

		try {
			switch (whence) {
				case AVSEEK_SIZE:
					return (int) fis.getChannel().size();
				case SEEK_SET:
					fis.getChannel().position(offset);
					res = fis.getChannel().position();
					break;
				case SEEK_CUR:
					fis.getChannel().position(fis.getChannel().position() + whence);
					res = fis.getChannel().position();
					break;
				case SEEK_END:
					fis.getChannel().position(fis.getChannel().size() - whence);
					res = fis.getChannel().position();
					break;
			}
		} catch (IOException ex) {
		}
		return res;
	}
}
