#ifndef __INFO_FILE_H__
#define __INFO_FILE_H__

#include "utils/DictDef.h"

#ifdef __cplusplus
extern "C" {
#endif

boolean InfoFile_Load(char * ifofile, long *wc, long *idxfs, long *swc,
		char ** bookname, char **version, char** description,
		char ** sametypesequence);
boolean InfoFile_GetValueByKey(char * pInfoBuffer, long * pValue, char * KEY);

#ifdef __cplusplus
}
#endif

#endif//!__INFO_FILE_H__
