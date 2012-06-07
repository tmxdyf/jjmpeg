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
package au.notzed.jjmpeg.mediaplayer;

import android.content.Context;
import android.opengl.GLES20;
import static android.opengl.GLES20.*;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Debug;
import android.util.Log;
import au.notzed.jjmpeg.AVFrame;
import au.notzed.jjmpeg.PixelFormat;
import au.notzed.jjmpeg.io.JJQueue;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.microedition.khronos.egl.*;
import javax.microedition.khronos.opengles.GL10;

/**
 *
 * @author notzed
 */
public class GLESVideoRenderer implements GLSurfaceView.Renderer {

	boolean bindTexture = false;
	boolean dataChanged = false;
	int vwidth, vheight;
	int twidth, theight;
	int pwidth, pheight;
	long lastpts;
	long thread;
	long threadLast;
	GLVideoView surface;
	static final int NBUFFERS = 3;
	GLTextureFrame[] bufferArray;
	JJQueue<GLTextureFrame> buffers = new JJQueue<GLTextureFrame>(NBUFFERS);
	JJQueue<GLTextureFrame> ready = new JJQueue<GLTextureFrame>(NBUFFERS);

	public GLESVideoRenderer(Context context, GLVideoView view) {
		this.context = context;
		this.surface = view;

		triangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
		triangleVertices.put(mTriangleVerticesData).position(0);

		Matrix.setIdentityM(stMatrix, 0);
		Matrix.setIdentityM(matrix, 0);
	}

	public long getPosition() {
		return lastpts;
	}

	public void postSeek(long position) {
		lastpts = position;
		startms = -1;
	}

	/**
	 * Video frame for pseudo "direct rendering" of AVFrames
	 */
	class GLTextureFrame extends VideoFrame implements Runnable {

		int[] textures = new int[3];
		boolean create = true;
		// current frame for run callback
		AVFrame frame;

		void genTextures() {
			GLES20.glGenTextures(3, textures, 0);

			for (int i = 0; i < 3; i++) {
				GLES20.glBindTexture(GL_TEXTURE_2D, textures[i]);
				glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
				glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			}
			create = true;
		}

		synchronized void sync() {
			create = false;
			notify();
		}

