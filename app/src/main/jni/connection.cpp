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

private:
    JNIEnv *mEnv;
    jobject mJavaCallback;
};


// mapping class <java, cpp>
struct JavaObjectHasher {
    std::size_t operator()(const uint64_t& key) const
    {
        return (size_t) key;
    }
};
static std::unordered_map<uint64_t , PlayTorrent::Connection*, JavaObjectHasher> sConnectionMap;

extern "C"
JNIEXPORT jlong JNICALL
Java_playtorrent_com_playtorrent_Connection_requestCreate(JNIEnv *env, jobject instance, jobject connection, jobject callback) {

    PlayTorrent::Connection* nativeConnection = new PlayTorrent::Connection();
    sConnectionMap.insert(std::make_pair((uint64_t)nativeConnection, nativeConnection));

    // create callback wrapper
    JavaConnectionCallback* callbackWrapper = new JavaConnectionCallback(env, callback);
    nativeConnection->setConnectionCallback(callbackWrapper);

    return (jlong) nativeConnection;
}

extern "C"
JNIEXPORT void JNICALL
Java_playtorrent_com_playtorrent_Connection_requestConnect(JNIEnv *env, jobject instance, jlong pointer) {
    PlayTorrent::Connection* connection = nullptr;
    try {
        connection = sConnectionMap.at((uint64_t)pointer);
    } catch (std::out_of_range ignore) {

    }

    if (connection == nullptr) {
        LOGE(TAG, "Failed to find connection from memory map");
        return;
    }

    connection->requestConnect();
}