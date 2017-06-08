//
// Created by lee on 17. 6. 8.
//

#ifndef PLAYTORRENT_EXCEPTION_H
#define PLAYTORRENT_EXCEPTION_H

#include <exception>
#include <string>

class UnExpectedException: public std::exception {

public:
    UnExpectedException(std::string message)
    :mMessage(message){

    }

    virtual const char* what() const _GLIBCXX_USE_NOEXCEPT {
        return mMessage.c_str();
    }

    virtual ~UnExpectedException() _GLIBCXX_USE_NOEXCEPT {
        mMessage.clear();
    }
private:
    std::string mMessage;
};

#endif //PLAYTORRENT_EXCEPTION_H
