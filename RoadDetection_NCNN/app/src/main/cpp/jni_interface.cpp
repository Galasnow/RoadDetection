#include <jni.h>
#include <string>
#include <ncnn/gpu.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include "NanoDet.h"



JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    ncnn::create_gpu_instance();
    if (ncnn::get_gpu_count() > 0) {
        NanoDet::hasGPU = true;
    }
//    LOGD("jni onload");
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    ncnn::destroy_gpu_instance();
    delete NanoDet::detector;
//    LOGD("jni onunload");
}

/*********************************************************************************************
                                         NanoDet
 ********************************************************************************************/
extern "C" JNIEXPORT void JNICALL
Java_com_roaddetection_NanoDet_init(JNIEnv *env, jobject thiz, jobject assetManager, jboolean useGPU) {
    if (NanoDet::detector != nullptr) {
        delete NanoDet::detector;
        NanoDet::detector = nullptr;
    }
    if (NanoDet::detector == nullptr) {
        AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
        NanoDet::detector = new NanoDet(mgr, "nanodet_plus_320.param", "nanodet_plus_320.bin", useGPU);
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_roaddetection_NanoDet_detect(JNIEnv *env, jobject thiz, jobject image, jdouble threshold, jdouble nms_threshold) {
    auto result = NanoDet::detector->detect(env, image, threshold, nms_threshold);

    auto box_cls = env->FindClass("com/roaddetection/Box");
    auto cid = env->GetMethodID(box_cls, "<init>", "(FFFFIF)V");
    jobjectArray ret = env->NewObjectArray(result.size(), box_cls, nullptr);
    int i = 0;
    for (auto &box:result) {
        env->PushLocalFrame(1);
        jobject obj = env->NewObject(box_cls, cid, box.x1, box.y1, box.x2, box.y2, box.label, box.score);
        obj = env->PopLocalFrame(obj);
        env->SetObjectArrayElement(ret, i++, obj);
    }
    return ret;
}
