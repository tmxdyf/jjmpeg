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

/**
 * Just a dummy object to hold the static init functions.
 *
 * AVDevice is optional, call AVDevice.registerAll() to enable
 * the AVDevice features.  If AVDevice is not available
 * it should throw a NoSuchMethodError.
 *
 * @author notzed
 */
public class AVDevice extends AVDeviceAbstract {

	public AVDevice(long p) {
	}

	public AVDevice(int p) {
	}
}

/**
 * All this to bind 2 static functions, probably easier just to do it manually
 * @author notzed
 */
class AVDeviceNative extends AVDeviceNativeAbstract {

	public AVDeviceNative(AVObject o) {
		super(o);
	}
}

class AVDeviceNative64 extends AVDeviceNative {
	long p;
	public AVDeviceNative64(AVObject o) {
		super(o);
	}
}

class AVDeviceNative32 extends AVDeviceNative {
	public AVDeviceNative32(AVObject o) {
		super(o);
	}
}