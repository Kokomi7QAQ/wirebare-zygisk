LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE           := wb
LOCAL_SRC_FILES        := wb_zygisk.cpp
LOCAL_CFLAGS           := -fno-threadsafe-statics
include $(BUILD_SHARED_LIBRARY)
