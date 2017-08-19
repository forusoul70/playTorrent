#include <jni.h>
#include <unordered_map>
#include "../cpp/logging.h"
#include "../cpp/Connection.h"

#define TAG "Connection"

class JavaConnectionCallback : public PlayTorrent::ConnectionCallback {
public:
    JavaConnectionCallback(JavaVM* env, jobject callbackInstance)
    :mJvm(env)
    ,mJavaCallback(callbackInstance){

    }

    virtual void onConnectionLost() {
        JNIEnv* env = getEnv();
        jclass cls = env->GetObjectClass(mJavaCallback);
        jmethodID onConnectionLost = env->GetMethodID(cls, "onConnectionLost", "()V");
        env->CallVoidMethod(mJavaCallback, onConnectionLost);
        env->DeleteLocalRef(cls);
    }

    virtual void onReceived(uint8_t* received, size_t length) {
        if (received == nullptr || length <= 0) {
            return;
        }

        // get env
        JNIEnv* env = getEnv();

        // Create new java bytes array reference
        jbyteArray jReceived = env->NewByteArray(length);
        if (jReceived == nullptr) {
            LOGE(TAG, "Failed to allocate java buffer [%d]", length);
            return;
        }
        // Set data region to java bytes array
        jbyte* javaBytes = env->GetByteArrayElements(jReceived, NULL);
        env->SetByteArrayRegion(jReceived, 0, length, (const jbyte *) received);

        // call java callback function
        jclass cls = env->GetObjectClass(mJavaCallback);
        jmethodID onReceived = env->GetMethodID(cls, "onReceived", "([B)V");
        env->CallVoidMethod(mJavaCallback, onReceived, jReceived);
        env->DeleteLocalRef(cls);

        // Release local reference
        env->ReleaseByteArrayElements(jReceived, javaBytes, 0);
        env->DeleteLocalRef(jReceived);
    }

    virtual ~JavaConnectionCallback() {
        JNIEnv* env = getEnv();
        env->DeleteGlobalRef(mJavaCallback);
    }

private:
    JavaVM *mJvm;
    jobject mJavaCallback;

    JNIEnv* getEnv() {
        JNIEnv* env;
        mJvm->AttachCurrentThread(&env, 0);
        return env;
    }
};


// mapping class <java, cpp>
struct JavaObjectHasher {
    std::size_t operator()(const int& key) const
    {
        return (size_t) key;
    }
};
static std::unordered_map<int , PlayTorrent::Connection*, JavaObjectHasher> sConnectionMap;

extern "C"
JNIEXPORT jint JNICALL
Java_playtorrent_com_playtorrent_Connection_requestCreate(JNIEnv *env, jobject instance, jobject callback) {

    PlayTorrent::Connection* nativeConnection = new PlayTorrent::Connection();
    sConnectionMap.insert(std::make_pair(nativeConnection->getId(), nativeConnection));

    // create callback wrapper
    JavaVM* jvm;
    env->GetJavaVM(&jvm);

    JavaConnectionCallback* callbackWrapper = new JavaConnectionCallback(jvm, env->NewGlobalRef(callback));
    nativeConnection->setConnectionCallback(callbackWrapper);
    return nativeConnection->getId();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_playtorrent_com_playtorrent_Connection_requestConnect(JNIEnv *env, jobject instance, jint id, jstring host_, jint port) {
    const char *host = env->GetStringUTFChars(host_, 0);
    PlayTorrent::Connection* connection = nullptr;
    try {
        connection = sConnectionMap.at(id);
    } catch (std::out_of_range ignore) {
    }

    if (connection == nullptr) {
        LOGE(TAG, "Failed to find connection from memory map");
        return (jboolean) false;
    }

    bool isConnected = connection->requestConnect(std::string(host), port);
    env->ReleaseStringUTFChars(host_, host);

    return (jboolean) isConnected;
}

extern "C"
JNIEXPORT void JNICALL
Java_playtorrent_com_playtorrent_Connection_requestSendMessage(JNIEnv *env, jobject instance, jint id, jbyteArray message_) {
    jbyte *message = env->GetByteArrayElements(message_, NULL);
    int length = env->GetArrayLength(message_);

    PlayTorrent::Connection* connection = nullptr;
    try {
        connection = sConnectionMap.at(id);
    } catch (std::out_of_range ignore) {
    }

    if (connection == nullptr) {
        LOGE(TAG, "Failed to find connection from memory map");
        return;
    }

    uint8_t* clone = new uint8_t[length];
    memcpy(clone, message, sizeof(uint8_t) * length);

    connection->requestSendMessage(clone, length);
    env->ReleaseByteArrayElements(message_, message, 0);
}