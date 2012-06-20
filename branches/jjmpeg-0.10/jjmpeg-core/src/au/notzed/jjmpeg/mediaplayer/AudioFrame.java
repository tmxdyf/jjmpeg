/*
 * Copyright (c) 2012 Michael Zucchi
 *
 * This file is part of jjmpeg.
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
package au.notzed.jjmpeg.mediaplayer;

import au.notzed.jjmpeg.AVFrame;
import au.notzed.jjmpeg.SampleFormat;

/**
 * Audio frame that can (possibly) dispose itself
 *
 * @author notzed
 */
public abstract class AudioFrame extends MediaFrame {

	long pts;
	//public final AVSamples frame;
	short[] samples;
	int samplesLen;
	int channelsCount;

	public AudioFrame() {
		//this.frame = samples;
	}

	public void setSamples(AVFrame src, int channels, SampleFormat fmt, int nsamples) {
		if (samples == null || samples.length < nsamples * channels) {
			samples = new short[nsamples * channels];
		}

		src.getSamples(fmt, channels, samples);
		samplesLen = nsamples;
		channelsCount = channels;
	}

	public void dispose() {
		//frame.dispose();
	}

	@Override
	long getPTS() {
		return pts;
	}
}