		synchronized void load() {
			surface.queueEvent(this);
			try {
				this.wait();
			} catch (InterruptedException ex) {
				Logger.getLogger(GLESVideoRenderer.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		@Override
		public void setFrame(AVFrame frame) {
			if (GLVideoView.useShared) {
				if (surface.bindSharedcontext()) {
					PixelFormat fmt = PixelFormat.PIX_FMT_YUV420P;

					//System.out.println("load texture " + this);

					long n = System.nanoTime();
					frame.loadTexture2D(fmt, vwidth, vheight, create, textures[0], textures[1], textures[2]);

					n = (System.nanoTime() - n) / 1000;
					if (n > 8000)
						System.out.printf("SLOW tex load: %d.%06ds\n", n / 1000000, n % 1000000);
				} else {
					System.out.println("no surface (yet?)");
				}
			} else {
				this.frame = frame;
				load();
			}
		}

		@Override
		void enqueue() throws InterruptedException {
			//System.out.println("enqueue: " + this);
			if (NBUFFERS > 1)
				ready.offer(this);
			// this wont work with the throttle mechanism used
			//surface.requestRender();
		}

		@Override
		void recycle() {
			//System.out.println("recycle: " + this);
			if (NBUFFERS > 1)
				buffers.offer(this);
		}

		public void run() {
			PixelFormat fmt = PixelFormat.PIX_FMT_YUV420P;

			long n = System.nanoTime();
			frame.loadTexture2D(fmt, vwidth, vheight, create, textures[0], textures[1], textures[2]);
			n = (System.nanoTime() - n) / 1000;
			if (n > 8000)
				System.out.printf("SLOW tex load took: %d.%06ds\n", n / 1000000, n % 1000000);
			sync();
		}
	}

	public VideoFrame getFrame() throws InterruptedException {
		if (NBUFFERS > 1) {
			//VideoFrame vf = buffers.take();
			VideoFrame vf;

			vf = buffers.take();
			return vf;
		} else {
			// just use the same texture over and over
			return buffers.peek();
		}
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
	boolean direct = false;
	long startms = -1;
	GLTextureFrame display;

	public void onDrawFrame(GL10 glUnused) {
		if (thread == 0)
			thread = Debug.threadCpuTimeNanos();

		// Find a frame that's ready for display
		// TODO: mess ...
		if (NBUFFERS > 1) {
			GLTextureFrame peek;
			long delay;
			do {
				peek = ready.peek();

				if (peek != null) {
					long pts = peek.pts;
					long targetms = pts + startms;
					long now = System.currentTimeMillis();

					if (startms == -1) {
						startms = now - pts;
						//startms = now - seekoffset;
						delay = 0;
					} else {
						delay = targetms - now;
					}

					// max speed
					//delay = -1;

					if (delay > 50) {
						System.out.println("weird delay " + delay + " pts " + peek.pts + " now = " + (now - startms));
						startms = -1;
					}

					if (delay <= 0) {
						// dump head
						ready.poll();
						if (display != null)
							display.recycle();
						display = peek;
					}
				} else {
					delay = 1;
				}
			} while (delay <= 0);
		} else {
			display = buffers.peek();
		}
		//System.out.println("display: " + display);

		if (bindTexture) {
			Matrix.setIdentityM(stMatrix, 0);
			Matrix.scaleM(stMatrix, 0, (float) vwidth / twidth, (float) vheight / theight, 1);
		}

		if (display == null)
			return;

		lastpts = display.getPTS();

		// Ignore the passed-in GL10 interface, and use the GLES20
		// class's static methods instead.
		glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
		glUseProgram(paintTexture);
		checkGlError("glUseProgram");

		glActiveTexture(GLES20.GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, display.textures[0]);
		glActiveTexture(GLES20.GL_TEXTURE1);
		glBindTexture(GL_TEXTURE_2D, display.textures[1]);
		glActiveTexture(GLES20.GL_TEXTURE2);
		glBindTexture(GL_TEXTURE_2D, display.textures[2]);

		glUniform1i(texY, 0);
		glUniform1i(texU, 1);
		glUniform1i(texV, 2);

		triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
		glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
		checkGlError("glVertexAttribPointer maPosition");
		glEnableVertexAttribArray(positionHandle);
		checkGlError("glEnableVertexAttribArray maPositionHandle");

		triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
		glVertexAttribPointer(textureHandle, 3, GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
		checkGlError("glVertexAttribPointer maTextureHandle");
		glEnableVertexAttribArray(textureHandle);
		checkGlError("glEnableVertexAttribArray maTextureHandle");

		Matrix.setIdentityM(matrix, 0);
		Matrix.multiplyMM(vpMatrix, 0, vMatrix, 0, matrix, 0);
		Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, vpMatrix, 0);

		GLES20.glUniformMatrix4fv(vpMatrixHandle, 1, false, vpMatrix, 0);
		GLES20.glUniformMatrix4fv(stMatrixHandle, 1, false, stMatrix, 0);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		checkGlError("glDrawArrays");

		threadLast = Debug.threadCpuTimeNanos();
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

		vpMatrixHandle = getUniform(paintTexture, "uMVPMatrix");
		stMatrixHandle = getUniform(paintTexture, "uSTMatrix");

		texY = getUniform(paintTexture, "yTexture");
		texU = getUniform(paintTexture, "uTexture");
		texV = getUniform(paintTexture, "vTexture");

		System.err.println("create textures");
		if (bufferArray == null) {
			bufferArray = new GLTextureFrame[NBUFFERS];
			for (int i = 0; i < NBUFFERS; i++) {
				GLTextureFrame tf = new GLTextureFrame();
				bufferArray[i] = tf;
				buffers.offer(tf);
			}
		}
		for (int i = 0; i < NBUFFERS; i++) {
			bufferArray[i].genTextures();
		}
		checkGlError("glTexParameteri mTextureID");

		//Matrix.setLookAtM(mVMatrix, 0, 0, 0, 5f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
		Matrix.setLookAtM(vMatrix, 0, 0, 0, -5, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
	}

	private int getUniform(int prog, String name) {
		int res = glGetUniformLocation(prog, name);
		checkGlError("glGetUniformLocation " + name);
		if (res == -1) {
			//throw new RuntimeException("Could not get location for " + name);
		}
		return res;
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
			+ "uniform sampler2D yTexture;\n"
			+ "uniform sampler2D uTexture;\n"
			+ "uniform sampler2D vTexture;\n"
			+ "const mat3 yuv2rgb = mat3(\n"
			+ "1, 0, 1.2802,\n"
			+ " 1, -0.214821, -0.380589,\n"
			+ " 1, 2.127982, 0\n"
			+ " );\n"
			+ "void main() {\n"
			+ "	vec3 yuv = vec3(1.1643 * ("
			+ "		texture2D(yTexture, vTextureCoord).r - 0.0625),\n"
			+ "		texture2D(uTexture, vTextureCoord).r - 0.5,\n"
			+ "		texture2D(vTexture, vTextureCoord).r - 0.5\n"
			+ "	);\n"
			+ "	vec3 rgb = yuv * yuv2rgb;\n"
			+ "	gl_FragColor = vec4(rgb, 1.0);\n"
			+ "}\n";
	private float[] vpMatrix = new float[16];
	private float[] projMatrix = new float[16];
	private float[] matrix = new float[16];
	private float[] vMatrix = new float[16];
	private float[] stMatrix = new float[16];
	private int paintTexture;
	private int texY, texU, texV;
	private int vpMatrixHandle;
	private int stMatrixHandle;
	private int positionHandle;
	private int textureHandle;
	private Context context;
	private static String TAG = "JJGLRenderer";
}
