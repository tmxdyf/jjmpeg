/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
package au.notzed.jjmpeg.util;

import android.content.Context;
import android.opengl.GLES20;
import static android.opengl.GLES20.*;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.*;
import javax.microedition.khronos.opengles.GL10;

/**
 *
 * @author notzed
 */
public class JJGLRenderer implements GLSurfaceView.Renderer {

	JJGLSurfaceView view;
	boolean bindTexture = false;
	boolean dataChanged = false;
	int vwidth, vheight;
	int twidth, theight;
	int pwidth, pheight;
	JJFrame pixelData;

	public JJGLRenderer(Context context, JJGLSurfaceView view) {
		this.context = context;
		this.view = view;

		triangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.length
				* FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
		triangleVertices.put(mTriangleVerticesData).position(0);

		Matrix.setIdentityM(stMatrix, 0);
		Matrix.setIdentityM(matrix, 0);
	}

	int roundUp(int v) {
		int n = 256;
		while (n < v) {
			n *= 2;
		}
		return n;
	}

	public synchronized void setVideoSize(int w, int h) {
		int tw = roundUp(w);
		int th = roundUp(h);

		if (this.twidth != tw
				|| this.theight != th) {
			bindTexture = true;
			twidth = tw;
			theight = th;
		}
		this.vwidth = w;
		this.vheight = h;
	}

	public synchronized void setFrame(JJFrame data) {
		if (pixelData != null) {
			System.out.println("frame still here, recycling: " + pixelData);
			pixelData.recycle();
		}
		pixelData = data;
		view.requestRender();
	}

