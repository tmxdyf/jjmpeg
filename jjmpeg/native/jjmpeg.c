
/*
 * Contains hand-rolled interfaces
 */

#include "jjmpeg-jni.c"

#define USEDL

#ifdef USEDL
//static int (*dav_open_input_stream)(AVFormatContext **ic_ptr,
//				    ByteIOContext *pb, const char *filename,
//				    AVInputFormat *fmt, AVFormatParameters *ap);
static int (*dav_open_input_file)(AVFormatContext **ic_ptr, const char *filename,
				  AVInputFormat *fmt,
				  int buf_size,
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

#define CALLDL(x) (*d ## x)
#define MAPDL(x, lib) if ((d ## x = dlsym(lib, #x)) == NULL) return 0
#else
#define CALLDL(x) x
#define MAPDL(x, lib)
#endif

static jmethodID byteio_readPacket;
static jmethodID byteio_writePacket;
static jmethodID byteio_seek;

/* ********************************************************************** */

static int init_local(JNIEnv *env) {

#ifdef USEDL
	//*(void **)(&dav_open_input_file) = dlsym(avformat_lib, "av_open_input_file");
	dav_open_input_file = dlsym(avformat_lib, "av_open_input_file");
	if (dav_open_input_file == NULL) return -1;

	dav_alloc_put_byte = dlsym(avformat_lib, "av_alloc_put_byte");
	if (dav_alloc_put_byte == NULL) return 0;

	//*(void **)(&davcodec_encode_video) = dlsym(avcodec_lib, "avcodec_encode_video");
	davcodec_encode_video = dlsym(avcodec_lib, "avcodec_encode_video");
	if (davcodec_encode_video == NULL) return 0;

	dsws_scale = dlsym(swscale_lib, "sws_scale");
	if (dsws_scale == NULL) return 0;

	MAPDL(av_rescale_q, avutil_lib);

	MAPDL(av_malloc, avutil_lib);
	MAPDL(av_free, avutil_lib);
#else
#endif

	jclass byteioclass = (*env)->FindClass(env, "au/notzed/jjmpeg/ByteIOContext");
	if (byteioclass == NULL)
		;
	byteio_readPacket = (*env)->GetMethodID(env, byteioclass, "readPacket", "(Ljava/nio/ByteBuffer;)I");
	byteio_writePacket = (*env)->GetMethodID(env, byteioclass, "writePacket", "(Ljava/nio/ByteBuffer;)I");
	byteio_seek = (*env)->GetMethodID(env, byteioclass, "seek", "(JI)J");

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
		av_init_packet(packet);

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
	if (bd == NULL)
		return -1;

	JNIEnv *env = bd->env;
	jobject byteBuffer = WRAP(buf, buf_size);
	int res = (*bd->env)->CallIntMethod(env, bd->jo, byteio_readPacket, byteBuffer);

	return res;
}

static int ByteIOContext_writePacket(void *opaque, uint8_t *buf, int buf_size) {
	struct byteio_data *bd = opaque;

	if (bd == NULL)
		return -1;

	JNIEnv *env = bd->env;
	jobject byteBuffer = WRAP(buf, buf_size);
	int res = (*bd->env)->CallIntMethod(env, bd->jo, byteio_writePacket, byteBuffer);

	return res;
}

static int64_t ByteIOContext_seek(void *opaque, int64_t offset, int whence) {
	struct byteio_data *bd = opaque;

	if (bd == NULL)
		return -1;

	JNIEnv *env = bd->env;
	int64_t res = (*bd->env)->CallLongMethod(env, bd->jo, byteio_seek, (jlong)offset, (jint)whence);

	return res;
}

JNIEXPORT jobject JNICALL Java_au_notzed_jjmpeg_ByteIOContext_create_1put_1byte (JNIEnv *env, jclass jc, jobject buffer, jint write_flag) {
	unsigned char *buf = ADDR(buffer);
	int buf_size = SIZE(buffer);	
	ByteIOContext *res = CALLDL(av_alloc_put_byte)(buf, buf_size, write_flag, NULL,
						       ByteIOContext_readPacket,
						       ByteIOContext_writePacket,
						       ByteIOContext_seek);

	return WRAP(res, sizeof(*res));
}

JNIEXPORT void JNICALL Java_au_notzed_jjmpeg_ByteIOContext_bind (JNIEnv *env, jobject jo) {
	jobject jptr = (*env)->GetObjectField(env, jo, field_p);
	ByteIOContext *cptr = ADDR(jptr);
	struct byteio_data *bd;

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

	(*env)->DeleteGlobalRef(env, bd->jo);

	free(bd);
	CALLDL(av_free)(cptr);
}

