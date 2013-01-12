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

/**
 * Base class for decoded frames.
 *
 * Frames are re-cycled using a return queue.
 *
 * Lifecycle will be:
 *  Set content (codec specific)
 *  enqueue puts it into the display list.
 *  recycle is called after being displayed.
 * @author notzed
 */
public abstract class MediaFrame implements Comparable<MediaFrame> {

	/**
	 * Sequence number, if it doesn't match clock.getSequence(), throw it away when you see it.
	 */
	int sequence;

	abstract long getPTS();

	abstract void dispose();

	abstract void enqueue() throws InterruptedException;

	abstract void recycle();

	public int compareTo(MediaFrame o) {
		return (int) (getPTS() - o.getPTS());
	}
}
