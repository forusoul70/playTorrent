//
// Created by lee on 17. 6. 5.
//

#ifndef PLAYTORRENT_CONNECTION_H
#define PLAYTORRENT_CONNECTION_H

#include <memory>
#include <sys/epoll.h>
#include <mutex>
#include <deque>
#include <pthread.h>

namespace PlayTorrent {
    class ConnectionCallback {
    public:
        virtual void onConnected() = 0;
    };

    class Connection {
    public:
        Connection();
        void requestConnect(std::string host, int port);
        virtual ~Connection();

        inline int getId() {return mId;}
        void setConnectionCallback(ConnectionCallback* callback);
        void requestSendMessage(std::shared_ptr<uint8_t*> message, int size);
    private:
        int mId;
        std::shared_ptr<ConnectionCallback> mCallback;

        // poll event
        int mPollFd;
        int mSendRequestEvent;
        struct epoll_event *mEventPollBuffer;
        pthread_t mSelectLoopingThread;

        // Connection
        const std::mutex CONNECTION_LOCK;
        std::string mHost;
        int mPort;

        // send buffer
        const std::mutex SENDING_BUFFER_LOCK;
        std::deque<std::shared_ptr<uint8_t*>> mSendMessageQueue;
    };
}

#endif //PLAYTORRENT_CONNECTION_H
