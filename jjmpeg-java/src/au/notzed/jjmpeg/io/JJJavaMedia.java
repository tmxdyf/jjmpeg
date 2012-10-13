/*
 * Copyright (c) 2012 Michael Zucchi
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
package au.notzed.jjmpeg.io;

import au.notzed.jjmpeg.exception.AVIOException;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * Utilities and helpers for JJMedia* for Java clients.
 * @author notzed
 */
public class JJJavaMedia {

	/**
	 * Create a buffered image suitable for the stream in question.
	 * @param vs
	 * @return
	 */
	public static BufferedImage createBufferedImage(JJMediaReader.JJReaderVideo vs) {
		return new BufferedImage(vs.getOutputWidth(), vs.getOutputHeight(), BufferedImage.TYPE_3BYTE_BGR);
	}

	/**
	 * Get an output frame into a BufferedImage.
	 * @param vs
	 * @param src If not null, Must be allocated using createBufferedImage.
	 * @return src or a new bufferedimage with the frame output.
	 */
	public static BufferedImage getOutputFrame(JJMediaReader.JJReaderVideo vs, BufferedImage src) {
		if (src == null)
			src = createBufferedImage(vs);

		byte[] data = ((DataBufferByte)src.getRaster().getDataBuffer()).getData();
		vs.getOutputFrame(data);

		return src;
	}

	/**
	 * Create a buffered image suitable for a writer
	 * @param vs
	 * @return
	 */
	public static BufferedImage createBufferedImage(JJMediaWriter.JJWriterVideo vs) {
		return new BufferedImage(vs.getContext().getWidth(), vs.getContext().getHeight(), BufferedImage.TYPE_3BYTE_BGR);
	}

	/**
	 * Add a frame
	 * @param vs
	 * @param src
	 * @throws AVIOException
	 */
	public static void addFrame(JJMediaWriter.JJWriterVideo vs, BufferedImage src) throws AVIOException {
		byte[] data = ((DataBufferByte)src.getRaster().getDataBuffer()).getData();

		vs.addFrameRGB(data);
	}

}
