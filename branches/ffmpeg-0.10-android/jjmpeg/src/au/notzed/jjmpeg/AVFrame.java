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

	/**
	 * Load the texture using glTexSubImage2D into the given textures.
	 * (up to).
	 *
	 * Only need to provide texture channels for each plane of the given format.
	 *
	 * This allows textures to be loaded without createing temporary AVPlane objects.
	 * 
	 * @param fmt must match format for picture.  Only YUV420P is supported.
	 * @param create call glTexImage2D if true, else call glTexSubImage2D
	 * @param tex0 texture channel 0 (e.g. Y)
	 * @param tex1 texture channel 1
	 * @param tex2 texture channel 2
	 */
	public void loadTexture2D(PixelFormat fmt, boolean create, int tex0, int tex1, int tex2) {
		n.loadTexture2D(fmt.toC(), create, tex0, tex1, tex2);
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

	native void loadTexture2D(int pixelFormat, boolean create, int tex0, int tex1, int tex2);

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
