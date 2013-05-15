LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_LDLIBS 	:= -llog
LOCAL_MODULE    := speed_test
LOCAL_SRC_FILES := speed_test.cpp

include $(BUILD_SHARED_LIBRARY)
