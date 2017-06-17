#include <jni.h>
#include <unordered_map>
#include "../cpp/logging.h"
#include "../cpp/Connection.h"

#define TAG "Connection"

class JavaConnectionCallback : public PlayTorrent::ConnectionCallback {
public:
    JavaConnectionCallback(JNIEnv* env, jobject callbackInstance)
    :mEnv(env)
    ,mJavaCallback(callbackInstance){

    }

    virtual void onConnectionLost() {
        jclass cls = mEnv->GetObjectClass(mJavaCallback);
        jmethodID onConnectionLost = mEnv->GetMethodID(cls, "onConnectionLost", "()V");
        mEnv->CallVoidMethod(mJavaCallback, onConnectionLost);
    }

    virtual void onReceived(uint8_t* received, size_t length) {
        if (received == nullptr || length <= 0) {
            return;
        }

        jbyteArray jReceived = mEnv->NewByteArray(length);
        if (jReceived == nullptr) {
            LOGE(TAG, "Failed to allocate java buffer [%d]", length);
            return;
        }
        mEnv->SetByteArrayRegion(jReceived, 0, length, reinterpret_cast<jbyte*>(received));

        jclass cls = mEnv->GetObjectClass(mJavaCallback);
        jmethodID onReceived = mEnv->GetMethodID(cls, "onReceived", "([B)V");
        mEnv->CallObjectMethod(mJavaCallback, onReceived, jReceived);
    }

    virtual ~JavaConnectionCallback() {
        mEnv->DeleteGlobalRef(mJavaCallback);
    }

private:
    JNIEnv *mEnv;
    jobject mJavaCallback;
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
    JavaConnectionCallback* callbackWrapper = new JavaConnectionCallback(env, env->NewGlobalRef(callback));
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

    connection->requestSendMessage((uint8_t*)message, length);

    env->ReleaseByteArrayElements(message_, message, 0);
}