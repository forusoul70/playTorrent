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
    ,mConnectionState(Idle)
    ,mHost("")
    ,mPort(-1)
    ,mSocket(-1)
    {
        // generate id
        mId = ++sIDGenerator;
        if (sIDGenerator >= INT_MAX) {
            sIDGenerator = 0;
        }

        // create epoll event
        if ((mPollFd = epoll_create(MAX_EVENT_COUNT)) == -1) {
            throw new RuntimeException("Failed to create epoll");
        }

        // setting file description flag
        int flags;
        if ((flags = fcntl(mPollFd, F_GETFD, NULL)) < 0) {
            throw new RuntimeException("Failed to get flag from epoll");
        }

        if ((flags & FD_CLOEXEC) == false) {
            if (fcntl(mPollFd, F_SETFD, flags | FD_CLOEXEC) == -1) {
                throw new RuntimeException("Failed to set FD_CLOEXEC to epoll");
            }
        }

        // create send request event descriptor for watching send request
        if ((mSendRequestEvent = eventfd(0, EFD_NONBLOCK)) < 0) {

        }
        struct epoll_event event = {0};
        event.data.ptr = &mSendRequestEvent;
        event.events = EPOLLIN | EPOLLET;
        if (epoll_ctl(mPollFd, EPOLL_CTL_ADD, mSendRequestEvent, &event) < 0) {
            throw new RuntimeException("Failed to add poll event");
        }

        // create event poll buffer
        mEventPollBuffer = new epoll_event[MAX_EVENT_COUNT]; // check memory issue?

        // create select looping thread
        auto lambdaOnThread = [](void* start_routine) -> void* {
            Connection* connection = (Connection*)start_routine;
            if (connection == nullptr) {
                return nullptr;
            }

            for (;;) {
                try {
                    connection->doSelectLooping();
                } catch(RuntimeException e) {
                    LOGE(TAG, "Looping finished [%s}", e.what());
                    break;
                }
            }
            return nullptr;
        };
        pthread_create(&mSelectLoopingThread, nullptr, lambdaOnThread, this);
    }

    Connection::~Connection() {
        LOGD(TAG, "Finish connection");

        if (mEventPollBuffer != nullptr) {
            delete[] mEventPollBuffer;
        }
    }

    bool Connection::requestConnect(std::string host, int port) {
        if (host.empty() || port <= 0) {
            LOGE(TAG, "Input host or port is invalid %s:%d", host.c_str(), port);
            return false;
        }

        // find host
        struct addrinfo hint;
        memset(&hint, 0, sizeof(addrinfo));
        hint.ai_family = AF_INET;
        hint.ai_socktype = SOCK_STREAM;

        struct addrinfo *peer = nullptr;

        if (getaddrinfo(host.c_str(), Utils::toString(port).c_str(), &hint, &peer) != 0) {
            LOGE(TAG, "Failed to get address info %s:%d", host.c_str(), port);
            return false;
        }

        int connectedSocket = -1;
        for (addrinfo* p = peer; p != nullptr; p = p->ai_next) {
            if ((connectedSocket = socket(p->ai_family, p->ai_socktype, 0)) < 0) {
                LOGE(TAG, "Failed to create socket");
                continue;
            }

            if (connect(connectedSocket, p->ai_addr, p->ai_addrlen) < 0) {
                LOGE(TAG, "Failed to connect socket");
                continue;
            }
            break;
        }

        if (connectedSocket > 0) {
            // lock connection
            std::lock_guard<std::mutex> connectionLock(*const_cast<std::mutex*>(&CONNECTION_LOCK));
            mSocket = connectedSocket;
            mConnectionState = Connected;
            return true;
        }

        return false;
    }

    void Connection::setConnectionCallback(ConnectionCallback *callback) {
        mCallback = std::shared_ptr<ConnectionCallback>(callback);
    }

    void Connection::requestSendMessage(uint8_t* bytes, int length) {
        if (bytes == nullptr || length <= 0) {
            LOGE(TAG, "requestSendMessage(), invalid input parameters");
            return;
        }

        if (mSendRequestEvent < 0) {
            LOGE(TAG, "requestSendMessage(), Current poll event fd is empty");
        }

        // lock sending buffer
        std::lock_guard<std::mutex> connectionLock(*const_cast<std::mutex*>(&SENDING_BUFFER_LOCK));
        std::shared_ptr<Message> message = std::shared_ptr<Message>(new Message(bytes, length));
        mSendMessageQueue.push_back(message);

        // wake up
        eventfd_write(mSendRequestEvent, 1);
    }

    void Connection::onSendMessageQueued() {
        // lock connection
        std::lock_guard<std::mutex> connectionLock(*const_cast<std::mutex*>(&CONNECTION_LOCK));

        // check connection state
        if (mConnectionState != Connected) {
            LOGE(TAG, "onSendMessageQueued(), Socket is not connected");
            const_cast<std::mutex*>(&SENDING_BUFFER_LOCK)->unlock();
            return;
        }

        // deque
        std::shared_ptr<Message> message;
        const_cast<std::mutex*>(&SENDING_BUFFER_LOCK)->lock();
        message = mSendMessageQueue.at(0);
        if (message != nullptr) {
            mSendMessageQueue.pop_front();
        }
        const_cast<std::mutex*>(&SENDING_BUFFER_LOCK)->unlock();

        if (message == nullptr) {
            LOGE(TAG, "onSendMessageQueued(), Failed to pop message");
            return;
        }

        // send
        size_t remainLength = message->length;
        while (remainLength > 0) {
            ssize_t count = send(mSocket, message->bytesPtr, message->length, 0);
            if (count < 0) {
                LOGE(TAG, "Failed to send data to socket");
                return;
            }
            remainLength -= count;
        }
    }

    void Connection::doSelectLooping() {
        if (mPollFd < 0 || mEventPollBuffer == nullptr) {
            LOGE(TAG, "Can't watch poll event, because current epoll description is empty");
            return;
        }

        int eventCount = 0;
        if ((eventCount =  epoll_wait(mPollFd, mEventPollBuffer, MAX_EVENT_COUNT, 1000)) < 0) {
            LOGD(TAG, "Failed to wait error : [%d] pollFd : [%d]", mPollFd, errno);
            throw new RuntimeException("Failed to wait");
        }

        for (int32_t i=0; i < eventCount; i++) {
            if (mEventPollBuffer[i].events & EPOLLIN) {
                if (mEventPollBuffer[i].data.ptr == &mSocket) { // socket

                } else { // sending buffer
                    onSendMessageQueued();
                }
            } else {
                LOGE(TAG, "Not handled event [%d]", mEventPollBuffer[i].events);
            }
        }
    }
}

#undef TAG