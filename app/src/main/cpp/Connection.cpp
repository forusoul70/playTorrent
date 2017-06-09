//
// Created by lee on 17. 6. 5.
//

#include <string>
#include <sstream>
#include <atomic>
#include <sys/eventfd.h>
#include <sys/socket.h>
#include <netdb.h>
#include "Connection.h"
#include "Exception.h"
#include "logging.h"
#include "Utils.h"

#define TAG "Connection"

static std::atomic<unsigned int> sIDGenerator(0);
const int MAX_EVENT_COUNT = 128;

namespace PlayTorrent {
    Connection::Connection()
    :mId(-1)
    ,mCallback(std::shared_ptr<ConnectionCallback>(nullptr))
    ,mPollFd(-1)
    ,mSendRequestEvent(-1)
    ,mEventPollBuffer(nullptr)
    ,mHost("")
    ,mPort(-1)
    {
        // generate id
        mId = ++sIDGenerator;
        if (sIDGenerator >= INT_MAX) {
            sIDGenerator = 0;
        }

        // create epoll event
        if ((mPollFd = epoll_create(MAX_EVENT_COUNT)) == -1) {
            throw new UnExpectedException("Failed to create epoll");
        }

        // setting file description flag
        int flags;
        if ((flags = fcntl(mPollFd, F_GETFD, NULL)) < 0) {
            throw new UnExpectedException("Failed to get flag from epoll");
        }

        if ((flags & FD_CLOEXEC) == false) {
            if (fcntl(mPollFd, F_SETFD, flags | FD_CLOEXEC) == -1) {
                throw new UnExpectedException("Failed to set FD_CLOEXEC to epoll");
            }
        }

        // create send request event descriptor for watching send request
        if ((mSendRequestEvent = eventfd(0, EFD_NONBLOCK)) < 0) {

        }
        struct epoll_event event = {0};
        event.data.ptr = &mSendRequestEvent;
        event.events = EPOLLIN | EPOLLET;
        if (epoll_ctl(mPollFd, EPOLL_CTL_ADD, mSendRequestEvent, &event) < 0) {
            throw new UnExpectedException("Failed to add poll event");
        }

        // create event poll buffer
        mEventPollBuffer = new epoll_event[MAX_EVENT_COUNT]; // check memory issue?
    }

    Connection::~Connection() {
        
    }

    void Connection::requestConnect(std::string host, int port) {
        if (host.empty() || port <= 0) {
            LOGE(TAG, "Input host or port is invalid %s:%d", host.c_str(), port);
            return;
        }

        // lock connection
        std::lock_guard<std::mutex> connectionLock(*const_cast<std::mutex*>(&CONNECTION_LOCK));

        // find host
        struct addrinfo hint;
        memset(&hint, 0, sizeof(addrinfo));
        hint.ai_family = AF_INET;
        hint.ai_socktype = SOCK_STREAM;

        struct addrinfo *peer = nullptr;

        if (getaddrinfo(host.c_str(), Utils::toString(port).c_str(), &hint, &peer) != 0) {
            LOGE(TAG, "Failed to get address info %s:%d", host.c_str(), port);
            return;
        }

        if (mCallback != nullptr) {
            mCallback->onConnected();
        }
    }

    void Connection::setConnectionCallback(ConnectionCallback *callback) {
        mCallback = std::shared_ptr<ConnectionCallback>(callback);
    }
}

#undef TAG