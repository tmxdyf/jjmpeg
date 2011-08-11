
/*
 * Contains hand-rolled interfaces
 */

#include "jjmpeg-jni.c"

#define d(x) x; fflush(stdout)

static int (*dav_open_input_file)(AVFormatContext **ic_ptr, const char *filename,
				  AVInputFormat *fmt,
				  int buf_size,
				  AVFormatParameters *ap);

static int (*dav_open_input_stream)(AVFormatContext **ic_ptr,
				    ByteIOContext *pb, const char *filename,
				    AVInputFormat *fmt,
				    AVFormatParameters *ap);

static int (*davcodec_encode_video)(AVCodecContext *avctx, uint8_t *buf, int buf_size, AVFrame *pict);

static ByteIOContext *(*dav_alloc_put_byte)(
                  unsigned char *buffer,
                  int buffer_size,
                  int write_flag,
                  void *opaque,
                  int (*read_packet)(void *opaque, uint8_t *buf, int buf_size),
                  int (*write_packet)(void *opaque, uint8_t *buf, int buf_size),
                  int64_t (*seek)(void *opaque, int64_t offset, int whence));

static int (*dsws_scale)(struct SwsContext *context, const uint8_t* const srcSlice[], const int srcStride[],
			 int srcSliceY, int srcSliceH, uint8_t* const dst[], const int dstStride[]);

static int64_t (*dav_rescale_q)(int64_t a, AVRational bq, AVRational cq);
static void * (*dav_malloc)(int size);
static void (*dav_free)(void *mem);
static void (*dav_init_packet)(AVPacket *);

static jmethodID byteio_readPacket;
static jmethodID byteio_writePacket;
static jmethodID byteio_seek;

/* ********************************************************************** */

