//
// Created by lee on 17. 6. 5.
//

#ifndef PLAYTORRENT_LOGGING_H
#define PLAYTORRENT_LOGGING_H

#include <android/log.h>

#define  LOGI(LOG_TAG,...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG, __VA_ARGS__)
#define  LOGD(LOG_TAG,...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG, __VA_ARGS__)
#define  LOGW(LOG_TAG,...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG, __VA_ARGS__)
#define  LOGE(LOG_TAG,...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG, __VA_ARGS__)

#endif //PLAYTORRENT_LOGGING_H
