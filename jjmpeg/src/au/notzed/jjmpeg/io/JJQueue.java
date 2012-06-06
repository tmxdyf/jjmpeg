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

/**
 * This is a simpler blocking queue which seems to run faster on android
 *
 * @author notzed
 */
public class JJQueue<T> {

	Object[] data;
	int head = 0;
	int tail = 0;
	final int mask;

	static int roundUp(int v) {
		int n = Integer.highestOneBit(v);
		if (n != v)
			return n << 1;
		return n;
	}

	public JJQueue(int size) {
		size = roundUp(Math.max(2, size));
		data = new Object[size];
		mask = size - 1;
	}

	synchronized public T take() throws InterruptedException {
		while (head == tail) {
			wait();
		}
		Object o = data[head];
		//data[head] = null;
		head = (head + 1) & mask;

		return (T) o;
	}

	synchronized public T remove() {
		T o = null;
		if (head != tail) {
			o = (T) data[head];
			//data[head] = null;
			head = (head + 1) & mask;
		}
		return o;
	}

	synchronized public void offer(T o) {
		boolean notify = head == tail;

		data[tail] = o;
		tail = (tail + 1) & mask;
		if (notify)
			notify();
	}

	synchronized public void clear() {
		head = tail = 0;
	}

	synchronized public T poll() {
		if (head == tail)
			return null;
		return (T) data[head];
	}

	synchronized public void drainTo(JJQueue<T> dst) {
		while (head != tail) {
			dst.offer((T) data[head]);
			//data[head] = null;
			head = (head + 1) & mask;
		}
	}
}
