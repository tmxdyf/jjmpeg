
JDK_HOME=/usr/java/latest
javah=$(JDK_HOME)/bin/javah

CFLAGS=-I$(JDK_HOME)/include -I$(JDK_HOME)/include/linux -I/usr/include/ffmpeg -fPIC -Wall -O2
LIBS=#-lavcodec -lavformat -lavutil

libdir=$(shell uname -m)

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

jjmpeg_lib=$(libdir)/libjjmpeg.so
jjdvb_lib=$(libdir)/libjjdvb.so

all: $(jjmpeg_lib) $(WRAPPERS) $(jjdvb_lib) $(WRAPPERS_DVB)

$(WRAPPERS): gennative.pl native.conf
	./gennative.pl

$(jjmpeg_lib): jjmpeg.o
	-mkdir -p $(libdir)
	$(CC) -shared -Wl,-soname,libjjmpeg.so -o $@ $^ $(LIBS)

jjmpeg.o: jjmpeg.c $(HEADERS) jjmpeg-jni.c jjmpeg-jni.h

$(HEADERS):
	$(javah) -classpath ../dist/jjmpeg.jar $(CLASSES)

../dist/jjmpeg.jar:
	( cd .. ; ant )




HEADERS_DVB=

$(WRAPPERS_DVB): gendvb.pl native-dvb.conf
	./gendvb.pl

$(jjdvb_lib): jjdvb.o
	-mkdir -p $(libdir)
	$(CC) -shared -Wl,-soname,libjjdvb.so -o $@ $^ $(LIBS)

jjdvb.o: jjdvb.c $(HEADERS_DVB) jjdvb-jni.c jjdvb-jni.h
