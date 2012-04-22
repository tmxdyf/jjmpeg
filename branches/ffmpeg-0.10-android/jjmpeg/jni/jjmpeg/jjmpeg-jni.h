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

#define ADDR(jp) (jp != NULL ? (*env)->GetDirectBufferAddress(env, jp) : NULL)
#define SIZE(jp) (jp != NULL ? (*env)->GetDirectBufferCapacity(env, jp) : 0)
#define STR(jp) (jp != NULL ? (*env)->GetStringUTFChars(env, jp, NULL) : NULL)
#define RSTR(jp, cp) ((jp != NULL) ? ((*env)->ReleaseStringUTFChars(env, jp, cp)):0 )

#define WRAP(cp, clen) ((*env)->NewDirectByteBuffer(env, cp, clen))
#define WRAPSTR(js) ((*env)->NewStringUTF(env, js))

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/avutil.h>

static int init_local(JNIEnv *env);

// swscale leaves SwsContext opaque.  We just need a dummy type for the binding library.
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

// and AVDictionary
struct AVDictionary {
	void *dummy;
};

/* Holder fields */
static jfieldID ObjectHolder_value;
static jfieldID LongHolder_value;
static jfieldID IntHolder_value;


