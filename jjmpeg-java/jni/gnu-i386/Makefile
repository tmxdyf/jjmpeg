
include $(TOP)/config.mk

LIB=libjjmpeg$(LIBVERSION).so
TARGET=gnu-i386
JNI=linux

JJMPEG_CFLAGS=-I/opt/ffmpeg-0.10/include -fPIC -m32
JJMPEG_LIBS= -lpthread
JJMPEG_LDFLAGS = -m32 -Wl,-soname,$(LIB)

all: $(LIB)

include $(TOP)/jjmpeg.mk
