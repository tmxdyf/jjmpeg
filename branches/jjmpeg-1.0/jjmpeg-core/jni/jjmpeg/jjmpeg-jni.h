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
 * Global includes
 */

#include <jni.h>

#include "jjmpeg-platform.h"

#define ADDR(jp) (jp != NULL ? (*env)->GetDirectBufferAddress(env, jp) : NULL)
#define SIZE(jp) (jp != NULL ? (*env)->GetDirectBufferCapacity(env, jp) : 0)
#define STR(jp) (jp != NULL ? (*env)->GetStringUTFChars(env, jp, NULL) : NULL)
#define RSTR(jp, cp) ((jp != NULL) ? ((*env)->ReleaseStringUTFChars(env, jp, cp)):0 )

#define WRAP(cp, clen) ((*env)->NewDirectByteBuffer(env, cp, clen))
#define WRAPSTR(js) ((*env)->NewStringUTF(env, js))

#if PLATFORM_BITS == 32
#define PTR(jo, type) (jo ? (void *)(*env)->GetIntField(env, jo, type ## _p) : NULL)
#define SET_PTR(jo, type, co) do { if (jo) (*env)->SetIntField(env, jo, type ## _p, (int)(co)); } while (0);
#define NEWOBJ(cp, type) (cp ? (*env)->NewObject(env, type ## _class, type ## _init_p, (int)cp) : NULL)
#define NEWSIG "(I)V"
#define NEWBITS "32"
#define NEWCAST(x) ((jint)(x))
#else
#define PTR(jo, type) (jo ? (void *)(*env)->GetLongField(env, jo, type ## _p) : NULL)
#define SET_PTR(jo, type, co) do { if (jo) (*env)->SetLongField(env, jo, type ## _p, (jlong)(co)); } while (0);
#define NEWOBJ(cp, type) (cp ? (*env)->NewObject(env, type ## _class, type ## _init_p, (jlong)cp) : NULL)
#define NEWSIG "(J)V"
#define NEWBITS "64"
#define NEWCAST(x) ((jlong)(x))
#endif

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/avutil.h>

static int init_local(JNIEnv *env);

// swscale leaves SwsContext opaque.  We just need a dummy type for the binding library.
// (actually this is left over from the bytebuffer stuff, i don't think it's needed now)
#include <libswscale/swscale.h>
struct SwsContext {
	int dummy;
};
typedef struct SwsContext SwsContext;

// same for ReSampleContext
struct ReSampleContext {
	int dummy;
};

#include <libswresample/swresample.h>
struct SwrContext {
	int dummy;
};

// and AVDictionary
struct AVDictionary {
	void *dummy;
};

/* Holder fields */
static jfieldID ObjectHolder_value;
static jfieldID LongHolder_value;
static jfieldID IntHolder_value;

#ifdef ENABLE_DL
static void *avutil_lib;
static void *avcodec_lib;
static void *avformat_lib;
static void *swscale_lib;
static void *swresample_lib;
#endif
