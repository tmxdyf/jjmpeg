SRC=..
TARGET=android-arm

include $(SRC)/config.make

FFMPEG_HOME=/home/notzed/android/include

NDK=/usr/local/android-ndk-r7c
CROSS=$(NDK)/toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/bin/arm-linux-androideabi-
TARGET_JNI_CFLAGS = -I$(SRC)/jni/linux
TARGET_CFLAGS = -fPIC --sysroot=${NDK}/platforms/android-8/arch-arm/ \
	-mfloat-abi=softfp -mfpu=neon -marm -march=armv7-a \
	-DANDROID=1
TARGET_JJMPEG_CFLAGS= -I$(FFMPEG_HOME)
TARGET_JJMPEG_LIBS=
TARGET_JJMPEG_LDFLAGS = -Wl,-soname,libjjmpeg.so -llog --sysroot=${NDK}/platforms/android-8/arch-arm/

include ../Makefile.libs

all: $(jjmpeg_lib) #$(jjdvb_lib)
