
// DVB interface for java

#include "jjdvb-jni.c"

//#define STR(jp) ((*env)->GetStringUTFChars(env, jp, NULL))
//#define RSTR(jp, cp) ((*env)->ReleaseStringUTFChars(env, jp, cp))

static int init_local(JNIEnv *env) {
	return 0;
}

JNIEXPORT void JNICALL Java_au_notzed_jjdvb_DVBNative__1free
(JNIEnv *env, jclass jc, jobject jmem) {
        void * mem = ADDR(jmem);

        free(mem);
}

/** Possibly I should just pass around the fd rather than a pointer to it */

JNIEXPORT jobject JNICALL Java_au_notzed_jjdvb_DMX_dmx_1open (JNIEnv *env, jclass jc, jstring jname) {
	const char*name = STR(jname);
	int fd = open(name, O_RDWR);

	perror("opening dmx");

	RSTR(jname, name);


	if (fd != -1) {
		DMX *dmx = malloc(sizeof(*dmx));

		dmx->demuxfd = fd;

		return WRAP(dmx, sizeof(*dmx));
	}
	return NULL;
}

JNIEXPORT void JNICALL Java_au_notzed_jjdvb_DMX_dmx_1close (JNIEnv *env, jobject jo) {
	jobject jptr = (*env)->GetObjectField(env, jo, field_p);
	DMX *cptr = ADDR(jptr);

	close(cptr->demuxfd);
}

JNIEXPORT jint JNICALL Java_au_notzed_jjdvb_DMX_dmx_1ioctl__I (JNIEnv *env, jobject jo, jint id) {
	jobject jptr = (*env)->GetObjectField(env, jo, field_p);
	DMX *cptr = ADDR(jptr);

	switch (id) {
	case 0:
		return ioctl(cptr->demuxfd, DMX_START);
	case 1:
		return ioctl(cptr->demuxfd, DMX_STOP);
	}

	return -1;
}

JNIEXPORT jint JNICALL Java_au_notzed_jjdvb_DMX_dmx_1ioctl__ILjava_nio_ByteBuffer_2 (JNIEnv *env, jobject jo, jint id, jobject jbb) {
	jobject jptr = (*env)->GetObjectField(env, jo, field_p);
	DMX *cptr = ADDR(jptr);
	void *dptr = ADDR(jbb);

	switch (id) {
	case 2:
		return ioctl(cptr->demuxfd, DMX_SET_FILTER, dptr);
	case 3:
		return ioctl(cptr->demuxfd, DMX_SET_PES_FILTER, dptr);
	}

	return -1;
}

JNIEXPORT jobject JNICALL Java_au_notzed_jjdvb_FE_fe_1open (JNIEnv *env, jclass jc, jstring jname) {
	const char *name = STR(jname);
	int fd = open(name, O_RDWR);

	perror("opening fe");

	RSTR(jname, name);

	if (fd != -1) {
		DMX *dmx = malloc(sizeof(*dmx));

		dmx->demuxfd = fd;

		return WRAP(dmx, sizeof(*dmx));
	}
	return NULL;
}

JNIEXPORT void JNICALL Java_au_notzed_jjdvb_FE_fe_1close (JNIEnv *env, jobject jo) {
	jobject jptr = (*env)->GetObjectField(env, jo, field_p);
	FE *cptr = ADDR(jptr);

	close(cptr->frontendfd);
}

JNIEXPORT jint JNICALL Java_au_notzed_jjdvb_FE_fe_1ioctl__I (JNIEnv *env, jobject jo, jint id) {
	//jobject jptr = (*env)->GetObjectField(env, jo, field_p);
	//FE *cptr = ADDR(jptr);

	return -1;
}

JNIEXPORT jint JNICALL Java_au_notzed_jjdvb_FE_fe_1ioctl__ILjava_nio_ByteBuffer_2 (JNIEnv *env, jobject jo, jint id, jobject jbb) {
	jobject jptr = (*env)->GetObjectField(env, jo, field_p);
	FE *cptr = ADDR(jptr);
	void *dptr = ADDR(jbb);

	switch (id) {
	case 69:
		return ioctl(cptr->frontendfd, FE_READ_STATUS, dptr);
	case 76:
		return ioctl(cptr->frontendfd, FE_SET_FRONTEND, dptr);
	}

	return -1;
}

JNIEXPORT jobject JNICALL Java_au_notzed_jjdvb_DVBFrontendParameters_alloc (JNIEnv *env, jclass jc) {
	struct dvb_frontend_parameters *params = malloc(sizeof(*params));

	if (params != NULL) {
		memset(params, 0, sizeof(*params));
		return WRAP(params, sizeof(*params));
	}
	return NULL;
}

JNIEXPORT jobject JNICALL Java_au_notzed_jjdvb_DMXPESFilterParams_alloc (JNIEnv *env, jclass jc) {
	struct dmx_pes_filter_params *params = malloc(sizeof(*params));

	if (params != NULL) {
		memset(params, 0, sizeof(*params));
		return WRAP(params, sizeof(*params));
	}
	return NULL;
}
