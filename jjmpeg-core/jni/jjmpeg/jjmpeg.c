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

/*
 * Contains hand-rolled interfaces
 */

//#define d(x) x; fflush(stdout)
#define d(x)

#include "jjmpeg-jni.h"

static int init_platform(JNIEnv *env);

#include "jjmpeg-jni.c"

#include "jjmpeg-platform.c"

static jmethodID byteio_readPacket;
static jmethodID byteio_writePacket;
static jmethodID byteio_seek;

/* ********************************************************************** */
// Returns 0 on success
static int init_local(JNIEnv *env) {
#ifdef ENABLE_DL
	DLOPEN(avutil_lib, "avutil", LIBAVUTIL_VERSION_MAJOR);
	DLOPEN(avcodec_lib, "avcodec", LIBAVCODEC_VERSION_MAJOR);
	DLOPEN(avformat_lib, "avformat", LIBAVFORMAT_VERSION_MAJOR);
	DLOPEN(swscale_lib, "swscale", LIBSWSCALE_VERSION_MAJOR);
	DLOPEN(swresample_lib, "swresample", LIBSWRESAMPLE_VERSION_MAJOR);
#endif
	jclass byteioclass = (*env)->FindClass(env, "au/notzed/jjmpeg/AVIOContext");
	if (byteioclass == NULL)
		;
	byteio_readPacket = (*env)->GetMethodID(env, byteioclass, "readPacket", "(Ljava/nio/ByteBuffer;)I");
	byteio_writePacket = (*env)->GetMethodID(env, byteioclass, "writePacket", "(Ljava/nio/ByteBuffer;)I");
	byteio_seek = (*env)->GetMethodID(env, byteioclass, "seek", "(JI)J");

	jclass class;
	class = (*env)->FindClass(env, "au/notzed/jjmpeg/ObjectHolder");
	if (class != NULL) {
		ObjectHolder_value = (*env)->GetFieldID(env, class, "value", "Ljava/lang/Object;");
	}
	class = (*env)->FindClass(env, "au/notzed/jjmpeg/LongHolder");
	if (class != NULL) {
		LongHolder_value = (*env)->GetFieldID(env, class, "value", "J");
	}
	class = (*env)->FindClass(env, "au/notzed/jjmpeg/IntHolder");
	if (class != NULL) {
		IntHolder_value = (*env)->GetFieldID(env, class, "value", "I");
	}

	return 0;
}

/* ********************************************************************** */

/**
 * Wraps a pointer to a structure into a bytebuffer
 */
JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_AVNative_getPointer
(JNIEnv *env, jclass jc, jobject jbase, jint offset, jint size) {
        void *base = ADDR(jbase);

        base += offset;

        return WRAP(((void **)base)[0], size);
}

/**
 * Takes the element of a pointer array and wraps it in a bytebuffer
 */
JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_AVNative_getPointerIndex
(JNIEnv *env, jclass jc, jobject jbase, jint offset, jint size, jint index) {
        void *base = ADDR(jbase);

        base += offset;

        return WRAP(((void **)base)[index], size);
}

JNIEXPORT void JNICALL Java_au_notzed_jjmpeg_AVNative_getVersions
(JNIEnv *env, jclass jc, jobject jvers) {
        int *vers = ADDR(jvers);

	vers[0] = LIBAVFORMAT_VERSION_MAJOR;
	vers[1] = LIBAVCODEC_VERSION_MAJOR;
	vers[2] = LIBAVUTIL_VERSION_MAJOR;

	printf("versions = %d %d %d\n", vers[0], vers[1], vers[2]); fflush(stdout);
}


/* ********************************************************************** */

JNIEXPORT void JNICALL Java_au_notzed_jjmpeg_AVCodecContextNative_free
(JNIEnv *env, jobject jptr) {
	void *ptr = PTR(jptr, AVCodecContext);

	if (ptr)
		CALLDL(av_free)(ptr);
}

