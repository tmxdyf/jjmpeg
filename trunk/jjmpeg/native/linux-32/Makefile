
# compiler prefix for cross compiling linux 32 bit
CROSS=

CC=$(CROSS)gcc
AR=$(CROSS)ar
LD=$(CROSS)ld

JDK_HOME=/usr/java/latest
javah=$(JDK_HOME)/bin/javah

JNI_CFLAGS=-I$(JJMPEG)/jni -I$(JJMPEG)/jni/linux -D_JNI_IMPLEMENTATION_
CFLAGS=$(JNI_CFLAGS) -I/usr/include/ffmpeg -fPIC -Wall -O2 -m32
LIBS=
LDFLAGS=-m32

JJMPEG=..
VPATH=$(JJMPEG)

WRAPPERS=AVAbstract.java jjmpeg-jni.c
WRAPPERS_DVB=DVBAbstract.java jjdvb-jni.c

jjmpeg_lib=libjjmpeg.so
jjdvb_lib=libjjdvb.so

all: $(jjmpeg_lib) $(jjdvb_lib)

$(jjmpeg_lib): jjmpeg.o
	$(CC) $(LDFLAGS) -shared -Wl,-soname,libjjmpeg.so -o $@ $^ $(LIBS)

jjmpeg.o: jjmpeg.c jjmpeg-jni.c jjmpeg-jni.h

$(jjdvb_lib): jjdvb.o
	$(CC) $(LDFLAGS) -shared -Wl,-soname,libjjdvb.so -o $@ $^ $(LIBS)

jjdvb.o: jjdvb.c jjdvb-jni.c jjdvb-jni.h

clean:
	-rm jjdvb.o
	-rm jjmpeg.o
	-rm $(jjdvb_lib)
	-rm $(jjmpeg_lib)
