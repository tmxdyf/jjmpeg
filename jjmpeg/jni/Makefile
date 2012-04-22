
#
# Normal gnu make file calls android one
#

include ./config.mk

all: ffmpeg.built jjmpeg.built
	$(NDK)/ndk-build V=1

ffmpeg.built:
	./build-ffmpeg
	touch ffmpeg.built

jjmpeg.built: jjmpeg/native-jjmpeg.conf jjmpeg/jjmpeg.c jjmpeg/genjjmpeg.pl
	for target in $(TARGETS) ; do \
		( cd $$target ; ../jjmpeg/genjjmpeg.pl ../jjmpeg/native-jjmpeg.conf AVAbstract.java jjmpeg-jni.c ) ; \
	done
	touch jjmpeg.built
