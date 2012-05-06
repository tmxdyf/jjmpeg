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
public class AVFrame extends AVFrameAbstract {

	AVFrame(int p) {
		setNative(new AVFrameNative32(this, p));
	}

	AVFrame(long p) {
		setNative(new AVFrameNative64(this, p));
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
		((AVFrameNative) f.n).filled = true;

		return f;
	}

	static public AVFrame allocFrame() {
		AVFrame af = AVFrameNativeAbstract.alloc_frame();

		af.n.allocated = true;

		return af;
	}

	public AVPlane getPlaneAt(int index, PixelFormat fmt, int width, int height) {
		int lineSize = getLineSizeAt(index);

		return new AVPlane(n.getPlaneAt(index, fmt.toC(fmt), width, height), lineSize, width, height);
	}

	public boolean isKeyFrame() {
		return getKeyFrame() != 0;
	}
}

class AVFrameNative extends AVFrameNativeAbstract {

	// Was it allocated (with allocFrame()), or just referenced
	boolean allocated = false;
	// Has it been filled using avpicture_alloc()
	boolean filled = false;

	AVFrameNative(AVObject o) {
		super(o);
	}

	native ByteBuffer getPlaneAt(int index, int pixelFormat, int width, int height);

	native void freeFrame();
}

class AVFrameNative32 extends AVFrameNative {

	int p;

	AVFrameNative32(AVObject o, int p) {
		super(o);
		this.p = p;
	}

	@Override
	public void dispose() {
		if (p != 0) {
			if (filled) {
				free();
			}
			if (allocated) {
				freeFrame();
			}
			p = 0;
		}
		super.dispose();
	}
}

class AVFrameNative64 extends AVFrameNative {

	long p;

	AVFrameNative64(AVObject o, long p) {
		super(o);
		this.p = p;
	}

	@Override
	public void dispose() {
		if (p != 0) {
			if (filled) {
				free();
			}
			if (allocated) {
				freeFrame();
			}
			p = 0;
		}
		super.dispose();
	}
}