static int jjmpeg_get_buffer(struct AVCodecContext *c, AVFrame *pic) {
	//LOGI("get buffer: %p", pic);
	return CALLDL(avcodec_default_get_buffer)(c, pic);
}

static void jjmpeg_release_buffer(struct AVCodecContext *c, AVFrame *pic) {
	//LOGI("release buffer: %p", pic);
	CALLDL(avcodec_default_release_buffer)(c, pic);
}

static int jjmpeg_reget_buffer(struct AVCodecContext *c, AVFrame *pic) {
	//LOGI("reget buffer: %p", pic);
	return CALLDL(avcodec_default_reget_buffer)(c, pic);
}

/*
  Sets up some tracing allocators for debugging */
JNIEXPORT void JNICALL Java_au_notzed_jjmpeg_AVCodecContextNative_debug
(JNIEnv *env, jobject jptr) {
	AVCodecContext *ptr = PTR(jptr, AVCodecContext);

	if (ptr) {
		ptr->get_buffer = jjmpeg_get_buffer;
		ptr->release_buffer = jjmpeg_release_buffer;
		ptr->reget_buffer = jjmpeg_reget_buffer;
	}
}

/* ********************************************************************** */

JNIEXPORT jint JNICALL Java_au_notzed_jjmpeg_AVFormatContextNative_findStreamInfo
(JNIEnv *env, jobject jptr, jobjectArray joptions) {
	AVFormatContext *ptr = PTR(jptr, AVFormatContext);
	int len = 0;
	AVDictionary **options = NULL;
	jobject *array;
	int res;
	int i;

	if (joptions != NULL) {
		len = (*env)->GetArrayLength(env, joptions);

		if (len != ptr->nb_streams) {
			fprintf(stderr, "invalid number of stream options\n");
			fflush(stderr);
			return -1;
		}

		options = alloca(sizeof(*options) * len);
		array = alloca(sizeof(*array) * len);
		for (i=0;i<len;i++) {
			jobject e = (*env)->GetObjectArrayElement(env, joptions, i);

			array[i] = e;
			options[i] = PTR(e, AVDictionary);
		}
	}
	
	res = CALLDL(avformat_find_stream_info)(ptr, options);

	if (joptions != NULL) {
		for (i=0;i<len;i++) {
			SET_PTR(array[i], AVDictionary, options[i]);
		}
	}

	return res;
}

JNIEXPORT jint JNICALL Java_au_notzed_jjmpeg_AVFormatContextNative_open_1input
(JNIEnv *env, jclass jc, jobject jps, jstring jfilename, jobject jfmt, jobject joptions) {
	AVFormatContext * ps = PTR(jps, AVFormatContext);
	const char * filename = STR(jfilename);
	AVInputFormat * fmt = PTR(jfmt, AVInputFormat);
	AVDictionary * options = PTR(joptions, AVDictionary);

	jint res = CALLDL(avformat_open_input)(&ps, filename, fmt, &options);

	SET_PTR(jps, AVFormatContext, ps);
	RSTR(jfilename, filename);
	SET_PTR(joptions, AVDictionary, options);

	return res;
}

JNIEXPORT void JNICALL Java_au_notzed_jjmpeg_AVFormatContextNative_close_1input
(JNIEnv *env, jclass jc, jobject js) {
	AVFormatContext * s = PTR(js, AVFormatContext);

	if (s)
		CALLDL(avformat_close_input)(&s);
	// else throw nullpointerexception
	SET_PTR(js, AVFormatContext, s);
}

JNIEXPORT jint JNICALL Java_au_notzed_jjmpeg_AVFormatContextNative_write_1header
(JNIEnv *env, jobject jo, jobject joptions) {
	AVFormatContext *cptr = PTR(jo, AVFormatContext);
	AVDictionary * options = PTR(joptions, AVDictionary);

	jint res = CALLDL(avformat_write_header)(cptr, &options);

	SET_PTR(joptions, AVDictionary, options);

	return res;
}


