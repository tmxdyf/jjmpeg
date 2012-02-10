
SRC=..
TARGET=darwin-i386

include $(SRC)/config.make

FFMPEG_HOME=/usr/include/ffmpeg

CROSS=i686-apple-darwin9-
TARGET_JNI_CFLAGS = -I$(SRC)/jni/linux
TARGET_CFLAGS = -fPIC -m32
TARGET_JJMPEG_CFLAGS= -I$(FFMPEG_HOME)
TARGET_JJMPEG_LIBS=
TARGET_JJMPEG_LDFLAGS = -m32

include ../Makefile.libs

all: $(jjmpeg_lib)
