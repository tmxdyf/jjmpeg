# Which binary targets to build for android
ANDROID_TARGETS=armeabi armeabi-v7a
# Java cross-compilation targets
JAVA_CROSS_TARGETS=mswin-amd64
# Java native-compilation targets
JAVA_HOST_TARGETS=gnu-amd64 gnu-i386
# FFmpeg include location for host-built targets (cross is bundled)
FFMPEG_HOST_CFLAGS=-I/opt/ffmpeg-1.0/include

# Java SDK location
JDK_HOME=/usr/java/latest
# And Java home
JAVA_HOME=/usr/java/latest
# For android release, key store location
KEYSTORE=/home/notzed/.ssh/jjmpeg-release-key.keystore
# Android SDK
ANDROID_SDK=/usr/local/android-sdk-linux
# Android NDK
ANDROID_NDK=/usr/local/android-ndk-r7c

# Release version name
VERSION=1.0.0a1
# Library version tag
LIBVERSION=100
# FFmpeg version path
FFMPEG_VERSION=ffmpeg-1.0
