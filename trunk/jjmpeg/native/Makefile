
JDK_HOME=/usr/java/latest
javah=$(JDK_HOME)/bin/javah

CFLAGS=-I$(JDK_HOME)/include -I$(JDK_HOME)/include/linux -I/usr/include/ffmpeg -fPIC -Wall -g
LIBS=#-lavcodec -lavformat -lavutil

# classes containing native methods
# TODO: incomplete
CLASSES=au.notzed.jjmpeg.AVFormatContextNative\
 au.notzed.jjmpeg.AVCodecNative\
 au.notzed.jjmpeg.AVCodecContext\
 au.notzed.jjmpeg.AVFrameNative\
 au.notzed.jjmpeg.AVPacketNative\
 au.notzed.jjmpeg.AVStreamNative \
 au.notzed.jjmpeg.AVNative \
 au.notzed.jjmpeg.SwsContextNative

HEADERS=$(patsubst %,%.h,$(subst .,_,$(CLASSES)))

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
