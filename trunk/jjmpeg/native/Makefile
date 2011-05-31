
JDK_HOME=/usr/java/latest
javah=$(JDK_HOME)/bin/javah

CFLAGS=-I$(JDK_HOME)/include -I$(JDK_HOME)/include/linux -I/usr/include/ffmpeg -fPIC -Wall -g
LIBS=-lavcodec -lavformat -lavutil

libdir=$(shell uname -m)

CLASSES=au.notzed.jjmpeg.AVFormatContext\
 au.notzed.jjmpeg.AVCodec\
 au.notzed.jjmpeg.AVCodecContext\
 au.notzed.jjmpeg.AVFrame\
 au.notzed.jjmpeg.AVPacket\
 au.notzed.jjmpeg.AVStream \
 au.notzed.jjmpeg.AVNative

HEADERS=au_notzed_jjmpeg_AVCodecContext.h \
 au_notzed_jjmpeg_AVCodec.h\
 au_notzed_jjmpeg_AVFormatContext.h\
 au_notzed_jjmpeg_AVFrame.h\
 au_notzed_jjmpeg_AVNative.h\
 au_notzed_jjmpeg_AVPacket.h\
 au_notzed_jjmpeg_AVStream.h

WRAPPERS=AVCodecContextAbstract.java

jjmpeg_lib=$(libdir)/libjjmpeg.so

all: genoffsets $(jjmpeg_lib) $(WRAPPERS)

$(WRAPPERS): genoffsets
	./genoffsets

$(jjmpeg_lib): jjmpeg.o
	-mkdir -p $(libdir)
	$(CC) -shared -Wl,-soname,libjjmpeg.so -o $@ $^ $(LIBS)

jjmpeg.o: jjmpeg.c $(HEADERS)

$(HEADERS):
	$(javah) -classpath ../dist/jjmpeg.jar $(CLASSES)

../dist/jjmpeg.jar:
	( cd .. ; ant )
