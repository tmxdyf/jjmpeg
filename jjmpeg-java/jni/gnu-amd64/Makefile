
include $(TOP)/config.mk

LIB=libjjmpeg$(LIBVERSION).so
TARGET=gnu-amd64
JNI=linux

JJMPEG_CFLAGS=-I/opt/ffmpeg-0.10/include -fPIC -m64
JJMPEG_LIBS= -lpthread
JJMPEG_LDFLAGS = -Wl,-soname,$(LIB)

all: $(LIB)

include $(TOP)/jjmpeg.mk
