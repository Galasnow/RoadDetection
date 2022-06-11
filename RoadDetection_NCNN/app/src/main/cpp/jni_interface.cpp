#include <jni.h>
#include <string>
#include <ncnn/gpu.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include "NanoDetPlus.h"
#include "YoloV5.h"



JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    ncnn::create_gpu_instance();
    if (ncnn::get_gpu_count() > 0) {
        NanoDetPlus::hasGPU = true;
    }
//    LOGD("jni onload");
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    ncnn::destroy_gpu_instance();
    delete NanoDetPlus::detector;
//    LOGD("jni onunload");
}

/*********************************************************************************************
                                         NanoDet-Plus
 ********************************************************************************************/
extern "C" JNIEXPORT void JNICALL
Java_com_roaddetection_NanoDetPlus_init(JNIEnv *env, jobject thiz, jobject assetManager, jboolean useGPU) {
    if (NanoDetPlus::detector != nullptr) {
        delete NanoDetPlus::detector;
        NanoDetPlus::detector = nullptr;
    }
    if (NanoDetPlus::detector == nullptr) {
        AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
        NanoDetPlus::detector = new NanoDetPlus(mgr, "nanodetplus.param", "nanodetplus.bin", useGPU);
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_roaddetection_NanoDetPlus_detect(JNIEnv *env, jobject thiz, jobject image, jdouble threshold, jdouble nms_threshold) {
    auto result = NanoDetPlus::detector->detect(env, image, threshold, nms_threshold);

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
