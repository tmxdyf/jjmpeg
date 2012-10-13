
include $(TOP)/../config.mk

LIB=libjjmpeg$(LIBVERSION).so
TARGET=gnu-i386
JNI=linux

JJMPEG_CFLAGS=$(FFMPEG_HOST_CFLAGS) -fPIC -m32
JJMPEG_LIBS= -lpthread
JJMPEG_LDFLAGS = -m32 -Wl,-soname,$(LIB)

all: $(LIB)

include $(TOP)/jjmpeg.mk
