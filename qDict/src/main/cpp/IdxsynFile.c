#include "IdxsynFile.h"
#include "utils/DictUtils.h"
#include <sys/endian.h>

static const char *IdxsynFile_ReadFirstOnPageKey(IdxsynFile * pMe, long page_idx)
{
	uint32 page_size = 0, minsize = 0;

	fseek(pMe->idxfile, pMe->oft_file.wordoffset[page_idx], SEEK_SET);
	page_size = pMe->oft_file.wordoffset[page_idx + 1] - pMe->oft_file.wordoffset[page_idx];
	minsize = sizeof(pMe->wordentry_buf);
	if (page_size < minsize)
		minsize = page_size;
	fread(pMe->wordentry_buf, minsize, 1, pMe->idxfile);
	if(!check_key_str_len(pMe->wordentry_buf, minsize)) {
		pMe->wordentry_buf[minsize-1] = '\0';
		// Error.Index key length exceeds allowed limit.
		return NULL;
	}

	return pMe->wordentry_buf;
}

static const char *IdxsynFile_GetFirstOnPageKey(IdxsynFile * pMe, long page_idx)
{
	if (page_idx < pMe->middle.idx) {
		if (page_idx == pMe->first.idx)
			return pMe->first.keystr;
		return IdxsynFile_ReadFirstOnPageKey(pMe, page_idx);
	} else if (page_idx > pMe->middle.idx) {
		if (page_idx == pMe->last.idx)
			return pMe->last.keystr;
		return IdxsynFile_ReadFirstOnPageKey(pMe, page_idx);
	} else
		return pMe->middle.keystr;
}

static boolean IdxsynFile_PageDataResize(IdxsynFile * pMe, uint32 size)
{
	if(NULL != pMe->page_data)
	{
		free( pMe->page_data );
		pMe->page_data = NULL;
	}

	if ((pMe->page_data = malloc(size)))
	if (NULL == pMe->page_data)
		return FALSE;

	return TRUE;
}

static void IdxsynFile_PageFill(IdxsynFile * pMe, char *data, int nent, long idx)
{
	long len = 0;
	int i = 0;
	char *p = data;
	pMe->page.idx = idx;

	for (i = 0; i < nent; ++i) {
		pMe->page.entries[i].keystr=p;
		len = strlen(p);
		p += len + 1;
		pMe->page.entries[i].index = ntohl(get_uint32(p));
		p += sizeof(uint32);
	}
}

static uint32 IdxsynFile_LoadPage(IdxsynFile * pMe, long page_idx)
{
	uint32 nentr = ENTR_PER_PAGE;
	if (page_idx == (long)(pMe->npages - 2))
		if ((nentr = pMe->wordcount % ENTR_PER_PAGE) == 0)
			nentr = ENTR_PER_PAGE;

	if (page_idx != pMe->page.idx) {
		int page_size = pMe->oft_file.wordoffset[page_idx+1] - pMe->oft_file.wordoffset[page_idx];

		if(page_size > pMe->page_size)
		{
			pMe->page_size = page_size;
			IdxsynFile_PageDataResize(pMe, page_size);
		}

		fseek(pMe->idxfile, pMe->oft_file.wordoffset[page_idx], SEEK_SET);
		fread(pMe->page_data, 1, page_size, pMe->idxfile);
		IdxsynFile_PageFill(pMe, pMe->page_data, nentr, page_idx);
	}

	return nentr;
}

const char *IdxsynFile_GetKey(IdxsynFile * pMe, long idx)
{
	long idx_in_page = 0;

	IdxsynFile_LoadPage(pMe, idx / ENTR_PER_PAGE);
	idx_in_page = idx % ENTR_PER_PAGE;
	pMe->wordentry_index = pMe->page.entries[idx_in_page].index;

	return pMe->page.entries[idx_in_page].keystr;
}

