//
// Created by lee on 17. 6. 8.
//

#ifndef PLAYTORRENT_EXCEPTION_H
#define PLAYTORRENT_EXCEPTION_H

#include <exception>
#include <string>

class RuntimeException: public std::exception {

public:
    RuntimeException(std::string message)
    :mMessage(message){

    }

    virtual const char* what() const _GLIBCXX_USE_NOEXCEPT {
        return mMessage.c_str();
    }

    virtual ~RuntimeException() _GLIBCXX_USE_NOEXCEPT {
        mMessage.clear();
    }
private:
    std::string mMessage;
};

#endif //PLAYTORRENT_EXCEPTION_H
