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
package au.notzed.jjmpeg.util;

import android.content.Context;
import android.opengl.GLSurfaceView;

/**
 *
 * @author notzed
 */
public class JJGLSurfaceView extends GLSurfaceView {

	JJGLRenderer renderer;

	public JJGLSurfaceView(Context context) {
		super(context);

		setEGLContextClientVersion(2);
		renderer = new JJGLRenderer(context, this);
		setRenderer(renderer);

		setRenderMode(RENDERMODE_WHEN_DIRTY);
	}
}