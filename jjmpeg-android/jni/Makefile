
#
# Normal gnu make file calls android one after building
# one specific architecture
#

include ../config.mk

CORE=../../jjmpeg-core/jni

VPATH=.:$(CORE)

# This makes sure chosen targets are built
FFBUILT=$(TARGET:%=build/%/include/libavcodec/avcodec.h)
JJBUILT=$(TARGET:%=build/%/jjmpeg-jni.c)

all: ${FFBUILT} ${JJBUILT}
	$(ANDROID_NDK)/ndk-build V=1 NDK_DEBUG=1

${FFBUILT}: build-ffmpeg
	./build-ffmpeg

# generates platform-specific sources
$(JJBUILT): jjmpeg/native-jjmpeg.conf jjmpeg/jjmpeg.c jjmpeg/genjjmpeg.pl
	mkdir -p build/$(TARGET)
	( cd build/$(TARGET) && ../../$(CORE)/jjmpeg/genjjmpeg.pl $(TARGET) ../../$(CORE)/jjmpeg/native-jjmpeg.conf AVAbstract.java jjmpeg-jni.c )

clean:
	rm -rf build/$(TARGET)

distclean:
	rm -rf build
