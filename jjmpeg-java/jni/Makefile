
include ../config.mk

TARGETS=$(JAVA_HOST_TARGETS) $(JAVA_CROSS_TARGETS)

CORE=../../jjmpeg-core/jni
VPATH=.:$(CORE)

# This makes sure chosen targets are built
FFBUILT=$(JAVA_CROSS_TARGETS:%=build/%/include/libavcodec/avcodec.h)
JJBUILT=$(TARGETS:%=build/%/jjmpeg-jni.c)

all: ffmpeg.built jjmpeg.built
	for target in $(TARGETS) ; do \
		( cd build/$$target ; $(MAKE) CORE="../../$(CORE)" TOP="../.." -f ../../$$target/Makefile ) ; \
	done

# If we want local build (e.g. mswin)
ffmpeg.built: build-ffmpeg $(FFBUILT)
	./build-ffmpeg
	touch ffmpeg.built

# generates platform-specific sources
jjmpeg.built: jjmpeg/native-jjmpeg.conf jjmpeg/jjmpeg.c jjmpeg/genjjmpeg.pl $(JJBUILT)
	for target in $(TARGETS) ; do \
		mkdir -p build/$$target ; \
		( cd build/$$target && ../../$(CORE)/jjmpeg/genjjmpeg.pl $$target ../../$(CORE)/jjmpeg/native-jjmpeg.conf AVAbstract.java jjmpeg-jni.c ) ; \
	done
	touch jjmpeg.built

# dummy targets, stops the script being called for every target but ensures dependencies work
$(FFBUILT):
$(JJBUILT):
