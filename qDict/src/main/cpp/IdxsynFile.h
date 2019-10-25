#ifndef __IDXSYN_FILE_H__
#define __IDXSYN_FILE_H__

#include "DictIndexUtil.h"


typedef struct _IdxsynFile {
	CacheFile oft_file;
	FILE *idxfile;
	uint32 npages;

	char wordentry_buf[MAX_INDEX_KEY_SIZE + sizeof(uint32) * 2];
	index_entry first, last, middle, real_last;

	char *page_data;
	int page_size;

	long wordcount;		// number of words in the index

	uint32 wordentry_index;

	page_t page;

} IdxsynFile;


#ifdef __cplusplus
extern "C" {
#endif

boolean IdxsynFile_New(IdxsynFile ** ppiIdxsynFile);
boolean IdxsynFile_Load(IdxsynFile * pMe, char* url, uint32 wc, uint32 fsize, boolean bCreateCacheFile);
boolean IdxsynFile_Lookup(IdxsynFile * pMe, const char *str, long *idx, long *idx_suggest);
const char *IdxsynFile_GetKey(IdxsynFile * pMe, long idx);
void IdxsynFile_GetWordNext(IdxsynFile * pMe, long * index);
void IdxsynFile_GetKeyNData(IdxsynFile * pMe, long index, const char **key, uint32 *wordindex);
int IdxsynFile_GetOrigWordCount(IdxsynFile * pMe, long * idx);
void IdxsynFile_Release(IdxsynFile * pMe);

#ifdef __cplusplus
}
#endif

#endif//!__IDXSYN_FILE_H__
