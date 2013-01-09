/*
 * Copyright (c)  2001 Fabrice Bellard
 * Copyright 2013 Michael Zucchi
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
 * Discard enums for various decoding points.
 * @author notzed
 */
public interface AVDiscard {

	public static final int AVDISCARD_NONE = -16, ///< discard nothing
			AVDISCARD_DEFAULT = 0, ///< discard useless packets like 0 size packets in avi
			AVDISCARD_NONREF = 8, ///< discard all non reference
			AVDISCARD_BIDIR = 16, ///< discard all bidirectional frames
			AVDISCARD_NONKEY = 32, ///< discard all frames except keyframes
			AVDISCARD_ALL = 48; ///< discard all
	public static final int values[] = {
		AVDISCARD_NONE,
		AVDISCARD_DEFAULT,
		AVDISCARD_NONREF,
		AVDISCARD_BIDIR,
		AVDISCARD_NONKEY,
		AVDISCARD_ALL
	};
};
