/*
 * Global includes
 */

#include <jni.h>

#ifdef WIN64
#  include <windows.h>

#define _TOSTR(x) #x
#define TOSTR(x) _TOSTR(x)

#  define DLOPEN(x, lib, ver) do { x = LoadLibrary(lib "-"  _TOSTR(ver) ".dll"); if (x == NULL) { fprintf(stderr, "cannot open %s\n",  lib "-" TOSTR(ver) ".dll"); fflush(stderr); return 0; } } while(0)
#  define CALLDL(x) (*d ## x)
#  define MAPDL(x, lib) do { if ((d ## x = (void *)GetProcAddress(lib, #x)) == NULL) { fprintf(stderr, "cannot resolve %s\n", #x); fflush(stderr); return 0; } } while(0)
#else
#  include <dlfcn.h>

#  define DLOPEN(x, lib, ver) x = dlopen("lib" lib ".so", RTLD_LAZY|RTLD_GLOBAL); do { if (x == NULL) { fprintf(stderr, "cannot open %s\n", lib ".so"); fflush(stderr); return 0; } } while (0)
#  define CALLDL(x) (*d ## x)
#  define MAPDL(x, lib) do { if ((d ## x = dlsym(lib, #x)) == NULL) { fprintf(stderr, "cannot resolve %s\n", #x); fflush(stderr); return 0; } } while (0)
#endif

#define ADDR(jp) (jp != NULL ? (*env)->GetDirectBufferAddress(env, jp) : NULL)
#define SIZE(jp) (jp != NULL ? (*env)->GetDirectBufferCapacity(env, jp) : 0)
#define STR(jp) (jp != NULL ? (*env)->GetStringUTFChars(env, jp, NULL) : NULL)
#define RSTR(jp, cp) ((jp != NULL) ? ((*env)->ReleaseStringUTFChars(env, jp, cp)):0 )

#define WRAP(cp, clen) ((*env)->NewDirectByteBuffer(env, cp, clen));
#define WRAPSTR(js) ((*env)->NewStringUTF(env, js));

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/avutil.h>

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


/**  Library handles */
static void *avutil_lib;
static void *avcodec_lib;
static void *avformat_lib;
static void *swscale_lib;
