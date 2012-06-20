
#
# Normal gnu make file calls android one
#

include ./config.mk

CORE=../../jjmpeg-core/jni

VPATH=.:$(CORE)

all: ffmpeg.built jjmpeg.built
	$(NDK)/ndk-build V=1 NDK_DEBUG=1

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