/* ********************************************************************** */

JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_AVFrameNative_getPlaneAt
(JNIEnv *env, jobject jptr, jint index, jint fmt, jint width, jint height) {
	AVFrame *cptr = PTR(jptr, AVFrame);

	//LOGI("get plane %d, linesize[] = %d data = %p\n", index, cptr->linesize[index], cptr->data[index]);

	// FIXME: this depends on pixel format
	// TODO: keep this in java?
	if (index > 0)
		height /= 2;

	int size = cptr->linesize[index] * height;

	return WRAP(cptr->data[index], size);
}

JNIEXPORT void JNICALL Java_au_notzed_jjmpeg_AVFrameNative_freeFrame
(JNIEnv *env, jobject jptr) {
	AVFrame *cptr = PTR(jptr, AVFrame);

	if (cptr)
		CALLDL(av_free)(cptr);
	// else throw nullpointer exception
}

#ifdef ENABLE_GLES2
#include <GLES2/gl2.h>

static int texture_size(int v) {
	int n = 1 << (31 - __builtin_clz(v));

	if (v != n)
		return n<<1;
	else
		return n;
}

JNIEXPORT void JNICALL Java_au_notzed_jjmpeg_AVFrameNative_loadTexture2D
(JNIEnv *env, jobject jptr, jint fmt, jint width, jint height, jboolean create, jint t0, jint t1, jint t2) {
	AVFrame *cptr = PTR(jptr, AVFrame);

	// FIXME: handle different formats

	int twidth = texture_size(width);
	int theight = texture_size(height);

	glPixelStorei(GL_PACK_ALIGNMENT, 4);

	glBindTexture(GL_TEXTURE_2D, t0);
	if (create)
		glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, twidth, theight, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, NULL);
	glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, cptr->linesize[0], height, GL_LUMINANCE, GL_UNSIGNED_BYTE, cptr->data[0]);
	
	glBindTexture(GL_TEXTURE_2D, t1);
	if (create)
		glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, twidth/2, theight/2, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, NULL);
	glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, cptr->linesize[1], height/2, GL_LUMINANCE, GL_UNSIGNED_BYTE, cptr->data[1]);
	
	glBindTexture(GL_TEXTURE_2D, t2);
	if (create)
		glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, twidth/2, theight/2, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, NULL);
	glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, cptr->linesize[2], height/2, GL_LUMINANCE, GL_UNSIGNED_BYTE, cptr->data[2]);
}
#endif

// Audio stuff for AVFrame


JNIEXPORT jint JNICALL Java_au_notzed_jjmpeg_AVFrameNative_getSamples
(JNIEnv *env, jobject jptr, jint fmt, int channels, jshortArray jsamples) {
	AVFrame *frame = PTR(jptr, AVFrame);
	int jlen = (*env)->GetArrayLength(env, jsamples);
	int planesize;
	int clen = CALLDL(av_samples_get_buffer_size)(&planesize, channels, frame->nb_samples, fmt, 1) / 2;

	// TODO: format must be S16

	if (jlen < clen) {
		LOGI("getSamples() short array write had %d needed %d", jlen, clen);
		clen = jlen;
	}

	(*env)->SetShortArrayRegion(env, jsamples, 0, clen, (const jshort *)frame->extended_data[0]);

	return clen;
}

/* ********************************************************************** */

JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_AVPacketNative_allocatePacket
(JNIEnv *env, jclass hc) {
	AVPacket *packet = malloc(sizeof(AVPacket));

	if (packet != NULL) {
		packet->data = NULL;
		packet->size = 0;
		CALLDL(av_init_packet)(packet);
	}

	d(printf("alloc packet = %p, data=%p size=%d\n", packet, packet->data, packet->size));

	return NEWOBJ(packet, AVPacket);
}

