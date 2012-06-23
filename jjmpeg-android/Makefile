
.PHONY: debug native

debug: native
	@echo "Building debug"
	ant debug

release: native
	ant release

native:
	( cd jni && make )

clean:
	( cd jni && make clean )
	ant clean

distclean:
	( cd jni && make distclean )
	ant clean
