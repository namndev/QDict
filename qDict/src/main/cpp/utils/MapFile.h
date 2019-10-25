#ifndef __MAP_FILE_H__
#define __MAP_FILE_H__

#include "DictDef.h"

#include<sys/stat.h>
#ifdef _WIN32
#include <windows.h>
#else
#include <sys/mman.h>
#include<sys/types.h>

#include<fcntl.h>
#endif



typedef struct _MapFile {
#ifndef _WIN32
	  int mmap_fd;
#endif
      char *data;
      unsigned long size;
}MapFile;

#ifdef __cplusplus
extern "C" {
#endif

boolean MapFile_Open(MapFile * mf, const char *file_name, unsigned long file_size);
void MapFile_Close(MapFile * mf);
char *MapFile_Begin(MapFile * mf);

#ifdef __cplusplus
}
#endif

#endif//!__MAP_FILE_H__
