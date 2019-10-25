#ifndef __DICT_ZIP_LIB_H__
#define __DICT_ZIP_LIB_H__

#include "utils/DictDef.h"
#include "utils/MapFile.h"
#include <zlib.h>


#define DICT_CACHE_SIZE 5

typedef struct _DictCache {
	int           chunk;
	char          *inBuffer;
	int           stamp;
	int           count;
}DictCache;

typedef struct _DictZipLib{
	const char    *start;	/* start of mmap'd area */
	const char    *end;		/* end of mmap'd area */
	unsigned long size;		/* size of mmap */

	int           type;
	z_stream      zStream;
	int           initialized;
  
	int           headerLength;
	int           method;
	int           flags;
	time_t        mtime;
	int           extraFlags;
	int           os;
	int           version;
	int           chunkLength;
	int           chunkCount;
	int           *chunks;
	unsigned long *offsets;	/* Sum-scan of chunks. */
	char          *origFilename;
	char          *comment;
	unsigned long crc;
	unsigned long length;
	unsigned long compressedLength;
	DictCache     cache[DICT_CACHE_SIZE];
	MapFile mapfile;
}DictZipLib;


#ifdef __cplusplus
extern "C" {
#endif

boolean DictZipLib_New(DictZipLib ** ppiDictZipLib);
boolean DictZipLib_Open(DictZipLib * pMe, char * fname);
void DictZipLib_Read(DictZipLib * pMe, char *buffer, unsigned long start, unsigned long size);
void DictZipLib_Release(DictZipLib * pMe);

#ifdef __cplusplus
}
#endif

#endif//!__DICT_ZIP_LIB_H__