JNIEXPORT void JNICALL Java_au_notzed_jjmpeg_AVPacketNative_freePacket
(JNIEnv *env, jobject jpacket) {
	AVPacket *packet = PTR(jpacket, AVPacket);

	free(packet);
}

JNIEXPORT jint JNICALL Java_au_notzed_jjmpeg_AVPacketNative_consume
(JNIEnv *env, jobject jptr, jint len) {
	AVPacket *packet = PTR(jptr, AVPacket);

	//printf("consume, packet = %p  data = %p, size = %d\n", packet, packet->data, packet->size); fflush(stdout);

	len = packet->size < len ? packet->size : len;

	packet->data += len;
	packet->size -= len;

	return packet->size;
}

JNIEXPORT void JNICALL Java_au_notzed_jjmpeg_AVPacketNative_setData
(JNIEnv *env, jobject jptr, jobject jp, jint size) {
	AVPacket *packet = PTR(jptr, AVPacket);
	uint8_t *p = ADDR(jp);

	packet->data = p;
	packet->size = size;
}

JNIEXPORT void JNICALL Java_au_notzed_jjmpeg_AVPacketNative_copyPacket
(JNIEnv *env, jobject jptr, jobject jsrc) {
	AVPacket *packet = PTR(jptr, AVPacket);
	AVPacket *src = PTR(jsrc, AVPacket);

	memcpy(packet, src, sizeof(AVPacket));
}

/* ********************************************************************** */

JNIEXPORT jint JNICALL Java_au_notzed_jjmpeg_SwsContextNative_scale
(JNIEnv *env, jobject jptr, jobject jsrc, jint srcSliceY, jint srcSliceH, jobject jdst) {
	struct SwsContext *sws = PTR(jptr, SwsContext);
	struct AVFrame *src = PTR(jsrc, AVFrame);
	struct AVFrame *dst = PTR(jdst, AVFrame);
	jint res;

	res = CALLDL(sws_scale)(sws, (const uint8_t * const *)src->data, src->linesize,
				srcSliceY, srcSliceH,
				dst->data, dst->linesize);

	return res;
}

static int scaleArray(JNIEnv *env, SwsContext *sws, AVFrame *src, jint srcSliceY, jint srcSliceH, jarray jdst, int dsize, jint fmt, jint width, jint height) {
	struct AVPicture dst;
	void *cdst;
	int res = -1;

	if ((*env)->GetArrayLength(env, jdst) * dsize < CALLDL(avpicture_fill)(&dst, NULL, fmt, width, height)) {
		fprintf(stderr, "array too small for scaleIntArray");
		fflush(stderr);
		// FIXME: exception
		return -1;
	}

	cdst = (*env)->GetPrimitiveArrayCritical(env, jdst, NULL);

	if (!cdst) {
		// FIXME: exception
		return res;
	}

	res = CALLDL(avpicture_fill)(&dst, cdst, fmt, width, height);
	if (res >= 0) {
		res = CALLDL(sws_scale)(sws, (const uint8_t * const *)src->data, src->linesize,
					srcSliceY, srcSliceH,
					dst.data, dst.linesize);
	}

	(*env)->ReleasePrimitiveArrayCritical(env, jdst, cdst, 0);

	return res;
}


JNIEXPORT jint JNICALL Java_au_notzed_jjmpeg_SwsContextNative_scaleIntArray
(JNIEnv *env, jobject jptr, jobject jsrc, jint srcSliceY, jint srcSliceH, jintArray jdst, jint fmt, jint width, jint height) {
	struct SwsContext *sws = PTR(jptr, SwsContext);
	struct AVFrame *src = PTR(jsrc, AVFrame);

	return scaleArray(env, sws, src, srcSliceY, srcSliceH, jdst, 4, fmt, width, height);
}

