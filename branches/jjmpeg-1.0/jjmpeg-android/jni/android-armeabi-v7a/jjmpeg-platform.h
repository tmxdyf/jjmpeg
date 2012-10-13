/*
 * Included before anything else
 */
#include <android/log.h>
#define  LOG_TAG    "jjmpegjni"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

/* ********************************************************************** */
// We do no dlopen on android
#define MAPDL(name, library)
#define CALLDL(x) x

#define PLATFORM_BITS 32

/* ********************************************************************** */
#define HAVE_LOCK_CB 1

#define ENABLE_GLES2 1
