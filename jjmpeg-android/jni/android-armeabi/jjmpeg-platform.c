/*
 * Defines platform specific interfaces and platform-specific init.
 */

#include "lock-pthread.c"

static void log_cb(void *x, int level, const char *fmt, va_list ap) {
	__android_log_vprint(ANDROID_LOG_INFO, "ffmpeg", fmt, ap);
}

static int init_platform(JNIEnv *env) {
	// init log callbacks to redirect to android log
	LOGI("Setting log level");
	av_log_set_callback(log_cb);
	//av_log_set_level(99);

	av_lockmgr_register(lock_cb);

	return 0;
}