JNIEXPORT jint JNICALL Java_au_notzed_jjmpeg_SwsContextNative_scaleByteArray
(JNIEnv *env, jobject jptr, jobject jsrc, jint srcSliceY, jint srcSliceH, jintArray jdst, jint fmt, jint width, jint height) {
	struct SwsContext *sws = PTR(jptr, SwsContext);
	struct AVFrame *src = PTR(jsrc, AVFrame);

	return scaleArray(env, sws, src, srcSliceY, srcSliceH, jdst, 1, fmt, width, height);
}

/* ********************************************************************** */

JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_SwrContextNative_alloc
(JNIEnv *env, jclass jc, jlong dstLayout, jint dstFormat, jint dstRate, jlong srcLayout, jint srcFormat, jint srcRate) {
	struct SwrContext *swr;

	swr = CALLDL(swr_alloc_set_opts)(NULL, dstLayout, dstFormat, dstRate, srcLayout, srcFormat, srcRate, 0, NULL);
	if (swr)
		CALLDL(swr_init)(swr);

	return NEWOBJ(swr, SwrContext);
}

JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_SwrContextNative_free
(JNIEnv *env, jobject jptr) {
	struct SwrContext *swr = PTR(jptr, SwrContext);

	CALLDL(swr_free)(&swr);

	SET_PTR(jptr, SwrContext, swr);
}

JNIEXPORT jint JNICALL Java_au_notzed_jjmpeg_SwrContextNative_convert
(JNIEnv *env, jobject jptr, jobject jdst, jobject jsrc) {
	struct SwrContext *swr = PTR(jptr, SwrContext);
	struct AVFrame *src = PTR(jsrc, AVFrame);
	struct AVFrame *dst = PTR(jdst, AVFrame);

	// if null ...
	//LOGI("converting src %p len %d dst %p len %d\n", src->data[0], src->nb_samples, dst->data[0], dst->nb_samples);

	return CALLDL(swr_convert)(swr, dst->data, dst->nb_samples, (const uint8_t **)src->data, src->nb_samples);
}

/* ********************************************************************** */

// this rescale_q takes pointer arguments
JNIEXPORT jlong JNICALL Java_au_notzed_jjmpeg_AVRationalNative_jjRescaleQ
(JNIEnv *env, jclass jc, jlong ja, jobject jbq, jobject jcq) {
	AVRational *bqp = PTR(jbq, AVRational);
	AVRational *cqp = PTR(jcq, AVRational);
	jlong res;

	res = CALLDL(av_rescale_q)(ja, *bqp, *cqp);

	return res;
}

/* ********************************************************************** */


/*

  FIXME: this stuff is all broken for android


 */

//JavaVM *vm = NULL;

// This assumes we only get called from threads which invoked the ffmpeg libraries.
// If not, things are a (tad) more complex.
struct avio_data {
	JNIEnv *env;
	jobject jo;
};

/* Wrappers for AVIOContext to allow callbacks to java */

static int AVIOContext_readPacket(void *opaque, uint8_t *buf, int buf_size) {
	struct avio_data *bd = opaque;

	d(printf("iocontext.readPacket()\n"));

	if (bd == NULL)
		return -1;

	JNIEnv *env = bd->env;
	jobject byteBuffer = WRAP(buf, buf_size);
	int res = (*bd->env)->CallIntMethod(env, bd->jo, byteio_readPacket, byteBuffer);

	d(printf("readpacket returned %d\n", res));

	return res;
}

static int AVIOContext_writePacket(void *opaque, uint8_t *buf, int buf_size) {
	struct avio_data *bd = opaque;

	d(printf("iocontext.writePacket()\n"));

	if (bd == NULL)
		return -1;

	JNIEnv *env = bd->env;
	jobject byteBuffer = WRAP(buf, buf_size);
	int res = (*bd->env)->CallIntMethod(env, bd->jo, byteio_writePacket, byteBuffer);

	return res;
}

static int64_t AVIOContext_seek(void *opaque, int64_t offset, int whence) {
	struct avio_data *bd = opaque;

	d(printf("iocontext.seek()\n"));

	if (bd == NULL)
		return -1;

	JNIEnv *env = bd->env;
	int64_t res = (*bd->env)->CallLongMethod(env, bd->jo, byteio_seek, (jlong)offset, (jint)whence);

	return res;
}

