/*
 * copyright (c) 2001 Fabrice Bellard
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

/**
 *
 * @author notzed
 */
public enum SampleFormat {

	SAMPLE_FMT_NONE,
	/**
	 * unsigned 8 bits
	 */
	SAMPLE_FMT_U8,
	/**
	 * signed 16 bits
	 */
	SAMPLE_FMT_S16,
	/**
	 * signed 32 bits
	 */
	SAMPLE_FMT_S32,
	/**
	 * float
	 */
	SAMPLE_FMT_FLT,
	/**
	 *  double
	 */
	SAMPLE_FMT_DBL;
	//  SAMPLE_FMT_NB;               ///< Number of sample formats. DO NOT USE if dynamically linking to libavcodec

	public static SampleFormat fromC(int fmtid) {
		return values()[fmtid + 1];
	}

	public static int toC(SampleFormat p) {
		return p.ordinal() - 1;
	}

	public int toC() {
		return ordinal() - 1;
	}
};
