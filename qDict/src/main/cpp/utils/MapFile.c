#include "MapFile.h"

char *MapFile_Begin(MapFile * mf)
{
	return mf->data;
}

boolean MapFile_Open(MapFile * mf, const char *file_name, unsigned long file_size)
{
#ifdef _WIN32
	FILE * f = fopen(file_name, "rb");
	mf->data = malloc(file_size);

	fread(mf->data, file_size, 1, f);
	fclose(f);
#else
  mf->size = file_size;

  if ((mf->mmap_fd = open(file_name, O_RDONLY)) < 0) {
    return FALSE;	// Open file failed.
  }

  mf->data = (char *)mmap( NULL, file_size, PROT_READ, MAP_SHARED, mf->mmap_fd, 0);

  if ((void *)mf->data == (void *)(-1)) {
    close( mf->mmap_fd ); // mmap file failed.
    mf->data = NULL;
    return FALSE;

  }
#endif

  return TRUE;
}

void MapFile_Close(MapFile * mf)
{
  if (!mf->data)
    return;

#ifdef _WIN32
  free(mf->data);
#else
  munmap(mf->data, mf->size);
  close(mf->mmap_fd);
#endif

  mf->data = NULL;
}