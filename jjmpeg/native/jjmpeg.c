
#include "au_notzed_jjmpeg_AVCodecContext.h"
#include "au_notzed_jjmpeg_AVCodec.h"
#include "au_notzed_jjmpeg_AVFormatContext.h"
#include "au_notzed_jjmpeg_AVFrame.h"
#include "au_notzed_jjmpeg_AVPacket.h"
#include "au_notzed_jjmpeg_AVStream.h"
#include "au_notzed_jjmpeg_AVNative.h"

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>

#define ADDR(jp) (jp != NULL ? (*env)->GetDirectBufferAddress(env, jp) : NULL)
#define STR(jp) ((*env)->GetStringUTFChars(env, jp, NULL))
#define RSTR(jp, cp) ((*env)->ReleaseStringUTFChars(env, jp, cp))

#define WRAP(cp, clen) ((*env)->NewDirectByteBuffer(env, cp, clen));

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

/**
 * Get size of pointer in bits.  i.e. 64 or 32.
 */
JNIEXPORT jint JNICALL Java_au_notzed_jjmpeg_AVNative_getPointerBits
(JNIEnv *env, jclass jc) {
	return sizeof(void *)*8;
}

/* ********************************************************************** */

jobject JNICALL Java_au_notzed_jjmpeg_AVCodec_find_1encoder
(JNIEnv *env, jclass jc, jint codecid) {
	AVCodec *codec;
	jobject res = NULL;

	codec = avcodec_find_encoder(codecid);
	if (codec != NULL) {
		res = WRAP(codec, sizeof(AVCodec));
	}

	return res;
}

jobject JNICALL Java_au_notzed_jjmpeg_AVCodec_find_1decoder
(JNIEnv *env, jclass jc, jint codecid) {
	AVCodec *codec;
	jobject res = NULL;

	codec = avcodec_find_decoder(codecid);
	if (codec != NULL) {
		res = WRAP(codec, sizeof(AVCodec));
	}

	return res;
}

/* ********************************************************************** */

jint JNICALL Java_au_notzed_jjmpeg_AVCodecContext_open
(JNIEnv *env, jobject o, jobject jcontext, jobject jcodec) {
	AVCodecContext *context = ADDR(jcontext);
	AVCodec *codec = ADDR(jcodec);
	int res = avcodec_open(context, codec);

	//printf("avcodec open %s = %d\n", codec->name, res);
	//fflush(stdout);

	return res;
}

jint JNICALL Java_au_notzed_jjmpeg_AVCodecContext_decode_1video
(JNIEnv *env, jobject o, jobject jcontext, jobject jframe, jobject jfinished, jobject jdata) {
	AVCodecContext *context = ADDR(jcontext);
	AVFrame *frame = ADDR(jframe);
	int *finished = ADDR(jfinished);
	AVPacket *data = ADDR(jdata);
	int res;

	res = avcodec_decode_video2(context, frame, finished, data);

	//if (finished[0])
	//	printf("video frame finished  ");
	//printf("video deced, frame size = %d\n", context->frame_size); fflush(stdout);
	//printf("planes = %p %p %p %p\n", frame->data[0], frame->data[1], frame->data[2], frame->data[3]);

	return res;
}

void JNICALL Java_au_notzed_jjmpeg_AVCodecContext_register_1all
(JNIEnv *env, jclass jc) {
	av_register_all();
}

/* ********************************************************************** */

JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_AVFormatContext_open_1input_1file
(JNIEnv *env, jclass jc, jstring jname, jobject jfmt, jint buf_size, jobject jap, jobject jerror_buf) {
	const char *name = STR(jname);
	AVFormatContext *context;
	int *resp = ADDR(jerror_buf);
	AVInputFormat *fmt = ADDR(jfmt);
	AVFormatParameters *ap = ADDR(jap);
	jobject res = NULL;

	resp[0] = av_open_input_file(&context, name, fmt, buf_size, ap);

	if (resp[0] == 0) {
		res = WRAP(context, sizeof(*context));
	}

	RSTR(jname, name);

	return res;
}

JNIEXPORT jint JNICALL Java_au_notzed_jjmpeg_AVFormatContext_seek_1frame
(JNIEnv *env, jobject o, jobject jcontext, jint stream_index, jlong timestamp, jint flags) {
	AVFormatContext *context = ADDR(jcontext);

	return av_seek_frame(context, stream_index, timestamp, flags);
}

jint JNICALL Java_au_notzed_jjmpeg_AVFormatContext_find_1stream_1info
(JNIEnv *env, jobject o, jobject jcontext) {
	AVFormatContext *context = ADDR(jcontext);
	int res;

	res = av_find_stream_info(context);

	return res;
}

int JNICALL Java_au_notzed_jjmpeg_AVFormatContext_read_1frame
(JNIEnv *env, jobject o, jobject jcontext, jobject jpacket) {
	AVFormatContext *context = ADDR(jcontext);
	AVPacket *packet = ADDR(jpacket);
	int res;

	res = av_read_frame(context, packet);

	return res;
}

/* ********************************************************************** */

JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_AVFrame_alloc_1frame
(JNIEnv *env, jclass jc) {
	AVFrame *frame = avcodec_alloc_frame();

	return WRAP(frame, sizeof(AVFrame));
}

JNIEXPORT void JNICALL Java_au_notzed_jjmpeg_AVFrame_free_1frame
(JNIEnv *env, jclass jc, jobject jframe) {
	AVFrame *frame = ADDR(jframe);

	av_free(frame);
}

/* ********************************************************************** */

JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_AVPacket_get_1data
(JNIEnv *env, jobject jo, jobject jpacket) {
	AVPacket *packet = ADDR(jpacket);

	return WRAP(packet->data, packet->size);
}

JNIEXPORT void JNICALL Java_au_notzed_jjmpeg_AVPacket_free_1packet
(JNIEnv *env, jobject jo, jobject jpacket) {
	AVPacket *packet = ADDR(jpacket);

	av_free_packet(packet);
}

JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_AVPacket_allocate
(JNIEnv *env, jclass hc) {
	AVPacket *packet = malloc(sizeof(AVPacket));

	return WRAP(packet, sizeof(AVPacket));
}

JNIEXPORT void JNICALL Java_au_notzed_jjmpeg_AVPacket_free
(JNIEnv *env, jclass jc, jobject jpacket) {
	AVPacket *packet = ADDR(jpacket);

	free(packet);
}
