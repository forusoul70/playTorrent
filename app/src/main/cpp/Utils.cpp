//
// Created by lee on 17. 6. 9.
//

#include <sstream>
#include "Utils.h"

namespace PlayTorrent {
    std::string Utils::toString(int value) {
        std::ostringstream oss;
        oss << value;
        oss.flush();
        return oss.str();
    }
}