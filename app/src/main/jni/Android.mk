LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=off
OPENCV_LIB_TYPE:=STATIC
include /Users/cdrsk/Downloads/OpenCV-2.4.11-android-sdk/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk
LOCAL_SRC_FILES  := DetectionBasedTracker.cpp

#LOCAL_C_INCLUDES += $(LOCAL_PATH)

LOCAL_LDLIBS +=  -llog -ldl
LOCAL_LDLIBS += -L$(SYSROOT)/usr/lib -llog
LOCAL_MODULE     := detection_based_tracker

include $(BUILD_SHARED_LIBRARY)