
#FFMPEG_HOME=z:/win/ffmpeg/ffmpeg-git-9c2651a-win64-dev/include
FFMPEG_HOME=/home/notzed/win/ffmpeg/ffmpeg-git-9c2651a-win64-dev/include

JJMPEG=..
VPATH=$(JJMPEG)

JNI_CFLAGS=-I$(JJMPEG)/jni -I$(JJMPEG)/jni/win32 -D_JNI_IMPLEMENTATION_
CFLAGS=$(JNI_CFLAGS) -I$(FFMPEG_HOME) -Wall -O2 -DWIN64
LIBS=#-lavcodec -lavformat -lavutil

CROSS=x86_64-w64-mingw32-

CC=$(CROSS)gcc
AR=$(CROSS)ar
LD=$(CROSS)ld

WRAPPERS=$(JJMPEG)/AVAbstract.java $(JJMPEG)/jjmpeg-jni.c

jjmpeg_dll=jjmpeg.dll

all: $(jjmpeg_dll) $(WRAPPERS)

$(WRAPPERS): 
	( cd $(JJMPEG) ; $(MAKE) )

$(jjmpeg_dll): jjmpeg.o
	$(CC) -shared -o $@ $^ $(LIBS) -Wl,--kill-at

jjmpeg.o: $(JJMPEG)/jjmpeg.c $(JJMPEG)/jjmpeg-jni.c $(JJMPEG)/jjmpeg-jni.h

# webstart jars
JDK_HOME=/usr/java/latest
jjmpeg_jar=jjmpeg-natives-windows-amd64.jar

webstart: $(jjmpeg_jar)

jar=$(JDK_HOME)/bin/jar
jarsigner=$(JDK_HOME)/bin/jarsigner
keystore=../../webstart/jjmpeg.jks
keypass=jjmpegpass

$(jjmpeg_jar): $(jjmpeg_dll)
	$(jar) cf $(jjmpeg_jar) $(jjmpeg_dll)
	$(jarsigner) -keystore $(keystore) -storepass $(keypass) -keypass $(keypass) $(jjmpeg_jar) jjmpeg

clean:
	-rm $(jjmpeg_dll)
	-rm jjmpeg.o