static int init_local(JNIEnv *env) {

	DLOPEN(avcodec_lib, "avcodec", LIBAVCODEC_VERSION_MAJOR);
	DLOPEN(avutil_lib, "avutil", LIBAVUTIL_VERSION_MAJOR);
	DLOPEN(avformat_lib, "avformat", LIBAVFORMAT_VERSION_MAJOR);
	DLOPEN(swscale_lib, "swscale", LIBSWSCALE_VERSION_MAJOR);

	MAPDL(av_open_input_file, avformat_lib);
	MAPDL(av_open_input_stream, avformat_lib);
	MAPDL(av_alloc_put_byte, avformat_lib);
	MAPDL(avcodec_encode_video, avcodec_lib);
	MAPDL(av_init_packet, avcodec_lib);
	MAPDL(sws_scale, swscale_lib);

	MAPDL(av_rescale_q, avutil_lib);
	MAPDL(av_malloc, avutil_lib);
	MAPDL(av_free, avutil_lib);

	jclass byteioclass = (*env)->FindClass(env, "au/notzed/jjmpeg/ByteIOContext");
	if (byteioclass == NULL)
		;
	byteio_readPacket = (*env)->GetMethodID(env, byteioclass, "readPacket", "(Ljava/nio/ByteBuffer;)I");
	byteio_writePacket = (*env)->GetMethodID(env, byteioclass, "writePacket", "(Ljava/nio/ByteBuffer;)I");
	byteio_seek = (*env)->GetMethodID(env, byteioclass, "seek", "(JI)J");

	return 1;
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

JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_AVNative__1malloc
(JNIEnv *env, jclass jc, jint size) {
	jobject res = WRAP(CALLDL(av_malloc)(size), size);
	return res;
}

JNIEXPORT void JNICALL Java_au_notzed_jjmpeg_AVNative__1free
(JNIEnv *env, jclass jc, jobject jmem) {
        void * mem = ADDR(jmem);

        (*dav_free)(mem);
}
/* ********************************************************************** */

// TODO: depdrecated in newer libavformat
JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_AVFormatContext_open_1input_1file
(JNIEnv *env, jclass jc, jstring jname, jobject jfmt, jint buf_size, jobject jap, jobject jerror_buf) {
	const char *name = STR(jname);
	AVFormatContext *context;
	int *resp = ADDR(jerror_buf);
	AVInputFormat *fmt = ADDR(jfmt);
	AVFormatParameters *ap = ADDR(jap);
	jobject res = NULL;

	resp[0] = CALLDL(av_open_input_file)(&context, name, fmt, buf_size, ap);

	if (resp[0] == 0) {
		res = WRAP(context, sizeof(*context));
	}

	RSTR(jname, name);

	return res;
}

// TODO: depdrecated in newer libavformat
JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_AVFormatContext_open_1input_1stream
(JNIEnv *env, jclass jc, jobject jpb, jstring jname, jobject jfmt, jobject jap, jobject jerror_buf) {
	ByteIOContext *pb = ADDR(jpb);
	const char *name = STR(jname);
	AVFormatContext *context;
	int *resp = ADDR(jerror_buf);
	AVInputFormat *fmt = ADDR(jfmt);
	AVFormatParameters *ap = ADDR(jap);
	jobject res = NULL;

	d(printf("open input stream  pb=%p name=%s fmt=%p ap=%p\n", pb, name, fmt, ap));
	resp[0] = CALLDL(av_open_input_stream)(&context, pb, name, fmt, ap);
	d(printf("open input stream = %d\n", resp[0]));

	if (resp[0] == 0) {
		res = WRAP(context, sizeof(*context));
	}

	RSTR(jname, name);

	return res;
}

/* ********************************************************************** */

/* ********************************************************************** */

JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_AVFrame_getPlaneAt(JNIEnv *env, jobject jo, jint index, jint fmt, jint width, jint height) {
	jobject jptr = (*env)->GetObjectField(env, jo, field_p);
	AVFrame *cptr = ADDR(jptr);

	// FIXME: this depends on pixel format
	if (index > 0)
		height /= 2;

	int size = cptr->linesize[index] * height;

	return WRAP(cptr->data[index], size);
}

/* ********************************************************************** */

// TODO: this isn't needed, decode-video just uses the packet directly
//JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_AVPacket_get_1data
//(JNIEnv *env, jobject jo, jobject jpacket) {
//	jobject jptr = (*env)->GetObjectField(env, jo, field_p);
//	AVPacket *packet = ADDR(jptr);

//	return WRAP(packet->data, packet->size);
//}

JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_AVPacket_allocate
(JNIEnv *env, jclass hc) {
	AVPacket *packet = malloc(sizeof(AVPacket));

	if (packet != NULL)
		CALLDL(av_init_packet)(packet);

	return WRAP(packet, sizeof(AVPacket));
}

JNIEXPORT void JNICALL Java_au_notzed_jjmpeg_AVPacket_free
(JNIEnv *env, jclass jc, jobject jpacket) {
	AVPacket *packet = ADDR(jpacket);

	free(packet);
}

JNIEXPORT jint JNICALL Java_au_notzed_jjmpeg_AVAudioPacket_consume
(JNIEnv *env, jobject jo, jint len) {
	jobject jptr = (*env)->GetObjectField(env, jo, field_p);
	AVPacket *packet = ADDR(jptr);

	//printf("consume, packet = %p  data = %p, size = %d\n", packet, packet->data, packet->size); fflush(stdout);

	len = packet->size < len ? packet->size : len;

	packet->data += len;
	packet->size -= len;

	return packet->size;
}

/* ********************************************************************** */

JNIEXPORT jint JNICALL Java_au_notzed_jjmpeg_SwsContext__1scale (JNIEnv *env, jobject jo, jobject jsrc, jint srcSliceY, jint srcSliceH, jobject jdst) {
	jobject jptr = (*env)->GetObjectField(env, jo, field_p);
	struct SwsContext *sws = ADDR(jptr);
	struct AVFrame *src = ADDR(jsrc);
	struct AVFrame *dst = ADDR(jdst);
	jint res;

	res = CALLDL(sws_scale)(sws, (const uint8_t * const *)src->data, src->linesize,
				srcSliceY, srcSliceH,
				dst->data, dst->linesize);

	return res;
}

/* ********************************************************************** */

// our rescale_q takes pointer arguments
JNIEXPORT jlong JNICALL Java_au_notzed_jjmpeg_AVRational_jj_1rescale_1q (JNIEnv *env, jclass jc, jlong ja, jobject jbq, jobject jcq) {
	AVRational *bqp = ADDR(jbq);
	AVRational *cqp = ADDR(jcq);
	jlong res;

	res = CALLDL(av_rescale_q)(ja, *bqp, *cqp);

	return res;
}

/* ********************************************************************** */

//JavaVM *vm = NULL;

// This assumes we only get called from threads which invoked the ffmpeg libraries.
// If not, things are a (tad) more complex.
struct byteio_data {
	JNIEnv *env;
	jobject jo;
};

/* Wrappers for ByteIOContext to allow callbacks to java */

static int ByteIOContext_readPacket(void *opaque, uint8_t *buf, int buf_size) {
	struct byteio_data *bd = opaque;

	d(printf("iocontext.readPacket()\n"));

	if (bd == NULL)
		return -1;

	JNIEnv *env = bd->env;
	jobject byteBuffer = WRAP(buf, buf_size);
	int res = (*bd->env)->CallIntMethod(env, bd->jo, byteio_readPacket, byteBuffer);

	return res;
}

static int ByteIOContext_writePacket(void *opaque, uint8_t *buf, int buf_size) {
	struct byteio_data *bd = opaque;

	d(printf("iocontext.writePacket()\n"));

	if (bd == NULL)
		return -1;

	JNIEnv *env = bd->env;
	jobject byteBuffer = WRAP(buf, buf_size);
	int res = (*bd->env)->CallIntMethod(env, bd->jo, byteio_writePacket, byteBuffer);

	return res;
}

static int64_t ByteIOContext_seek(void *opaque, int64_t offset, int whence) {
	struct byteio_data *bd = opaque;

	d(printf("iocontext.seek()\n"));

	if (bd == NULL)
		return -1;

	JNIEnv *env = bd->env;
	int64_t res = (*bd->env)->CallLongMethod(env, bd->jo, byteio_seek, (jlong)offset, (jint)whence);

	return res;
}

JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_ByteIOContext_create_1put_1byte (JNIEnv *env, jclass jc, jobject buffer, jint write_flag) {
	unsigned char *buf = ADDR(buffer);
	int buf_size = SIZE(buffer);	

	d(printf("iocontext.createPutByte(%p, %d)\n", buf, buf_size));

	ByteIOContext *res = CALLDL(av_alloc_put_byte)(buf, buf_size, write_flag, NULL,
						       ByteIOContext_readPacket,
						       ByteIOContext_writePacket,
						       ByteIOContext_seek);

	d(printf(" = %p\n", res));

	return WRAP(res, sizeof(*res));
}

JNIEXPORT void JNICALL Java_au_notzed_jjmpeg_ByteIOContext_bind (JNIEnv *env, jobject jo) {
	jobject jptr = (*env)->GetObjectField(env, jo, field_p);
	ByteIOContext *cptr = ADDR(jptr);
	struct byteio_data *bd;

	d(printf("iocontext.bind()\n"));

	bd = malloc(sizeof(*bd));
	if (bd == NULL) {
		// throw new ...
	}

	bd->env = env;
	bd->jo = (*env)->NewGlobalRef(env, jo);

	cptr->opaque = bd;
}

JNIEXPORT void JNICALL Java_au_notzed_jjmpeg_ByteIOContext_unbind (JNIEnv *env, jobject jo) {
	jobject jptr = (*env)->GetObjectField(env, jo, field_p);
	ByteIOContext *cptr = ADDR(jptr);
	struct byteio_data *bd = cptr->opaque;

	d(printf("iocontext.unbind()\n"));

	(*env)->DeleteGlobalRef(env, bd->jo);

	free(bd);
	CALLDL(av_free)(cptr);
}

