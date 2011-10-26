
JDK_HOME=/usr/java/latest
javah=$(JDK_HOME)/bin/javah

CFLAGS=-I$(JDK_HOME)/include -I$(JDK_HOME)/include/linux -I/usr/include/ffmpeg -fPIC -Wall -O2
LIBS=#-lavcodec -lavformat -lavutil

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

SUBDIRS=linux-amd64 linux-32 win-amd64

all: $(WRAPPERS) $(WRAPPERS_DVB) $(HEADERS) $(HEADERS_DVB)

$(WRAPPERS): genjjmpeg.pl native.conf
	./genjjmpeg.pl

$(HEADERS):
	$(javah) -classpath ../dist/jjmpeg.jar $(CLASSES)

../dist/jjmpeg.jar:
	( cd .. ; ant )


HEADERS_DVB=

$(WRAPPERS_DVB): gendvb.pl native-dvb.conf
	./gendvb.pl

clean all webstart:
	for name in $(SUBDIRS) ; do \
	  ( cd $$name && $(MAKE) $@ ); \
	done
