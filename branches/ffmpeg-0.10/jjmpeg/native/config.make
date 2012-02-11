# global configuration

# most of these in the first section are over-ridden in target specific makefiles
# location of ffmpeg includes headers/library
FFMPEG_HOME=/usr/include/ffmpeg/include

jjmpeg_lib = libjjmpeg.so
jjmpeg_jar = jjmpeg-natives-$(TARGET).jar

jjdvb_lib = libjjdvb.so
jjdvb_jar = jjdvb-natives-$(TARGET).jar

# java (jni headers included in source)
JDK_HOME=/usr/java/latest

# for the webstart stuff
jar=$(JDK_HOME)/bin/jar
jarsigner=$(JDK_HOME)/bin/jarsigner
keystore=$(SRC)/../webstart/jjmpeg.jks
keypass=jjmpegpass

# binaries
javah=$(JDK_HOME)/bin/javah
# override stupid distribution defaults of interactive rm
rm=/bin/rm

# defaults for lower level
TARGET_CC=gcc
TARGET_AR=ar
TARGET_LD=ld


