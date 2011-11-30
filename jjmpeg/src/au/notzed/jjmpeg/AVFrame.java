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
public class AVFrame extends AVFrameAbstract {

	protected AVFrame(ByteBuffer p) {
		setNative(new AVFrameNative(this, p));
	}

	static public AVFrame create(ByteBuffer p) {
		return new AVFrame(p);
	}

	static public AVFrame create() {
		return allocFrame();
	}

	static public AVFrame create(PixelFormat fmt, int width, int height) {
		AVFrame f = create();
		int res = f.alloc(fmt.toC(), width, height);

		if (res != 0) {
			throw new ExceptionInInitializerError("Unable to allocate bitplanes");
		}
		((AVFrameNative) f.n).allocated = true;

		return f;
	}

	public AVPlane getPlaneAt(int index, PixelFormat fmt, int width, int height) {
		int lineSize = getLineSizeAt(index);

		return new AVPlane(AVFrameNative.getPlaneAt(n.p, index, fmt.toC(fmt), width, height), lineSize, width, height);
	}

	public boolean isKeyFrame() {
		return getKeyFrame() != 0;
	}
}

class AVFrameNative extends AVFrameNativeAbstract {

	boolean allocated = false;

	AVFrameNative(AVObject o, ByteBuffer p) {
		super(o, p);
	}

	static native ByteBuffer getPlaneAt(ByteBuffer p, int index, int pixelFormat, int width, int height);

	@Override
	public void dispose() {
		if (p != null) {
			if (allocated) {
				free(p);
			}
		}
		super.dispose();
	}
}
