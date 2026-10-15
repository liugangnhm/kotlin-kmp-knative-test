#include <jni.h>
#include "demo.h"

JNIEXPORT jlong JNICALL
Java_demo_android_NativeBridge_sum(JNIEnv *env, jclass type, jintArray values) {
    (void)type;
    const jsize count = (*env)->GetArrayLength(env, values);
    if (count == 0) {
        return (jlong)demo_sum(NULL, 0);
    }

    jint *elements = (*env)->GetIntArrayElements(env, values, NULL);
    if (elements == NULL) {
        return 0; /* The JVM has a pending OutOfMemoryError. */
    }
    const int64_t result = demo_sum((const int32_t *)elements, count);
    (*env)->ReleaseIntArrayElements(env, values, elements, JNI_ABORT);
    return (jlong)result;
}

JNIEXPORT jstring JNICALL
Java_demo_android_NativeBridge_backend(JNIEnv *env, jclass type) {
    (void)type;
    return (*env)->NewStringUTF(env, demo_backend());
}
