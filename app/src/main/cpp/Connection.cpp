//
// Created by lee on 17. 6. 5.
//

#include "Connection.h"
#include "logging.h"

#define TAG "Connection"

namespace PlayTorrent {
    Connection::Connection()
    :mCallback(std::shared_ptr<ConnectionCallback>(nullptr))
    {

    }

    Connection::~Connection() {
        
    }

    void Connection::requestConnect() {
        LOGI(TAG, "requestConnect called");
        if (mCallback != nullptr) {
            mCallback->onConnected();
        }
    }

    void Connection::setConnectionCallback(ConnectionCallback *callback) {
        LOGI(TAG, "setConnectionCallback called");
        mCallback = std::shared_ptr<ConnectionCallback>(callback);
    }
}

#undef TAG