	public void onDrawFrame(GL10 glUnused) {
		synchronized (this) {
			// Need to recreate texture, e.g. video size changed
			if (bindTexture) {
				bindTexture = false;
				GLES20.glBindTexture(GL_TEXTURE_2D, textureID);
				glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, twidth, theight, 0, GL_RGB, GL_UNSIGNED_SHORT_5_6_5, null);
				checkGlError("glTexImage2D");
				System.out.println("texture size changed, " + twidth + "x" + theight);

				Matrix.setIdentityM(stMatrix, 0);
				Matrix.scaleM(stMatrix, 0, (float)vwidth/twidth, (float)vheight/theight, 1);
			}
			if (pixelData != null) {
				//System.out.println(" update texture: " + pixelData);
				glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, vwidth, vheight, GL_RGB, GL_UNSIGNED_SHORT_5_6_5, pixelData.getBuffer());
				checkGlError("textsubimage2d");
				pixelData.recycle();
				pixelData = null;
			}
		}

		// Ignore the passed-in GL10 interface, and use the GLES20
		// class's static methods instead.
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
		GLES20.glUseProgram(paintTexture);
		checkGlError("glUseProgram");

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GL_TEXTURE_2D, textureID);

		triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
		GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false,
				TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
		checkGlError("glVertexAttribPointer maPosition");
		GLES20.glEnableVertexAttribArray(positionHandle);
		checkGlError("glEnableVertexAttribArray maPositionHandle");

		triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
		GLES20.glVertexAttribPointer(textureHandle, 3, GLES20.GL_FLOAT, false,
				TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
		checkGlError("glVertexAttribPointer maTextureHandle");
		GLES20.glEnableVertexAttribArray(textureHandle);
		checkGlError("glEnableVertexAttribArray maTextureHandle");

		long time = SystemClock.uptimeMillis() % 4000L;
		float angle = 0.090f * ((int) time);

		angle = 0;
		//Matrix.setRotateM(matrix, 0, angle, 0, 0, 1.0f);
		Matrix.setIdentityM(matrix, 0);
		//Matrix.scaleM(matrix, 0, 2, 2, 2);

		Matrix.multiplyMM(vpMatrix, 0, vMatrix, 0, matrix, 0);
		Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, vpMatrix, 0);

		GLES20.glUniformMatrix4fv(vpMatrixHandle, 1, false, vpMatrix, 0);
		GLES20.glUniformMatrix4fv(stMatrixHandle, 1, false, stMatrix, 0);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		checkGlError("glDrawArrays");
	}

	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		// Ignore the passed-in GL10 interface, and use the GLES20
		// class's static methods instead.
		GLES20.glViewport(0, 0, width, height);
		float ratio = (float) width / height;

		pwidth = width;
		pheight = height;

		Matrix.orthoM(projMatrix, 0, 1, -1, 1, -1, 3, 7);
	}

	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		// Ignore the passed-in GL10 interface, and use the GLES20
		// class's static methods instead.

		/* Set up alpha blending and an Android background color */
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		GLES20.glClearColor(0.643f, 0.776f, 0.223f, 1.0f);

		/* Set up shaders and handles to their variables */
		paintTexture = createProgram(paintVS, paintFS);
		if (paintTexture == 0) {
			return;
		}
		positionHandle = GLES20.glGetAttribLocation(paintTexture, "aPosition");
		checkGlError("glGetAttribLocation aPosition");
		if (positionHandle == -1) {
			throw new RuntimeException("Could not get attrib location for aPosition");
		}
		textureHandle = GLES20.glGetAttribLocation(paintTexture, "aTextureCoord");
		checkGlError("glGetAttribLocation aTextureCoord");
		if (textureHandle == -1) {
			throw new RuntimeException("Could not get attrib location for aTextureCoord");
		}

		vpMatrixHandle = GLES20.glGetUniformLocation(paintTexture, "uMVPMatrix");
		checkGlError("glGetUniformLocation uMVPMatrix");
		if (vpMatrixHandle == -1) {
			throw new RuntimeException("Could not get attrib location for uMVPMatrix");
		}

		stMatrixHandle = GLES20.glGetUniformLocation(paintTexture, "uSTMatrix");
		checkGlError("glGetUniformLocation uSTMatrix");
		if (vpMatrixHandle == -1) {
			throw new RuntimeException("Could not get attrib location for uSTMatrix");
		}

		/*
		 * Create our texture. This has to be done each time the
		 * surface is created.
		 */

		int[] textures = new int[1];
		GLES20.glGenTextures(1, textures, 0);

		textureID = textures[0];
		GLES20.glBindTexture(GL_TEXTURE_2D, textureID);
		checkGlError("glBindTexture mTextureID");

		GLES20.glTexParameterf(GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameterf(GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

		checkGlError("glTexParameteri mTextureID");

		//Matrix.setLookAtM(mVMatrix, 0, 0, 0, 5f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
		Matrix.setLookAtM(vMatrix, 0, 0, 0, -5, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
	}

	private int loadShader(int shaderType, String source) {
		int shader = GLES20.glCreateShader(shaderType);
		if (shader != 0) {
			GLES20.glShaderSource(shader, source);
			GLES20.glCompileShader(shader);
			int[] compiled = new int[1];
			GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
			if (compiled[0] == 0) {
				Log.e(TAG, "Could not compile shader " + shaderType + ":");
				Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
				GLES20.glDeleteShader(shader);
				shader = 0;
			}
		}
		return shader;
	}

	private int createProgram(String vertexSource, String fragmentSource) {
		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
		if (vertexShader == 0) {
			return 0;
		}
		int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
		if (pixelShader == 0) {
			return 0;
		}

		int program = GLES20.glCreateProgram();
		if (program != 0) {
			GLES20.glAttachShader(program, vertexShader);
			checkGlError("glAttachShader");
			GLES20.glAttachShader(program, pixelShader);
			checkGlError("glAttachShader");
			GLES20.glLinkProgram(program);
			int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
			if (linkStatus[0] != GLES20.GL_TRUE) {
				Log.e(TAG, "Could not link program: ");
				Log.e(TAG, GLES20.glGetProgramInfoLog(program));
				GLES20.glDeleteProgram(program);
				program = 0;
			}
		}
		return program;
	}

	private void checkGlError(String op) {
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			Log.e(TAG, op + ": glError " + error);
			throw new RuntimeException(op + ": glError " + error);
		}
	}
	private static final int FLOAT_SIZE_BYTES = 4;
	private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
	private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
	private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
	private final float[] mTriangleVerticesData = {
		// X, Y, Z, U, V
		-1.0f, -1.0f, 0, 0.f, 0.f,
		1.0f, -1.0f, 0, 1.f, 0.f,
		-1.0f, 1.0f, 0, 0.f, 1.f,
		1.0f, 1.0f, 0, 1.f, 1.f,};
	private FloatBuffer triangleVertices;
	private final String paintVS =
			"uniform mat4 uMVPMatrix;\n"
			+ "uniform mat4 uSTMatrix;\n"
			+ "uniform float uCRatio;\n"
			+ "attribute vec4 aPosition;\n"
			+ "attribute vec4 aTextureCoord;\n"
			+ "varying vec2 vTextureCoord;\n"
			+ "varying vec2 vTextureNormCoord;\n"
			+ "void main() {\n"
			+ "  gl_Position = uMVPMatrix * aPosition;\n"
			+ "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n"
			+ "  vTextureNormCoord = aTextureCoord.xy;\n"
			+ "}\n";
	private final String paintFS =
			"precision mediump float;\n"
			+ "varying vec2 vTextureCoord;\n"
			+ "varying vec2 vTextureNormCoord;\n"
			+ "uniform sampler2D sTexture;\n"
			+ "void main() {\n"
			+ "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n"
			+ "}\n";
	private float[] vpMatrix = new float[16];
	private float[] projMatrix = new float[16];
	private float[] matrix = new float[16];
	private float[] vMatrix = new float[16];
	private float[] stMatrix = new float[16];
	private int paintTexture;
	private int textureID;
	private int vpMatrixHandle;
	private int stMatrixHandle;
	private int positionHandle;
	private int textureHandle;
	private Context context;
	private static String TAG = "JJGLRenderer";
}
