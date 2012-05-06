/*
 * Copyright (C) 2001-2003 Michael Niedermayer <michaelni@gmx.at>
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

/**
 * Used to convert between formats and optionally scale at the same time.
 *
 * @author notzed
 */
public class ReSampleContext extends ReSampleContextAbstract {

	protected ReSampleContext(int p) {
		setNative(new ReSampleContextNative32(this, p));
	}

	protected ReSampleContext(long p) {
		setNative(new ReSampleContextNative64(this, p));
	}

	static public ReSampleContext create(int output_channels, int input_channels, int output_rate, int input_rate, SampleFormat sample_fmt_out, SampleFormat sample_fmt_in, int filter_length, int log2_phase_count, int linear, double cutoff) {
		return ReSampleContext.resampleInit(output_channels, input_channels, output_rate, input_rate, sample_fmt_out, sample_fmt_in, filter_length, log2_phase_count, linear, cutoff);
	}

	public void close() {
		dispose();
	}
}

class ReSampleContextNative extends ReSampleContextNativeAbstract {

	public ReSampleContextNative(AVObject o) {
		super(o);
	}
}

class ReSampleContextNative32 extends ReSampleContextNative {

	int p;

	ReSampleContextNative32(AVObject o, int p) {
		super(o);
		this.p = p;
	}

	@Override
	public void dispose() {
		if (p != 0) {
			resample_close();
			super.dispose();
			p = 0;
		}
	}
}

class ReSampleContextNative64 extends ReSampleContextNative {

	long p;

	ReSampleContextNative64(AVObject o, long p) {
		super(o);
		this.p = p;
	}

	@Override
	public void dispose() {
		if (p != 0) {
			resample_close();
			super.dispose();
			p = 0;
		}
	}
}
