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

import au.notzed.jjmpeg.*;
import au.notzed.jjmpeg.exception.AVDecodingError;
import java.io.IOException;

/**
 * Decoded AVPacket's into audio, and pass them to the MediaSink
 *
 * @author notzed
 */
public class AudioDecoder extends MediaDecoder {

	AVPacket apacket;
	AVFrame frame;
	SwrContext resample;
	AVFrame resampledFrame;
	int dstChannels;
	AVSampleFormat dstFormat;

	AudioDecoder(MediaReader src, MediaSink dest, AVStream stream, int streamid) throws IOException {
		super("Audio Decoder", src, dest, stream, streamid);

		apacket = AVPacket.create();
		frame = AVFrame.create();

		dstChannels = cc.getChannels();
		dstFormat = cc.getSampleFmt();
	}

	/**
	 * @param layout use 3 for stereo
	 * @param channels
	 * @param fmt
	 */
	public void setOutputFormat(long layout, int channels, AVSampleFormat fmt, int rate) {
		if (cc.getSampleFmt() == fmt
				&& cc.getChannels() == channels) {
			resample = null;
		} else {
			System.out.printf("Create resample context dst layout = %d fmt %d rate %d\n  src layout %d fmt %d rate %d\n",
					layout, fmt.toC(), rate,
					cc.getChannelLayout(), cc.getSampleFmt().toC(), cc.getSampleRate());
			resample = SwrContext.create(layout, fmt, rate, cc.getChannelLayout(), cc.getSampleFmt(), cc.getSampleRate());
			resampledFrame = AVFrame.create();
		}
		dstChannels = channels;
		dstFormat = fmt;
	}

	@Override
	void decodePacket(AVPacket packet) throws AVDecodingError, InterruptedException {
		//System.out.println("audio decode packet()");
		//if (true)return;
		apacket.setSrc(packet);
		//apacket = AVAudioPacket.create(packet);

		while (apacket.getSize() > 0) {
			try {
				AVFrame data = frame;
				int samples = cc.decodeAudio(frame, apacket);

				if (samples > 0) {
					if (resample != null) {
						//System.out.println("resampling audio input samples: " + frame.getNbSamples());
						resampledFrame.fillAudioFrame(dstChannels, dstFormat, samples);
						samples = resample.convert(resampledFrame, frame);
						data = resampledFrame;
					}

					AudioFrame af = dest.getAudioFrame();
					try {
						af.pts = convertPTS(packet.getDTS());
						af.setSamples(data, dstChannels, dstFormat, samples);
						af.enqueue();
						af = null;
					} finally {
						if (af != null)
							af.recycle();
					}
				}
			} catch (AVDecodingError ex) {
				System.out.println("decode audio failed " + ex + " packet size " + packet.getSize());
				throw ex;
			} catch (InterruptedException ex) {
				throw ex;
			} catch (Exception ex) {
				System.out.println("decode audio failed " + ex + " packet size " + packet.getSize());
			}
		}
	}
}
