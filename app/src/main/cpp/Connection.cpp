//
// Created by lee on 17. 6. 5.
//

#include <atomic>
#include <climits>
#include "Connection.h"

#define TAG "Connection"

static std::atomic<unsigned int> sIDGenerator(0);

namespace PlayTorrent {
    Connection::Connection()
    :mId(-1)
    ,mCallback(std::shared_ptr<ConnectionCallback>(nullptr))
    {
        mId = ++sIDGenerator;
        if (sIDGenerator >= INT_MAX) {
            sIDGenerator = 0;
        }
    }

    Connection::~Connection() {
        
    }

    void Connection::requestConnect() {
        if (mCallback != nullptr) {
            mCallback->onConnected();
        }
    }

    void Connection::setConnectionCallback(ConnectionCallback *callback) {
        mCallback = std::shared_ptr<ConnectionCallback>(callback);
    }
}

#undef TAG