
JDK_HOME=/usr/java/latest
javah=$(JDK_HOME)/bin/javah

CFLAGS=-I${JDK_JOME}/include -I/usr/include/ffmpeg -fPIC -Wall -g
LIBS=-lavcodec -lavformat -lavutil

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

all: genoffsets libjjmpeg.so $(WRAPPERS)

$(WRAPPERS): genoffsets
	./genoffsets

libjjmpeg.so: jjmpeg.o
	$(CC) -shared -Wl,-soname,libjjmpeg.so -o $@ $^ $(LIBS)

jjmpeg.o: jjmpeg.c $(HEADERS)

$(HEADERS):
	$(javah) -classpath ../dist/jjmpeg.jar $(CLASSES)

../dist/jjmpeg.jar:
	( cd .. ; ant )
