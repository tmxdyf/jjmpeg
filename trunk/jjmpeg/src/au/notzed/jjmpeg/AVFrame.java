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
public abstract class AVFrame extends AVFrameAbstract {

	protected AVFrame(ByteBuffer p) {
		super(p);
	}

	static public AVFrame create() {
		return new AVFrame64(alloc_frame());
	}

	public void dispose() {
		free_frame(p);
	}

	native static ByteBuffer alloc_frame();
	native static void free_frame(ByteBuffer bb);

	public AVPlane getPlaneAt(int index, PixelFormat fmt, int width, int height) {
		if (index >= 4)
			throw new ArrayIndexOutOfBoundsException();

		int lineSize = getLineSizeAt(index);

		// FIXME: take fmt into account somehow
		if (index > 0) {
			height /= 2;
		}
		return new AVPlane(AVNative.getPointerIndex(p, getDataOffset(), lineSize * height, index), lineSize, width, height);
	}
}
