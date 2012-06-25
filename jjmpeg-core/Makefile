# Yuck.

.PHONY: webstart none all java android dist

include ./config.mk

none:
	@echo "Select a target:"
	@echo ""
	@echo " make android TARGET=target, build specific android target, unsigned release package"
	@echo " make java, build configured java targets"
	@echo " make dist, build all for all configured targets"
	@echo ""

android: native-android
	( cd ../jjmpeg-android && ./configure --target=$(TARGET) && cd jni && make )
	( cd ../jjmpeg-android && ant release )

java:
	( cd ../jjmpeg-java && ./configure && cd jni && make )
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
