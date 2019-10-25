#ifndef __DICT_UTILS_H__
#define __DICT_UTILS_H__

#include "DictDef.h"
#include "MapFile.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifndef _WIN32
int stricmp(const char* s1, const char* s2);
#endif

#ifndef _WIN32
int strnicmp(const char* s1, const char* s2, int n);
#endif

// Only for memory leak check in MFC dictionary application.
#ifdef _WIN32
char *strdup(const char * str);
#endif

extern const char * const utf8_skip;
#define utf8_next_char(p) (char *)((p) + utf8_skip[*(const unsigned char *)(p)])

// UTF8 string.
long UTF8StrLen (const char *p, long max);
char * UTF8StrReverse (const char *str, int len);

// WString.
boolean UTF8ToWStr(const byte * pSrc,int nLen, wchar * pDst, int nSize);
boolean WStrToUTF8(const wchar * pSrc,int nLen, byte * pDst, int nSize);
void WStrLower(wchar * pszDest);

// sort.
void QSort(void *base, size_t nmemb, size_t size, int (*compar) (const void *, const void *));

#ifdef __cplusplus
}
#endif

#endif//!__DICT_UTILS_H__
