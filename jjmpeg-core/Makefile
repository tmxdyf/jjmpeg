# Yuck.

.PHONY: webstart none all java android dist

include ./config.mk

none:
	@echo "Select a target:"
	@echo ""
	@echo " make all - compile core, java, and android, and natives"
	@echo " make java - compile core, java classes"
	@echo " make android - compile core, android classes, and android native"
	@echo " make dist - create distribution archives of binaries"
	@echo ""

all: native-java native-android
	ant jar
	( cd ../jjmpeg-java && ant jar )
	( cd ../jjmpeg-android && ant debug )

native-java:
	( cd ../jjmpeg-java/jni && make )

native-android:
	( cd ../jjmpeg-android/jni && make )

java: native-java
	ant jar
	( cd ../jjmpeg-java && ant jar )

dist: java
	./makedist

clean:
	-rm config.sh
	ant clean

webstart:
	( cd webstart ; make )
	( cd native ; make webstart )
