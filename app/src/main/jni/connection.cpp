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

    virtual void onConnected() {
        jclass cls = mEnv->GetObjectClass(mJavaCallback);
        jmethodID onConnectedMethod = mEnv->GetMethodID(cls, "onConnected", "()V");
        mEnv->CallVoidMethod(mJavaCallback, onConnectedMethod);
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
JNIEXPORT void JNICALL
Java_playtorrent_com_playtorrent_Connection_requestConnect(JNIEnv *env, jobject instance, jint id, jstring host_, jint port) {
    const char *host = env->GetStringUTFChars(host_, 0);
    PlayTorrent::Connection* connection = nullptr;
    try {
        connection = sConnectionMap.at(id);
    } catch (std::out_of_range ignore) {
    }

    if (connection == nullptr) {
        LOGE(TAG, "Failed to find connection from memory map");
        return;
    }

    connection->requestConnect(std::string(host), port);
    env->ReleaseStringUTFChars(host_, host);
}extern "C"
JNIEXPORT void JNICALL
Java_playtorrent_com_playtorrent_Connection_requestSendMessage(JNIEnv *env, jobject instance, jint id, jbyteArray message_) {
    jbyte *message = env->GetByteArrayElements(message_, NULL);

    PlayTorrent::Connection* connection = nullptr;
    try {
        connection = sConnectionMap.at(id);
    } catch (std::out_of_range ignore) {
    }

    if (connection == nullptr) {
        LOGE(TAG, "Failed to find connection from memory map");
        return;
    }



    env->ReleaseByteArrayElements(message_, message, 0);
}