//
// Created by NetEase on 16/6/6.
//

#include <stdlib.h>
#include "jni.h"

#include "bspatch.h"

#include "JNIHelper.h"

#define TAG "CandyWebCache"

static jboolean CourgettePatcher_nativeApplyPatch(JNIEnv* env, jclass, jstring javaOldFilePath,
                                                  jstring javaPatchFilePath, jstring javaOutFilePath) {
    return JNI_TRUE;
}

static JNINativeMethod gCourgettePatcherMethods[] = {
        NATIVE_METHOD(CourgettePatcher, nativeApplyPatch, "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z"),
};


static jboolean BsdiffPatcher_nativeApplyPatch(JNIEnv* env, jclass, jstring javaOldFilePath,
                                               jstring javaPatchFilePath, jstring javaOutFilePath) {
    const char* oldFilePath = env->GetStringUTFChars(javaOldFilePath, NULL);
    const char* patchFilePath = env->GetStringUTFChars(javaPatchFilePath, NULL);
    const char* outFilePath = env->GetStringUTFChars(javaOutFilePath, NULL);

    char **argv = (char **)malloc(sizeof(char *) * 5);
    argv[0] = "bspatch";
    argv[1] = const_cast<char *>(oldFilePath);
    argv[2] = const_cast<char *>(outFilePath);
    argv[3] = const_cast<char *>(patchFilePath);
    argv[4] = NULL;

    int ret = bspatch_main(4, argv);
    free(argv);

    env->ReleaseStringUTFChars(javaOutFilePath, outFilePath);
    env->ReleaseStringUTFChars(javaPatchFilePath, patchFilePath);
    env->ReleaseStringUTFChars(javaOldFilePath, oldFilePath);

    if (ret == 0) {
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

static JNINativeMethod gBsdiffPatcherMethods[] = {
          NATIVE_METHOD(BsdiffPatcher, nativeApplyPatch, "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z"),
};

static int jniRegisterNativeMethods(JNIEnv* env, const char *classPathName, JNINativeMethod *nativeMethods, jint nMethods) {
    jclass clazz;
    clazz = env->FindClass(classPathName);
    if (clazz == NULL) {
        LOGW("Native registration unable to find class '%s'", classPathName);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, nativeMethods, nMethods) < 0) {
        LOGW("RegisterNatives failed for '%s'", classPathName);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

void register_com_netease_hearttouch_candywebcache_cachemanager_CourgettePatcher(JNIEnv* env) {
    jniRegisterNativeMethods(env, "com/netease/hearttouch/candywebcache/cachemanager/CourgettePatcher",
                             gCourgettePatcherMethods, NELEM(gCourgettePatcherMethods));
}

void register_com_netease_hearttouch_candywebcache_cachemanager_BsdiffPatcher(JNIEnv* env) {
    jniRegisterNativeMethods(env, "com/netease/hearttouch/candywebcache/cachemanager/BsdiffPatcher",
                             gBsdiffPatcherMethods, NELEM(gBsdiffPatcherMethods));
}

// DalvikVM calls this on startup, so we can statically register all our native methods.
jint JNI_OnLoad(JavaVM* vm, void*) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGE("JavaVM::GetEnv() failed");
        abort();
    }
//    register_com_netease_hearttouch_candywebcache_cachemanager_CourgettePatcher(env);
    register_com_netease_hearttouch_candywebcache_cachemanager_BsdiffPatcher(env);
    return JNI_VERSION_1_6;
}