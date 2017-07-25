//
// Created by lee on 17. 6. 5.
//

#include <string>
#include <sstream>
#include <atomic>
#include <sys/eventfd.h>
#include <sys/socket.h>
#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>
#include "Connection.h"
#include "Exception.h"
#include "logging.h"
#include "Utils.h"

#define TAG "Connection"

static std::atomic<unsigned int> sIDGenerator(0);
const int MAX_EVENT_COUNT = 128;
const int READ_BUFFER_SIZE = 1024 * 32;
const int LISTENQ = 1024;

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
    ,mReceiveBuffer(new uint8_t[READ_BUFFER_SIZE])
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

        delete[] mReceiveBuffer;
    }

    bool Connection::requestConnect(std::string host, int port) {
        // lock connection
        std::lock_guard<std::mutex> connectionLock(*const_cast<std::mutex*>(&CONNECTION_LOCK));
        if (mConnectionState != Idle) {
            LOGE(TAG, "requestConnect(), Connection state is not idle");
            return false;
        }

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
                LOGE(TAG, "Failed to connect socket %s:%d [%d] try by explicit address", host.c_str(), port, errno);
                if ((connectedSocket = connectByIp4(host, port)) < 0) {
                    continue;
                }
            }
            break;
        }
        freeaddrinfo(peer);

        if (connectedSocket > 0 && addSocketToEpoll(connectedSocket)) {
            mSocket = connectedSocket;
            mConnectionState = Connected;
            return true;
        }

        return false;
    }

    int Connection::connectByIp4(std::string host, int port) {
        if (host.length() == 0 || port < 0) {
            return -1;
        }

        // addr info


        int sock= -1;
        if ((sock = socket(PF_INET, SOCK_STREAM, 0)) < 0) {
            LOGE(TAG, "connectByIp4(), Failed to create socket");
            return -1;
        }

        struct sockaddr_in addr = {0};
        addr.sin_family = AF_INET;;
        addr.sin_port = htons(port);
        addr.sin_addr.s_addr = ((struct in_addr *)gethostbyname(host.c_str()))->s_addr;

        if (connect(sock, (sockaddr*)&addr, sizeof(sockaddr_in)) < 0) {
            LOGE(TAG, "connectByIp4(), Failed to connect socket %s:%d [%d]", host.c_str(), port, errno);
            close(sock);
            return -1;
        }

        return sock;
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
                    LOGD(TAG, "Received message");
                    onReceivedFromSocket();
                } else { // sending buffer
                    LOGD(TAG, "Send message");
                    onSendMessageQueued();
                }
            } else if (mEventPollBuffer[i].events & EPOLLHUP) {
                LOGD(TAG, "Connection lost");
                onConnectionLost();
            } else {
                LOGE(TAG, "Not handled event [%d]", mEventPollBuffer[i].events);
            }
        }
    }

    void Connection::onReceivedFromSocket() {
        // lock connection
        std::lock_guard<std::mutex> connectionLock(*const_cast<std::mutex*>(&CONNECTION_LOCK));
        if (mConnectionState != Connected) {
            LOGE(TAG, "onReceivedFromSocket(), socket is not connected");
            return;
        }

        ssize_t readBytes = recv(mSocket, (void*) mReceiveBuffer, READ_BUFFER_SIZE, 0);
        if (readBytes < 0) {
            LOGE(TAG, "onReceivedFromSocket(), Failed to read message from socket");
        }

        if (mCallback != nullptr) {
            // clone buffer
            uint8_t *clone = new uint8_t[readBytes];
            memcpy(clone, mReceiveBuffer, sizeof(uint8_t) * readBytes);
            mCallback->onReceived(clone, (size_t) readBytes);
        }
    }

    void Connection::onConnectionLost() {
        // lock connection
        std::lock_guard<std::mutex> connectionLock(*const_cast<std::mutex*>(&CONNECTION_LOCK));
        mConnectionState = Suspended;

        if (mCallback != nullptr) {
            mCallback->onConnectionLost();
        }
    }

    bool Connection::bindServerConnection(short port) {
        // lock connection
        std::lock_guard<std::mutex> connectionLock(*const_cast<std::mutex*>(&CONNECTION_LOCK));
        if (mConnectionState != Idle) {
            LOGE(TAG, "bindServerConnection(), Connection state is not idle");
            return false;
        }

        // create socket
        int sock;
        if ((sock = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
            LOGE(TAG, "bindServerConnection(), Failed to create socket");
            return false;
        }

        // bind
        struct sockaddr_in localAddress;
        memset(&localAddress, 0, sizeof(sockaddr_in));
        localAddress.sin_family = AF_INET;
        localAddress.sin_addr.s_addr = htonl(INADDR_ANY);
        localAddress.sin_port = htons(port);

        if (bind(sock, (sockaddr*) &localAddress, sizeof(localAddress)) < 0) {
            LOGE(TAG, "bindServerConnection() Failed to bind socket [%d]", errno);
            close(sock);
            return false;
        }

        // listen
        if (listen(sock, LISTENQ) < 0) {
            LOGE(TAG, "bindServerConnection() Failed to listen socket [%d]", errno);
            close(sock);
            return false;
        }

        if (addSocketToEpoll(sock)) {
            mSocket = sock;
            mConnectionState = Bind;
            return true;
        }

        return false;
    }

    bool Connection::addSocketToEpoll(int sockFd) {
        struct epoll_event socketEvent = {0};
        socketEvent.data.ptr = &mSocket;

        // EPOLLRDHUP :  detect peer shut down
        // EPOLLLET :  edge trigger ??

        socketEvent.events = EPOLLIN | EPOLLERR | EPOLLHUP | EPOLLET;
        if (epoll_ctl(mPollFd, EPOLL_CTL_ADD, sockFd, &socketEvent) != 0) {
            LOGE(TAG, "Failed to add event [%d]", errno);
            return false;
        }
        return true;
    }
}

#undef TAG