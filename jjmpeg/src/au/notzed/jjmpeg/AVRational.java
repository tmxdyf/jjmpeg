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

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;

/**
 *
 * @author notzed
 */
public class AVRational extends AVRationalAbstract {

	protected AVRational(int p) {
		setNative(new AVRationalNative(this, p));
	}

	//static AVRational create(ByteBuffer p) {
	//	return new AVRational(p);
	//}

	static AVRational create(int num, int den) {
		// FIXME!

		// since it's so simple we can create this ourselves
		//ByteBuffer b = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder());
		//b.asIntBuffer().put(num).put(den).rewind();
		//return create(b);

		return null;
	}
	public static final AVRational AV_TIME_BASE_Q = create(1, 1000000);

	public double q2d() {
		return (double) getNum() / (double) getDen();
	}

	static public long rescaleQ(long a, AVRational bq, AVRational cq) {
		return AVRationalNative.jjRescaleQ(a, bq.n, cq.n);
	}

	/**
	 * Perform v * (num * s) / den
	 * @param v
	 * @param s
	 * @return
	 */
	public long scale(long v, int s) {
		return starSlash(v, (long)getNum() * (long)s, getDen());
	}

	/**
	 * Performs A * B / C, where A * B is treated as 128 bit
	 * @param a
	 * @param b
	 * @param c
	 * @return
	 */
	static public long starSlash(long a, long b, long c) {
		// lazy today .. just use BigInteger, even if it's SAF
		byte[] longBytes = new byte[8];
		LongBuffer lb = ByteBuffer.wrap(longBytes).asLongBuffer();

		lb.put(0, a);
		BigInteger A = new BigInteger(longBytes);
		lb.put(0, b);
		BigInteger B = new BigInteger(longBytes);
		lb.put(0, c);
		BigInteger C = new BigInteger(longBytes);

		long res = A.multiply(B).divide(C).longValue();

		//System.out.printf("%d * %d / %d = %d  ? %d  ? %d\n", a, b, c, res, a * b / c, a * b);
		//System.out.printf("%d * %d / %d = %d\n", A.longValue(), B.longValue(), C.longValue(), res);

		return res;
	}
}

class AVRationalNative extends AVRationalNativeAbstract {
	int p;

	AVRationalNative(AVObject o, int p) {
		super(o);

		this.p = p;
	}

	// perhapd implement this locally instead?
	static native long jjRescaleQ(long a, AVRationalNative bq, AVRationalNative cq);
	//	/usr/include/ffmpeg/libavutil/mathematics.h:int64_t av_rescale_q(int64_t a, AVRational bq, AVRational cq) av_const;

}

