LOCAL_PATH := $(call my-dir)

FF=build/$(TARGET_ARCH_ABI)

include $(CLEAR_VARS)
LOCAL_MODULE := avcodec
LOCAL_SRC_FILES := $(FF)/lib/libavcodec.a
LOCAL_EXPORT_C_INCLUDES := $(FF)/include
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := avformat
LOCAL_SRC_FILES := $(FF)/lib/libavformat.a
LOCAL_EXPORT_C_INCLUDES := $(FF)/include
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := avutil
LOCAL_SRC_FILES := $(FF)/lib/libavutil.a
LOCAL_EXPORT_C_INCLUDES := $(FF)/include
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := swresample
LOCAL_SRC_FILES := $(FF)/lib/libswresample.a
LOCAL_EXPORT_C_INCLUDES := $(FF)/include
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := swscale
LOCAL_SRC_FILES := $(FF)/lib/libswscale.a
LOCAL_EXPORT_C_INCLUDES := $(FF)/include
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
#APP_OPTIM=debug
CORE=../../jjmpeg-core/jni
LIBVERSION=011
LOCAL_MODULE := jjmpeg$(LIBVERSION)
LOCAL_SRC_FILES := ../../jjmpeg-core/jni/jjmpeg/jjmpeg.c
LOCAL_STATIC_LIBRARIES := avformat avcodec swscale avutil swresample
LOCAL_CFLAGS := -I$(FF) -I$(CORE)/jjmpeg -Iandroid-$(TARGET_ARCH_ABI) -I$(CORE)/platform
LOCAL_LDLIBS := -lz -llog -lGLESv2

jjmpeg$(LIBVERSION): $(FF)/jjmpeg-jni.c

include $(BUILD_SHARED_LIBRARY)
