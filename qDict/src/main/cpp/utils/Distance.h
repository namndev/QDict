#ifndef __DISTANCE_H__
#define __DISTANCE_H__

#include "DictDef.h"

#ifdef __cplusplus
extern "C" {
#endif

int CalEditDistance(const wchar *s, const wchar *t, const int limit, int * d);

#ifdef __cplusplus
}
#endif

#endif//!__DISTANCE_H__
