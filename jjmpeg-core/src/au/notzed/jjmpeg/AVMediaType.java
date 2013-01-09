/*
 * Copyright (c) 2006 Michael Niedermayer <michaelni@gmx.at>
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
 *
 * @author notzed
 */
public interface AVMediaType {

	public static final int AVMEDIA_TYPE_UNKNOWN = -1,
			AVMEDIA_TYPE_VIDEO = 0,
			AVMEDIA_TYPE_AUDIO = 1,
			AVMEDIA_TYPE_DATA = 2,
			AVMEDIA_TYPE_SUBTITLE = 3,
			AVMEDIA_TYPE_ATTACHMENT = 4,
			AVMEDIA_TYPE_NB = 5;
}
