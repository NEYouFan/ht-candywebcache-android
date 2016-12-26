include $(call all-subdir-makefiles)

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#source code
LOCAL_SRC_FILES = $(LOCAL_PATH)/3rd/bsdiff/bsdiff.c \
	$(LOCAL_PATH)/3rd/bsdiff/bspatch.c \
	$(LOCAL_PATH)/3rd/bzip2/blocksort.c \
	$(LOCAL_PATH)/3rd/bzip2/bzlib.c \
	$(LOCAL_PATH)/3rd/bzip2/compress.c \
	$(LOCAL_PATH)/3rd/bzip2/crctable.c \
	$(LOCAL_PATH)/3rd/bzip2/decompress.c \
	$(LOCAL_PATH)/3rd/bzip2/huffman.c \
	$(LOCAL_PATH)/3rd/bzip2/randtable.c

LOCAL_SRC_FILES += $(LOCAL_PATH)/PatcherJni.cpp

LOCAL_CFLAGS := -DDEBUG

# LOCAL_CPPFLAGS :=

LOCAL_C_INCLUDES += $(LOCAL_PATH)/3rd/bsdiff \
	$(LOCAL_PATH)/3rd/bzip2 \
	$(LOCAL_PATH)/

LOCAL_LDLIBS := -llog -landroid -lz

LOCAL_SHARED_LIBRARIES :=

LOCAL_MODULE:= patcher

include $(BUILD_SHARED_LIBRARY)