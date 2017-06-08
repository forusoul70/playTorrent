//
// Created by lee on 17. 6. 5.
//

#include <atomic>
#include <climits>
#include <sys/eventfd.h>
#include "Connection.h"
#include "Exception.h"

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