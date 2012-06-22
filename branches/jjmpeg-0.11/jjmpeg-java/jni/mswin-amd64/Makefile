include $(TOP)/config.mk

LIB=jjmpeg$(LIBVERSION).dll
TARGET=mswin-amd64
JNI=win32

CROSS=x86_64-w64-mingw32-
CC=$(CROSS)gcc
LD=$(CROSS)ld

JJMPEG_CFLAGS=-I$(TOP)/build/$(TARGET)/include -m64
JJMPEG_LIBS=
JJMPEG_LDFLAGS = -m64 -Wl,--kill-at

all: $(LIB)

include $(TOP)/jjmpeg.mk
