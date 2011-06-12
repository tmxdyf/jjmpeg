/*
 * Global includes
 */

#include <dlfcn.h>
#include <jni.h>

#define ADDR(jp) (jp != NULL ? (*env)->GetDirectBufferAddress(env, jp) : NULL)
#define SIZE(jp) (jp != NULL ? (*env)->GetDirectBufferCapacity(env, jp) : 0)
#define STR(jp) ((*env)->GetStringUTFChars(env, jp, NULL))
#define RSTR(jp, cp) ((*env)->ReleaseStringUTFChars(env, jp, cp))

#define WRAP(cp, clen) ((*env)->NewDirectByteBuffer(env, cp, clen));
#define WRAPSTR(js) ((*env)->NewStringUTF(env, js));

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>

static int init_local(JNIEnv *env);

// swscale leavves SwsContext opaque.  We just need a dummy type for the binding library.
#include <libswscale/swscale.h>
struct SwsContext {
	int dummy;
};
typedef struct SwsContext SwsContext;

// same for ReSampleContext
struct ReSampleContext {
	int dummy;
};
//typedef struct ReSampleContext ReSampleContext;
