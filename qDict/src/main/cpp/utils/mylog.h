#ifndef __MY_LOG_H__
#define __MY_LOG_H__

#ifdef _WIN32
#define MyLog_v
#else
#include <android/log.h>

#define  LOG_TAG	"QDictNDK"
#define  MyLog_v(...)  __android_log_print( ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#endif

#endif//!__MY_LOG_H__