#define ALLOC_WRITE 1
#define ALLOC_STREAMED 2

JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_AVIOContextNative_allocContext
(JNIEnv *env, jclass jc, jclass oc, jint size, jint flags) {
	jmethodID init_p = (*env)->GetMethodID(env, oc, "<init>", NEWSIG);

	if (!init_p) {
		fprintf(stderr, "No suitable constructor for native object");
		// throw error
		return NULL;
	}

	unsigned char *buf = CALLDL(av_malloc)(size);

	if (buf == NULL)
		return NULL;

	// This has a weird-arse interface: you must pass it an allocated buffer
	// but then it feels free to reallocate it whenever it wants.
	// So we allocate here based on the requested size.

	d(printf("iocontext.allocContext(%p, %d)\n", buf, size));

	int write_flag = (flags & ALLOC_WRITE) != 0;

	AVIOContext *res = CALLDL(avio_alloc_context)(buf, size, write_flag, NULL,
						      AVIOContext_readPacket,
						      AVIOContext_writePacket,
						      AVIOContext_seek);

	d(printf(" = %p\n", res));
	if (res != NULL) {
		// this is deprecated, although i don't know if i should still set it anyway
		//res->is_streamed = (flags & ALLOC_STREAMED) != 0;
		res->seekable = (flags & ALLOC_STREAMED) ? 0 : AVIO_SEEKABLE_NORMAL;

		return (*env)->NewObject(env, oc, init_p, NEWCAST(res));
	} else {
		return NULL;
	}
}

JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_AVIOContextNative_probeInput
(JNIEnv *env, jclass jc, jobject jpb, jstring jname, jint offset, jint max_probe_size) {
	AVIOContext *pb = PTR(jpb, AVIOContext);
	const char *name = STR(jname);
	AVInputFormat *fmt = NULL;
	jobject res = NULL;

	//resp[0]
	int ret = CALLDL(av_probe_input_buffer)(pb, &fmt, name, NULL, offset, max_probe_size);
	d(printf("probe input buffer = %d\n", ret));

	if (ret == 0) {
		res = NEWOBJ(fmt, AVInputFormat);
	}

	RSTR(jname, name);

	return res;
}

JNIEXPORT void JNICALL Java_au_notzed_jjmpeg_AVIOContextNative_bind
(JNIEnv *env, jclass jc, jobject jo, jobject jptr) {
	AVIOContext *cptr = PTR(jptr, AVIOContext);
	struct avio_data *bd;

	d(printf("iocontext.bind()\n"));

	bd = malloc(sizeof(*bd));
	if (bd == NULL) {
		// throw new ...
	}

	bd->env = env;
	bd->jo = (*env)->NewGlobalRef(env, jo);

	cptr->opaque = bd;
}

JNIEXPORT void JNICALL Java_au_notzed_jjmpeg_AVIOContextNative_unbind
(JNIEnv *env, jclass jc, jobject jo, jobject jptr) {
	AVIOContext *cptr = PTR(jptr, AVIOContext);
	struct avio_data *bd = cptr->opaque;

	d(printf("iocontext.unbind()\n"));

	(*env)->DeleteGlobalRef(env, bd->jo);

	free(bd);
	CALLDL(av_free)(cptr);
}


JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_AVIOContextNative_open
(JNIEnv *env, jclass jc, jstring jurl, int flags, jobject jerror_buf) {
	const char *url = STR(jurl);
	AVIOContext *context = NULL;
	int *resp = ADDR(jerror_buf);
	jobject res = NULL;

	resp[0] = CALLDL(avio_open)(&context, url, flags);

	if (resp[0] == 0) {
		res = NEWOBJ(context, AVIOContext);
	}

	RSTR(jurl, url);

	return res;
}

