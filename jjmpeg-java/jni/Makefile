
include ./config.mk

TARGETS=$(HOST_TARGETS) $(CROSS_TARGETS)

CORE=../../jjmpeg-core/jni
VPATH=.:$(CORE)

all: ffmpeg.built jjmpeg.built
	for target in $(TARGETS) ; do \
		( cd build/$$target ; $(MAKE) CORE="../../$(CORE)" TOP="../.." -f ../../$$target/Makefile ) ; \
	done

# If we want local build (e.g. mswin)
ffmpeg.built: build-ffmpeg
	./build-ffmpeg
	touch ffmpeg.built

# generates platform-specific sources
jjmpeg.built: jjmpeg/native-jjmpeg.conf jjmpeg/jjmpeg.c jjmpeg/genjjmpeg.pl
	for target in $(TARGETS) ; do \
		mkdir -p build/$$target ; \
		( cd build/$$target && ../../$(CORE)/jjmpeg/genjjmpeg.pl $$target ../../$(CORE)/jjmpeg/native-jjmpeg.conf AVAbstract.java jjmpeg-jni.c ) ; \
	done
	touch jjmpeg.built
