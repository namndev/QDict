#ifndef __OFFSET_INDEX_H__
#define __OFFSET_INDEX_H__

#include "DictIndexUtil.h"


typedef struct _OffsetIndex {
	CacheFile oft_file;
	FILE *idxfile;
	uint32 npages;

	char wordentry_buf[MAX_INDEX_KEY_SIZE + sizeof(uint32) * 2];
	index_entry first, last, middle, real_last;

	char *page_data;
	int page_size;

	long wordcount;		// number of words in the index

	uint32 wordentry_offset;
	uint32 wordentry_size;

	page_t page;

} OffsetIndex;


#ifdef __cplusplus
extern "C" {
#endif

boolean OffsetIndex_New(OffsetIndex ** ppiOffsetIndex);
boolean OffsetIndex_Load(OffsetIndex * pMe, char* url, uint32 wc, uint32 fsize, boolean bCreateCacheFile);
boolean OffsetIndex_LookupSimilarWords(OffsetIndex * pMe, const char *str, long *idx, long *idx_suggest);
boolean OffsetIndex_Lookup(OffsetIndex * pMe, const char *str, long *idx, long *idx_suggest);
const char *OffsetIndex_GetKey(OffsetIndex * pMe, long idx);
void OffsetIndex_GetWordNext(OffsetIndex * pMe, long * index);
void OffsetIndex_GetKeyNData(OffsetIndex * pMe, long index, const char **key, uint32 *offset, uint32 *size);
int OffsetIndex_GetOrigWordCount(OffsetIndex * pMe, long * idx);
void OffsetIndex_Release(OffsetIndex * pMe);

#ifdef __cplusplus
}
#endif

#endif//!__OFFSET_INDEX_H__
