//
// Created by lee on 17. 6. 5.
//

#ifndef PLAYTORRENT_CONNECTION_H
#define PLAYTORRENT_CONNECTION_H

#include <memory>

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

        void setConnectionCallback(ConnectionCallback* callback);
    private:
        std::shared_ptr<ConnectionCallback> mCallback;
    };
}

#endif //PLAYTORRENT_CONNECTION_H
