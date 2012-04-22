LOCAL_PATH := $(call my-dir)

FF=armeabi

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

#include $(CLEAR_VARS)
#LOCAL_MODULE := swresample
#LOCAL_SRC_FILES := $(FF)/lib/libswresample.a
#LOCAL_EXPORT_C_INCLUDES := $(FF)/include
#include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := swscale
LOCAL_SRC_FILES := $(FF)/lib/libswscale.a
LOCAL_EXPORT_C_INCLUDES := $(FF)/include
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := jjmpeg
LOCAL_SRC_FILES := jjmpeg/jjmpeg.c
LOCAL_STATIC_LIBRARIES := avformat avcodec swscale avutil
LOCAL_CFLAGS := -I$(FF) -Ijjmpeg
LOCAL_LDLIBS := -lz
include $(BUILD_SHARED_LIBRARY)
