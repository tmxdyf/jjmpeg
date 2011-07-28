
# compiler prefix for cross compiling linux x86-64
CROSS=

CC=$(CROSS)gcc
AR=$(CROSS)ar
LD=$(CROSS)ld

JDK_HOME=/usr/java/latest
javah=$(JDK_HOME)/bin/javah

JNI_CFLAGS=-I$(JJMPEG)/jni -I$(JJMPEG)/jni/linux -D_JNI_IMPLEMENTATION_
CFLAGS=$(JNI_CFLAGS) -I/usr/include/ffmpeg -fPIC -Wall -O2
LIBS=#-lavcodec -lavformat -lavutil

JJMPEG=..
VPATH=$(JJMPEG)

CLASSES=au.notzed.jjmpeg.AVFormatContext\
 au.notzed.jjmpeg.AVCodec\
 au.notzed.jjmpeg.AVCodecContext\
 au.notzed.jjmpeg.AVFrame\
 au.notzed.jjmpeg.AVPacket\
 au.notzed.jjmpeg.AVStream \
 au.notzed.jjmpeg.AVNative \
 au.notzed.jjmpeg.AVRational \
 au.notzed.jjmpeg.SwsContext

HEADERS=au_notzed_jjmpeg_AVCodecContext.h \
 au_notzed_jjmpeg_AVCodec.h\
 au_notzed_jjmpeg_AVFormatContext.h\
 au_notzed_jjmpeg_AVFrame.h\
 au_notzed_jjmpeg_AVNative.h\
 au_notzed_jjmpeg_AVPacket.h\
 au_notzed_jjmpeg_AVStream.h

WRAPPERS=AVAbstract.java jjmpeg-jni.c
WRAPPERS_DVB=DVBAbstract.java jjdvb-jni.c

jjmpeg_lib=libjjmpeg.so
jjdvb_lib=libjjdvb.so

all: $(jjmpeg_lib) $(jjdvb_lib)

$(jjmpeg_lib): jjmpeg.o
	$(CC) -shared -Wl,-soname,libjjmpeg.so -o $@ $^ $(LIBS)

jjmpeg.o: jjmpeg.c $(HEADERS) jjmpeg-jni.c jjmpeg-jni.h

$(jjdvb_lib): jjdvb.o
	$(CC) -shared -Wl,-soname,libjjdvb.so -o $@ $^ $(LIBS)

jjdvb.o: jjdvb.c $(HEADERS_DVB) jjdvb-jni.c jjdvb-jni.h

clean:
	-rm jjdvb.o
	-rm jjmpeg.o
	-rm $(jjdvb_lib)
	-rm $(jjmpeg_lib)
