#include <jni.h>


#ifndef _Included_com_rogertalk_roger_audio_RogerLame
#define _Included_com_rogertalk_roger_audio_RogerLame
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_rogertalk_roger_audio_RogerLame_init
(JNIEnv *, jclass, jint, jint, jint, jint, jint, jstring, jstring);


JNIEXPORT jint JNICALL Java_com_rogertalk_roger_audio_RogerLame_encode
        (JNIEnv *, jclass, jshortArray, jshortArray, jint, jbyteArray);


JNIEXPORT jint JNICALL Java_com_rogertalk_roger_audio_RogerLame_flush
        (JNIEnv *, jclass, jbyteArray);


JNIEXPORT void JNICALL Java_com_rogertalk_roger_audio_RogerLame_close
(JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif