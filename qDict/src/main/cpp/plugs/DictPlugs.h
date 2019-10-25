#ifndef __DICT_PLUGS_H__
#define __DICT_PLUGS_H__

#include "../utils/DictDef.h"
#include "../utils/DictUtils.h"

#ifdef __cplusplus
extern "C" {
#endif

char * DictPlugs_ParseData(char * data, int size, char * sameTypeSequence);

#ifdef __cplusplus
}
#endif

#endif//!__DICT_PLUGS_H__
