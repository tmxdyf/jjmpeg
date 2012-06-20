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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

/**
 *
 * @author notzed
 */
abstract public class AVNative extends WeakReference<AVObject> {

	static private ReferenceQueue<AVObject> refqueue = new ReferenceQueue<AVObject>();
	static private LinkedList<AVNative> reflist = new LinkedList<AVNative>();

	protected AVNative(AVObject jobject) {
		super(jobject, refqueue);
		reflist.add(this);
		gc(2);
	}
	static final boolean is64;
	/**
	 * Index of libavformat major version number in result from getVersions()
	 */
	static final int LIBAVFORMAT_VERSION = 0;
	static final int LIBAVCODEC_VERSION = 1;
	static final int LIBAVUTIL_VERSION = 2;

	static {
		int bits;

		System.err.println("load jjmpeg");

		System.loadLibrary("jjmpeg010");
		bits = initNative();

		if (bits == 0) {
			throw new UnsatisfiedLinkError("Unable to open jjmpeg");
		}
		is64 = bits == 64;

		// may as well do these here i guess?
		AVFormatContext.registerAll();
		AVFormatContext.networkInit();
	}

	static native ByteBuffer getPointer(ByteBuffer base, int offset, int size);

	static native ByteBuffer getPointerIndex(ByteBuffer base, int offset, int size, int index);

	static native int initNative();

	static native void getVersions(ByteBuffer b);

	/**
	 * Retrieve run-time library version info.
	 *
	 * use LIB*_VERSION indices to get actual versions.
	 */
	static public int[] getVersions() {
		ByteBuffer bvers = ByteBuffer.allocateDirect(4 * 3).order(ByteOrder.nativeOrder());

		getVersions(bvers);

		int[] vers = new int[3];
		bvers.asIntBuffer().get(vers);
		return vers;
	}

	private static void gc(int limit) {
		AVNative an;
		int count = 0;

		while (count < limit
				&& ((an = (AVNative) refqueue.poll()) != null)) {
			System.err.println("auto-disposing " + an);
			an.dispose();
			count += 1;
		}
	}

	/**
	 * Check if any objects are still lying around
	 */
	public static void check() {
		System.err.println("active objects:");
		for (AVNative n : reflist) {
			System.err.println(" " + n);
		}
	}

	/**
	 * Dispose of this resource. It must be safe to call this multiple times.
	 *
	 * The default dispose removes it from the weak reference list, so
	 * must be called.
	 */
	public void dispose() {
		System.out.println("jjmpeg: dispose native: " + this);
		reflist.remove(this);
	}
}
