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
    enum ConnectionState {
        Idle,
        Connected,
        Connecting,
        Suspended
    };

    class Message {
    public:
        uint8_t* bytesPtr;
        size_t length;

        Message(uint8_t* bytes, size_t length) // auto delete
        :bytesPtr(bytes)
        ,length(length){

        }

        ~Message() {
            delete[] bytesPtr;
        }

        void test() {

        }
    };

    class ConnectionCallback {
    public:
        virtual void onConnectionLost() = 0;
        virtual void onReceived(uint8_t* received, size_t length) = 0;
    };

    class Connection {
    public:
        Connection();

        bool requestConnect(std::string host, int port);
        virtual ~Connection();

        inline int getId() {return mId;}
        void setConnectionCallback(ConnectionCallback* callback);
        void requestSendMessage(uint8_t* bytes, int length);
        void onSendMessageQueued();
        void onReceivedFromSocket();
        void onConnectionLost();

    private:
        void doSelectLooping();
    private:
        // mutex
        const std::mutex CONNECTION_LOCK;
        const std::mutex SENDING_BUFFER_LOCK;

        int mId;
        std::shared_ptr<ConnectionCallback> mCallback;

        // poll event
        int mPollFd;
        int mSendRequestEvent;
        struct epoll_event *mEventPollBuffer;
        pthread_t mSelectLoopingThread;

        // Connection
        ConnectionState mConnectionState;
        std::string mHost;
        int mPort;
        int mSocket;

        // buffer
        std::deque<std::shared_ptr<Message>> mSendMessageQueue;
        const uint8_t *mReceiveBuffer;
    };
}

#endif //PLAYTORRENT_CONNECTION_H
