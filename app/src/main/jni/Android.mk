LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
 
LOCAL_MODULE    	:= mp3lame
LOCAL_CFLAGS := -g -DSTDC_HEADERS
LOCAL_C_INCLUDES := lame-3.99.5/include
LOCAL_SRC_FILES 	:= \
  lame-3.99.5/libmp3lame/bitstream.c \
  lame-3.99.5/libmp3lame/encoder.c \
  lame-3.99.5/libmp3lame/fft.c \
  lame-3.99.5/libmp3lame/gain_analysis.c \
  lame-3.99.5/libmp3lame/id3tag.c \
  lame-3.99.5/libmp3lame/lame.c \
  lame-3.99.5/libmp3lame/mpglib_interface.c \
  lame-3.99.5/libmp3lame/newmdct.c \
  lame-3.99.5/libmp3lame/presets.c \
  lame-3.99.5/libmp3lame/psymodel.c \
  lame-3.99.5/libmp3lame/quantize.c \
  lame-3.99.5/libmp3lame/quantize_pvt.c \
  lame-3.99.5/libmp3lame/reservoir.c \
  lame-3.99.5/libmp3lame/set_get.c \
  lame-3.99.5/libmp3lame/tables.c \
  lame-3.99.5/libmp3lame/takehiro.c \
  lame-3.99.5/libmp3lame/util.c \
  lame-3.99.5/libmp3lame/vbrquantize.c \
  lame-3.99.5/libmp3lame/VbrTag.c \
  lame-3.99.5/libmp3lame/version.c \
  rogermp3.c

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
