#include <stdlib.h>
#include <unistd.h>

#include "zygisk.hpp"

using zygisk::Api;
using zygisk::AppSpecializeArgs;
using zygisk::ServerSpecializeArgs;

#define GID_PERMISSION_INTERNET 3003 // android.permission.INTERNET

class WireBareZygisk : public zygisk::ModuleBase {
public:
    void onLoad(Api *api_, JNIEnv *env_) override {
        this->api = api_;
        this->env = env_;
    }

    void postAppSpecialize(const AppSpecializeArgs *args) override {
        api->setOption(zygisk::Option::DLCLOSE_MODULE_LIBRARY);

        // ensure App has requested internet permission
        bool requested_internet = false;
        jintArray gids = args->gids;
        if (gids != nullptr) {
            jint *arr = env->GetIntArrayElements(gids, nullptr);
            jsize len = env->GetArrayLength(gids);
            for (int i = 0; i < len; i++) {
                gid_t gid = arr[i];
                if (gid == GID_PERMISSION_INTERNET) {
                    requested_internet = true;
                    break;
                }
            }
            env->ReleaseIntArrayElements(gids, arr, JNI_ABORT);
        }
        if (!requested_internet) return;

        // let App use certificate in system/etc/security/cacerts instead of apex/com.android.conscrypt/cacerts
        // see org/conscrypt/TrustedCertificateStore.java for details
        jclass system = env->FindClass("java/lang/System");
        jmethodID setprop = env->GetStaticMethodID(
                system,
                "setProperty",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
        );
        jstring prop = env->NewStringUTF("system.certs.enabled");
        jstring value = env->NewStringUTF("true");
        env->CallStaticObjectMethod(system, setprop, prop, value);
    }

    void postServerSpecialize([[maybe_unused]] const ServerSpecializeArgs *args) override {
        api->setOption(zygisk::Option::DLCLOSE_MODULE_LIBRARY);
    }

private:
    Api *api{};
    JNIEnv *env{};
};

REGISTER_ZYGISK_MODULE(WireBareZygisk)
