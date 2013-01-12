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
import au.notzed.jjmpeg.util.JJQueue;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.microedition.khronos.egl.*;
import javax.microedition.khronos.opengles.GL10;

/**
 * Needs a good deal of work but works somewhat.
 * @author notzed
 */
public class GLESVideoRenderer implements GLSurfaceView.Renderer {

	boolean paused = false;
	private final MediaClock clock;

	public void pause() {
		paused = true;
	}

	public void play() {
		paused = false;
	}

	enum Mode {

		// load synchronously - too bloody slow
		SYNC_LOAD,
		// Only works with some codecs/settings
		ENQUEUE_DIRECT,
		// Copy the raw frame first
		ENQUEUE_COPY,
		// Convert to rgb565, and set that as texture
		ENQUEUE_RGB
	};

	public class TimeInfo {

		long start;
		long last;
		long total;
		int count;

		public void start() {
			start = System.nanoTime();
		}

		public void end() {
			add((System.nanoTime() - start) / 1000);
		}

		public void add(long t) {
			last = t;
			total += last;
			count += 1;
		}

		public long average() {
			if (count == 0)
				return 0;
			return total / count;
		}

		@Override
		public String toString() {
			long a = average();
			return String.format("%3d.%03d %3d.%03d", last / 1000, last % 1000, a / 1000, a % 1000);
		}
	}
	Mode mode = Mode.ENQUEUE_COPY;
	// how many buffers to use, must be > 1 if enqueueFrames used
	static final int NBUFFERS = 31;
	boolean updateView = false;
	boolean dataChanged = false;
	boolean stopped = false;
	int vwidth, vheight;
	int twidth, theight;
	int pwidth, pheight;
	long lastpts;
	long thread;
	long threadLast;
	GLVideoView surface;
	GLTextureFrame[] bufferArray;
	JJQueue<GLTextureFrame> buffers = new JJQueue<GLTextureFrame>(NBUFFERS);
	JJQueue<GLTextureFrame> ready = new JJQueue<GLTextureFrame>(NBUFFERS);
	int seeked = 0;
	// profiling stuff
	// raw load time
	TimeInfo load = new TimeInfo();
	TimeInfo sync = new TimeInfo();
	TimeInfo copy = new TimeInfo();
	TimeInfo render = new TimeInfo();
	TimeInfo decode = new TimeInfo();
	int framesDropped = 0;
	int[] atextures = new int[3];
	int lastSequence;

	public GLESVideoRenderer(Context context, GLVideoView view, MediaClock clock) {
		this.context = context;
		this.surface = view;

		triangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
		triangleVertices.put(mTriangleVerticesData).position(0);

		Matrix.setIdentityM(stMatrix, 0);
		Matrix.setIdentityM(matrix, 0);
		this.clock = clock;
	}

	public long getPosition() {
		return lastpts;
	}

	// When finishing off, indicates no more work to be done
	public void stop() {
		stopped = true;
	}

	/**
	 * Video frame for pseudo "direct rendering" of AVFrames
	 */
	class GLTextureFrame extends VideoFrame implements Runnable {

		// NB: This is only used in SYNC_LOAD mode
		int[] textures = new int[3];
		boolean create = true;
		// current frame for run callback
		AVFrame frame;
		AVFrame srcFrame;
		int rwidth;
		ByteBuffer rgb;
		ShortBuffer rgbs;

		void genTextures() {
			if (mode == Mode.SYNC_LOAD) {
				GLES20.glGenTextures(3, textures, 0);

				for (int i = 0; i < 3; i++) {
					GLES20.glBindTexture(GL_TEXTURE_2D, textures[i]);
					//glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
					glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
					glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
					glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
					glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
				}
			}
			create = true;
		}

		synchronized void sync() {
			create = false;
			notify();
		}

		synchronized void load() {
			sync.start();
			long now = System.nanoTime();
			surface.queueEvent(this);
			try {
				this.wait();
			} catch (InterruptedException ex) {
				Logger.getLogger(GLESVideoRenderer.class.getName()).log(Level.SEVERE, null, ex);
			}
			sync.end();
		}

		@Override
		public void setFrame(AVFrame frame) {
			//System.out.println("set frame: " + frame);

			switch (mode) {
				case SYNC_LOAD:
					this.srcFrame = frame;
					load();
					break;
				case ENQUEUE_DIRECT:
					// Use frame as frame
					this.srcFrame = frame;
					break;
				case ENQUEUE_COPY:
					copy.start();
					if (this.frame == null)
						this.frame = AVFrame.create(PixelFormat.PIX_FMT_YUV420P, vwidth, vheight);
					this.frame.copy(frame, PixelFormat.PIX_FMT_YUV420P, vwidth, vheight);
					copy.end();
					break;
				case ENQUEUE_RGB:
					if (rgb == null) {
						rwidth = (vwidth + 15) & (~15);
						rgb = ByteBuffer.allocateDirect(rwidth * vheight * 2).order(ByteOrder.nativeOrder());
						rgbs = rgb.asShortBuffer();
					}

					copy.start();
					frame.toRGB565(PixelFormat.PIX_FMT_YUV420P, vwidth, vheight, rgb);
					copy.end();
					break;
			}

			if (true)
				return;

			if (false) {
				if (this.frame == null)
					this.frame = AVFrame.create(PixelFormat.PIX_FMT_YUV420P, vwidth, vheight);
				this.frame.copy(frame, PixelFormat.PIX_FMT_YUV420P, vwidth, vheight);
			} else {
				//this.frame = frame;
				this.srcFrame = frame;
			}
			/*
			 if (enqueueFrames) {
			 // do nothing
			 } else if (GLVideoView.useShared) {
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
			 load();
			 }*/
		}