void IdxsynFile_GetWordNext(IdxsynFile * pMe, long * index)
{
	long idx = *index;
	char *cWord = NULL;
	const char *pWord;
	boolean found = FALSE;

	if(idx < 0 || idx > pMe->wordcount)
		return;

	cWord = strdup(IdxsynFile_GetKey(pMe, idx));

	while (idx < pMe->wordcount - 1) {
		pWord = IdxsynFile_GetKey(pMe, idx + 1);
		if (strcmp(pWord, cWord) != 0) {
			found = TRUE;
			break;
		}
		idx++;
	}
	free(cWord);
	if (found)
		idx++;
	else
		idx=INVALID_INDEX;

	*index = idx;
}

boolean IdxsynFile_Load(IdxsynFile * pMe, char* url, uint32 wc, uint32 fsize, boolean bCreateCacheFile)
{
	MapFile mapfile;
	char * idxdatabuffer = NULL;
	char * p1 = NULL;
	uint32 index_size = 0;
	uint32 i = 0, j = 0;
	uint32 npages = 0;

	pMe->wordcount = wc;
	pMe->npages = (wc - 1) / ENTR_PER_PAGE + 2;
	npages = pMe->npages;

    pMe->idxfile = fopen(url, "rb");
	if (NULL == pMe->idxfile) {
       return FALSE;
	}

	if (!MapFile_Open(&mapfile, url, fsize)) {
	    return FALSE;
	}
	idxdatabuffer = MapFile_Begin(&mapfile);
	pMe->oft_file.wordoffset = malloc(npages * sizeof(uint32));
	p1 = idxdatabuffer;
	for (i = 0; i < wc; i++) {
		index_size = strlen(p1) + 1 + sizeof(uint32);
		if (i % ENTR_PER_PAGE == 0) {
			pMe->oft_file.wordoffset[j] = p1 - idxdatabuffer;
			++j;
		}
		p1 += index_size;
	}
	pMe->oft_file.wordoffset[j] = p1 - idxdatabuffer;

	pMe->first.idx = 0;
	strcpy(pMe->first.keystr, IdxsynFile_ReadFirstOnPageKey(pMe, 0));
	pMe->last.idx = npages - 2;
	strcpy(pMe->last.keystr, IdxsynFile_ReadFirstOnPageKey(pMe, npages - 2));
	pMe->middle.idx = (npages - 2) / 2;
	strcpy(pMe->middle.keystr, IdxsynFile_ReadFirstOnPageKey(pMe, (npages - 2) / 2));
	pMe->real_last.idx = wc - 1;
	strcpy(pMe->real_last.keystr, IdxsynFile_GetKey(pMe, wc - 1));
	MapFile_Close(&mapfile);

	return TRUE;
}

boolean IdxsynFile_Lookup(IdxsynFile * pMe, const char *str, long *idx, long *idx_suggest)
{
	long idx2 = 0, idx_suggest2 = 0;
	boolean bFound = FALSE;
	long iFrom = 0;
	long iTo = pMe->npages - 2;
	int cmpint = 0;
	long iThisIndex = 0;

	if (svn_strcmp(str, pMe->first.keystr)<0) {
		*idx = 0;
		*idx_suggest = 0;
		return FALSE;
	} else if (svn_strcmp(str, pMe->real_last.keystr) >0) {
		*idx = INVALID_INDEX;
		*idx_suggest = pMe->wordcount - 1;
		return FALSE;
	} else {
		// find the page number where the search word might be
		iFrom = 0;
		iThisIndex = 0;
		while (iFrom <= iTo) {
			iThisIndex = (iFrom + iTo) / 2;
			cmpint = svn_strcmp(str, IdxsynFile_GetFirstOnPageKey(pMe, iThisIndex));
			if (cmpint > 0)
				iFrom = iThisIndex + 1;
			else if (cmpint < 0)
				iTo = iThisIndex - 1;
			else {
				bFound = TRUE;
				break;
			}
		}
		if (!bFound) {
			idx2 = iTo;    //prev
		} else {
			idx2 = iThisIndex;
		}
	}
	if (!bFound) {
		// the search word is on the page number idx if it's anywhere
		uint32 netr = IdxsynFile_LoadPage(pMe, idx2);
		iFrom =  1; // Needn't search the first word anymore.
		iTo = netr - 1;
		iThisIndex = 0;
		while (iFrom <= iTo) {
			iThisIndex = (iFrom + iTo) / 2;
			cmpint = svn_strcmp(str, pMe->page.entries[iThisIndex].keystr);
			if (cmpint > 0)
				iFrom = iThisIndex + 1;
			else if (cmpint < 0)
				iTo = iThisIndex - 1;
			else {
				bFound = TRUE;
				break;
			}
		}
		idx2 *= ENTR_PER_PAGE;
		if (!bFound) {
			int best, back;
			idx2 += iFrom;    //next
			idx_suggest2 = idx2;

			best = prefix_match (str, pMe->page.entries[idx_suggest2 % ENTR_PER_PAGE].keystr);
			for (;;) {
				if ((iTo = idx_suggest2 - 1) < 0)
					break;
				if (idx_suggest2 % ENTR_PER_PAGE == 0)
					IdxsynFile_LoadPage(pMe, iTo / ENTR_PER_PAGE);
				back = prefix_match (str, pMe->page.entries[iTo % ENTR_PER_PAGE].keystr);
				if (!back || back < best)
					break;
				best = back;
				idx_suggest2 = iTo;
			}
		} else {
			idx2 += iThisIndex;
			idx_suggest2 = idx2;
		}
	} else {
		idx2 *=ENTR_PER_PAGE;
		idx_suggest2 = idx2;
	}

	*idx = idx2;
	*idx_suggest = idx_suggest2;

	return bFound;
}

