//
// Created by lee on 17. 6. 5.
//

#ifndef PLAYTORRENT_CONNECTION_H
#define PLAYTORRENT_CONNECTION_H

#include <memory>
#include <sys/epoll.h>

namespace PlayTorrent {
    class ConnectionCallback {
    public:
        virtual void onConnected() = 0;
    };

    class Connection {
    public:
        Connection();
        void requestConnect();
        virtual ~Connection();

        inline int getId() {return mId;}
        void setConnectionCallback(ConnectionCallback* callback);
    private:
        int mId;
        std::shared_ptr<ConnectionCallback> mCallback;

        // poll event
        int mPollFd;
        int mSendRequestEvent;
        struct epoll_event *mEventPollBuffer;
    };
}

#endif //PLAYTORRENT_CONNECTION_H