		@Override
		void enqueue() throws InterruptedException {
			//if (this.frame == null)
			//	this.frame = AVFrame.create(PixelFormat.PIX_FMT_YUV420P, vwidth, vheight);
			//this.frame.copy(srcFrame, PixelFormat.PIX_FMT_YUV420P, vwidth, vheight);

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

		// NB: not used
		public void run() {
			//System.out.println("shit");
			loadTexture();
			sync();
		}

		@Override
		public AVFrame getFrame() {
			if (this.frame == null)
				this.frame = AVFrame.create(PixelFormat.PIX_FMT_YUV420P, vwidth, vheight);
			return frame;
		}

		/**
		 * Load texture to GL, call from GL draw callback
		 */
		void loadTexture() {
			load.start();

			PixelFormat fmt = PixelFormat.PIX_FMT_YUV420P;
			switch (mode) {
				case SYNC_LOAD:
					srcFrame.loadTexture2D(fmt, vwidth, vheight, create, textures[0], textures[1], textures[2]);
					break;
				case ENQUEUE_DIRECT:
				case ENQUEUE_COPY:
					frame.loadTexture2D(fmt, vwidth, vheight, create, atextures[0], atextures[1], atextures[2]);
					break;
				case ENQUEUE_RGB:
					// Just a hack here.
					//AVPlane p = frame.getPlaneAt(0, fmt, vwidth, vheight);

					GLES20.glBindTexture(GL_TEXTURE_2D, atextures[0]);
					if (create) {
						GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, twidth, theight, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_SHORT_5_6_5, null);
					}
					GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, rwidth, vheight, GLES20.GL_RGB, GLES20.GL_UNSIGNED_SHORT_5_6_5, rgb);
					break;
			}
			load.end();
		}
	}
	int dropped = 0;
	int dropLimit = 8;

	public VideoFrame getFrame() throws InterruptedException {
		// It saves enough to catch up most of the time.
		if (lag > 100 && dropped < dropLimit) {
			dropped++;
			framesDropped++;
			//System.out.println("lagged, dropping frame");
			return null;
		}
		dropped = 0;

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
			updateView = true;
			twidth = tw;
			theight = th;
		}
		this.vwidth = w;
		this.vheight = h;
	}

	public void setVideoAspect(float aspect) {
		this.aspect = aspect;
		updateView = true;
	}
	float aspect = 1;
	boolean direct = false;
	GLTextureFrame display;
	long lag;

	public void onDrawFrame(GL10 glUnused) {
		if (stopped)
			return;

		if (clock.isPaused())
			return;

		if (thread == 0)
			thread = Debug.threadCpuTimeNanos();
		GLTextureFrame displayNew = null;

		// Find a frame that's ready for display
		// TODO: mess ...
		if (NBUFFERS > 1) {
			GLTextureFrame peek;
			long delay;
			do {
				peek = ready.peek();

				if (peek != null) {
					if (peek.sequence != clock.getSequence()) {
						ready.poll();
						if (displayNew != null)
							displayNew.recycle();
						displayNew = peek;
						delay = -1;
						continue;
					}

					long pts = peek.pts;

					delay = clock.getVideoDelay(pts);
					// max speed
					//delay = -1;

					//if (delay > 150) {
					//	System.out.println("weird delay " + delay + " pts " + peek.pts);
					//}

					if (delay <= 0) {
						lag = -delay;
						// dump head
						ready.poll();
						if (displayNew != null)
							displayNew.recycle();
						displayNew = peek;
					} else {
						lag = 0;
					}
				} else {
					delay = 1;
				}
			} while (delay <= 0);
		} else {
			display = buffers.peek();
		}
		//System.out.println("display: " + display);

		if (updateView) {
			Matrix.setIdentityM(stMatrix, 0);

			float rw = 1;
			float rh = 1;
			float pw = (float) vwidth * aspect / pwidth;
			float ph = (float) vheight / pheight;
			if (vwidth != 0 && vheight != 0 && pwidth != 0 && pheight != 0) {
				if (pw > ph) {
					rh = pw / ph;
				} else {
					rw = ph / pw;
				}
			}
			System.out.printf("pw %f ph %f rw %f rh %f  vsize %dx%d psize %dx%d aspect %f\n", pw, ph, rw, rh, vwidth, vheight, pwidth, pheight, aspect);

			// -1 just makes sure the texture fits over to avoid flickering crap
			Matrix.scaleM(stMatrix, 0, (float) (vwidth - 1) / twidth, (float) (vheight - 1) / theight, 1);

			Matrix.orthoM(projMatrix, 0, rw, -rw, rh, -rh, 3, 7);

			updateView = false;
		}

		if (displayNew != null)
			display = displayNew;

		if (display == null)
			return;

		decode.add(display.decodeTime);

		// note that we can recycle the buffer as soon as we've loaded the texture
		switch (mode) {
			case SYNC_LOAD:
				if (displayNew != null) {
					displayNew.recycle();
				}
				break;
			case ENQUEUE_DIRECT:
			case ENQUEUE_COPY:
			case ENQUEUE_RGB:
				if (displayNew != null) {
					displayNew.loadTexture();
					displayNew.recycle();
				}
				break;
		}

		lastpts = display.getPTS();


		// Ignore the passed-in GL10 interface, and use the GLES20
		// class's static methods instead.
		//glClear(GL_COLOR_BUFFER_BIT);

		render.start();

		glUseProgram(paintTexture);
		checkGlError("glUseProgram");

		if (mode == Mode.SYNC_LOAD) {
			glActiveTexture(GLES20.GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, display.textures[0]);
			glActiveTexture(GLES20.GL_TEXTURE1);
			glBindTexture(GL_TEXTURE_2D, display.textures[1]);
			glActiveTexture(GLES20.GL_TEXTURE2);
			glBindTexture(GL_TEXTURE_2D, display.textures[2]);
		} else {
			glActiveTexture(GLES20.GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, atextures[0]);
			glActiveTexture(GLES20.GL_TEXTURE1);
			glBindTexture(GL_TEXTURE_2D, atextures[1]);
			glActiveTexture(GLES20.GL_TEXTURE2);
			glBindTexture(GL_TEXTURE_2D, atextures[2]);
		}
		if (mode == Mode.ENQUEUE_RGB) {
			glUniform1i(texY, 0);
		} else {
			glUniform1i(texY, 0);
			glUniform1i(texU, 1);
			glUniform1i(texV, 2);
		}

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

		//GLES20.glFinish();
		render.end();
	}

	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		System.out.println("surface changed");

		// HACK: Try not to resize when the navigation bar is shown/hidden.
		pwidth = Math.max(width, pwidth);
		pheight = Math.max(height, pheight);

		// HACK: Offset by difference if we're 'shrunk' to fir the nav bar.
		GLES20.glViewport(0, height - pheight, pwidth, pheight);

		Matrix.orthoM(projMatrix, 0, 1, -1, 1, -1, 3, 7);
		updateView = true;
	}

	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		// Ignore the passed-in GL10 interface, and use the GLES20
		// class's static methods instead.

		/* Set up alpha blending and an Android background color */
		// wtf do i want blending for?
		GLES20.glDisable(GLES20.GL_BLEND);
		//GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		glClearColor(0, 0, 0, 0);

		/* Set up shaders and handles to their variables */
		if (this.mode == Mode.ENQUEUE_RGB)
			paintTexture = createProgram(vs_rgb, fs_rgb);
		else
			paintTexture = createProgram(vs_yuv, fs_yuv);
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

		if (this.mode == Mode.ENQUEUE_RGB) {
			texY = getUniform(paintTexture, "sTexture");
		} else {
			texY = getUniform(paintTexture, "yTexture");
			texU = getUniform(paintTexture, "uTexture");
			texV = getUniform(paintTexture, "vTexture");
		}
		checkGlError("texture shit");

		System.err.println("create textures");
		if (bufferArray == null) {
			bufferArray = new GLTextureFrame[NBUFFERS];
			for (int i = 0; i < NBUFFERS; i++) {
				GLTextureFrame tf = new GLTextureFrame();
				bufferArray[i] = tf;
				buffers.offer(tf);
			}
		}
		if (mode == Mode.SYNC_LOAD) {
			for (int i = 0; i < NBUFFERS; i++) {
				bufferArray[i].genTextures();
			}
		} else {
			GLES20.glGenTextures(3, atextures, 0);

			for (int i = 0; i < 3; i++) {
				GLES20.glBindTexture(GL_TEXTURE_2D, atextures[i]);
				//glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
				glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
				glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			}
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
		//System.out.println("Load vs: \n" + vertexSource);
		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
		if (vertexShader == 0) {
			return 0;
		}
		//System.out.println("Load fs: \n" + fragmentSource);
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
	private final String vs_yuv =
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
	private final String fs_yuv =
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
	private final String vs_rgb = vs_yuv;
	private final String fs_rgb =
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
	private int texY, texU, texV;
	private int vpMatrixHandle;
	private int stMatrixHandle;
	private int positionHandle;
	private int textureHandle;
	private Context context;
	private static String TAG = "JJGLRenderer";
}