void IdxsynFile_GetKeyNData(IdxsynFile * pMe, long index, const char **key, uint32 *wordindex)
{
	*key = IdxsynFile_GetKey(pMe, index);
	*wordindex = pMe->wordentry_index;
}

int IdxsynFile_GetOrigWordCount(IdxsynFile * pMe, long * idx)
{
	char *cWord = strdup(IdxsynFile_GetKey(pMe, *idx));
	const char *pWord;
	int count = 1;
	long idx1 = *idx;
	long idx2 = *idx;

	while (idx1 > 0) {
		pWord = IdxsynFile_GetKey(pMe, idx1 - 1);
		if (strcmp(pWord, cWord) != 0)
			break;
		count++;
		idx1--;
	}

	while (idx2 < pMe->wordcount - 1) {
		pWord = IdxsynFile_GetKey(pMe, idx2 + 1);
		if (strcmp(pWord, cWord) != 0)
			break;
		count++;
		idx2++;
	}
	*idx = idx1;
	free(cWord);
	return count;
}

void IdxsynFile_Release(IdxsynFile * pMe)
{
	if(NULL == pMe)
		return;

	if(NULL != pMe->oft_file.wordoffset)
	{
		free(pMe->oft_file.wordoffset);
		pMe->oft_file.wordoffset = NULL;
	}

	if(NULL != pMe->page_data)
	{
		free(pMe->page_data);
		pMe->page_data = NULL;
	}

	if(NULL != pMe->idxfile)
	{
		fclose(pMe->idxfile);
		pMe->idxfile = NULL;
	}

	free( pMe );
}

boolean IdxsynFile_New(IdxsynFile ** ppMe)
{
    IdxsynFile *pMe = 0; // Declare pointer to this extension.

    // Validate incoming parameters
    if(0 == ppMe)
    {
        return FALSE;
    }

    *ppMe = 0; //Initialize the module pointer to 0.

    // Allocate memory for size of class and function table:
    pMe = (IdxsynFile *)malloc( sizeof(IdxsynFile) );
	memset((void*)pMe, 0, sizeof(IdxsynFile));

    // If there wasn't enough memory left for this extension:
    if(0 == pMe) {
        return FALSE;   //Return "no memory" error.
    }

	pMe->idxfile = NULL;
	pMe->npages = 0;
	pMe->page_data = NULL;
	pMe->page_size = 0;
	pMe->oft_file.wordoffset = NULL;
	pMe->page.idx = -1;

    // Fill the pointer that will be output with this extension pointer:
    *ppMe = pMe; 

    return TRUE; 
}
