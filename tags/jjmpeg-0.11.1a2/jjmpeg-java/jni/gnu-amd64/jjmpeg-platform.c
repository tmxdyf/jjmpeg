/*
 * Defines platform specific interfaces and platform-specific init.
 */

#include "lock-pthread.c"

static void log_cb(void *x, int level, const char *fmt, va_list ap) {
	vfprintf(stderr, fmt, ap);
}

static int init_platform(JNIEnv *env) {
	// init log callbacks to redirect to stderr
	CALLDL(av_log_set_callback)(log_cb);
	//av_log_set_level(99);
	CALLDL(av_lockmgr_register)(lock_cb);

	return 0;
}
