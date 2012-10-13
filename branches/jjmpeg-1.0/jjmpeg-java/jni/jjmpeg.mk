# build using GNU tools
# this is included by platform specific makefile

# Yes: this is a bit of a fucking mess.

VPATH=$(CORE)/jjmpeg:$(TOP)/$(TARGET):$(TOP)/build/$(TARGET)
CFLAGS=-I$(CORE)/jjmpeg -I$(CORE)/platform -I$(TOP)/$(TARGET) -I$(TOP)/build/$(TARGET) -I$(CORE)/jni -I$(CORE)/jni/$(JNI) $(JJMPEG_CFLAGS)

$(LIB): jjmpeg.o
	echo "building $(LIB) $^"
	$(CC) $(LDFLAGS) -shared $(JJMPEG_LDFLAGS) -o $@ $^ $(LIBS) $(JJMPEG_LIBS)

jjmpeg.o: jjmpeg.c jjmpeg-jni.h jjmpeg-jni.c jjmpeg-platform.c jjmpeg-platform.h
