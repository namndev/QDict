#ifndef __DICT_INDEX_UTIL_H__
#define __DICT_INDEX_UTIL_H__

#include "utils/DictDef.h"
#include "utils/MapFile.h"

#define ENTR_PER_PAGE             32
#define MAX_INDEX_KEY_SIZE       256
#define UNSET_INDEX				  -1
#define INVALID_INDEX           -100

//#define MAX_WORD_COUNT       846

typedef struct _CacheFile {
	uint32 *wordoffset;
} CacheFile;

typedef struct _index_entry {
	long idx; // page number
	char keystr[MAX_INDEX_KEY_SIZE + sizeof(uint32) * 2];
} index_entry;

typedef struct _page_entry {
	char *keystr;
	uint32 off, size; // This is for OffsetIndex.
	uint32 index; // This is for IdxsynFile.
} page_entry;

typedef struct _page_t {
	long idx;
	page_entry entries[ENTR_PER_PAGE];
} page_t;

#ifdef __cplusplus
extern "C" {
#endif

boolean check_key_str_len(const char* str, size_t buf_size);
uint32 get_uint32(const char * addr);
uint32 get_byte(const char * addr);
int svn_strcmp(const char *s1, const char *s2);
int prefix_match(const char *s1, const char *s2);

#ifdef __cplusplus
}
#endif

#endif//!__DICT_INDEX_UTIL_H__
