#include "DictEng.h"
#include "plugs/DictPlugs.h"
#include "utils/Distance.h"
#include "utils/Pattern.h"
typedef struct _CurrentIndex {
	long idx;
	long idx_suggest;
	long synidx;
	long synidx_suggest;
} CurrentIndex;

typedef struct _Fuzzystruct {
	char * pMatchWord;
	int iMatchWordDistance;
} Fuzzystruct;

static void DictEng_UnloadDicts(DictEng * pMe) {
	int i = 0;

	if (NULL == pMe->dictInfo)
		return;

	for (i = 0; i < pMe->dictCount; i++) {
		if (NULL != pMe->dictInfo[i].bookName)
			free(pMe->dictInfo[i].bookName);
		if (NULL != pMe->dictInfo[i].sameTypeSequence)
			free(pMe->dictInfo[i].sameTypeSequence);

		free(pMe->dictInfo[i].dictPath);
		free(pMe->dictInfo[i].dictName);

		if (NULL != pMe->dictInfo[i].piOffsetIndex)
			OffsetIndex_Release(pMe->dictInfo[i].piOffsetIndex);
		if (NULL != pMe->dictInfo[i].piIdxsynFile)
			IdxsynFile_Release(pMe->dictInfo[i].piIdxsynFile);
		if (NULL != pMe->dictInfo[i].piDictZipLib)
			DictZipLib_Release(pMe->dictInfo[i].piDictZipLib);
	}

	free(pMe->dictInfo);
	pMe->dictInfo = NULL;
	pMe->dictCount = 0;
}

boolean DictEng_LoadDicts(DictEng * pMe, char ** pPaths, char ** pNames,
		int * pTypes, int count) {
	int i = 0;
	struct stat stats;

	DictEng_UnloadDicts(pMe);

	pMe->dictInfo = (DictInfo *) malloc(sizeof(DictInfo) * count);
	if (NULL == pMe->dictInfo)
		return FALSE;

	pMe->dictCount = count;

	for (i = 0; i < count; i++) {
		pMe->dictInfo[i].type = pTypes[i];
		pMe->dictInfo[i].wordcount = 0;
		pMe->dictInfo[i].idxfilesize = 0;
		pMe->dictInfo[i].synwordcount = 0;
		pMe->dictInfo[i].bookName = NULL;
		pMe->dictInfo[i].sameTypeSequence = NULL;
		pMe->dictInfo[i].piOffsetIndex = NULL;
		pMe->dictInfo[i].piIdxsynFile = NULL;
		pMe->dictInfo[i].piDictZipLib = NULL;

		pMe->dictInfo[i].dictPath = strdup(pPaths[i]);
		pMe->dictInfo[i].dictName = strdup(pNames[i]);

		OffsetIndex_New(&pMe->dictInfo[i].piOffsetIndex);
		DictZipLib_New(&pMe->dictInfo[i].piDictZipLib);

		if (NULL != pMe->dictInfo[i].piOffsetIndex
				&& NULL != pMe->dictInfo[i].piDictZipLib) {
			char * path = NULL;
			int pathLen = strlen(pMe->dictInfo[i].dictPath)
					+ strlen(pMe->dictInfo[i].dictName) + 12;
			path = (char*) malloc(pathLen);

			memset(path, 0, pathLen);
			strcat(path, pMe->dictInfo[i].dictPath);
			strcat(path, pMe->dictInfo[i].dictName);
			strcat(path, ".ifo");
			InfoFile_Load(path, &pMe->dictInfo[i].wordcount,
					&pMe->dictInfo[i].idxfilesize,
					&pMe->dictInfo[i].synwordcount, &pMe->dictInfo[i].bookName,
					&pMe->dictInfo[i].version, &pMe->dictInfo[i].description,
					&pMe->dictInfo[i].sameTypeSequence);

			memset(path, 0, pathLen);
			strcat(path, pMe->dictInfo[i].dictPath);
			strcat(path, pMe->dictInfo[i].dictName);
			strcat(path, ".idx");
			OffsetIndex_Load(pMe->dictInfo[i].piOffsetIndex, path,
					pMe->dictInfo[i].wordcount, pMe->dictInfo[i].idxfilesize,
					FALSE);

			memset(path, 0, pathLen);
			strcat(path, pMe->dictInfo[i].dictPath);
			strcat(path, pMe->dictInfo[i].dictName);
			strcat(path, ".syn");
			if (0 == stat(path, &stats)) {
				IdxsynFile_New(&pMe->dictInfo[i].piIdxsynFile);
				IdxsynFile_Load(pMe->dictInfo[i].piIdxsynFile, path,
						pMe->dictInfo[i].synwordcount, stats.st_size, FALSE);
			}

			memset(path, 0, pathLen);
			strcat(path, pMe->dictInfo[i].dictPath);
			strcat(path, pMe->dictInfo[i].dictName);
			strcat(path, ".dict.dz");
			if (FALSE == DictZipLib_Open(pMe->dictInfo[i].piDictZipLib, path)) // Try plain text format.
					{
				memset(path, 0, pathLen);
				strcat(path, pMe->dictInfo[i].dictPath);
				strcat(path, pMe->dictInfo[i].dictName);
				strcat(path, ".dict");
				DictZipLib_Open(pMe->dictInfo[i].piDictZipLib, path);
			}

			free(path);
		}
	}

	return TRUE;
}

char * DictEng_GetSuggestWord(DictEng * pMe, char *sWord,
		CurrentIndex * pCurrentIndex) {
	const char *poCurrentWord = NULL;
	const char *word;
	int best = 0;
	int back = 0;
	int i = 0;

	for (i = 0; i < pMe->dictCount; i++) {
		if (!(pMe->dictInfo[i].type & DICT_TYPE_INDEX)) // If not DICT_TYPE_INDEX, ignore this dictionary.
			continue;

		if (pCurrentIndex[i].idx_suggest == INVALID_INDEX
				|| pCurrentIndex[i].idx_suggest == UNSET_INDEX)
			continue;

		if (poCurrentWord == NULL) {
			poCurrentWord = OffsetIndex_GetKey(pMe->dictInfo[i].piOffsetIndex,
					pCurrentIndex[i].idx_suggest);
			best = prefix_match(sWord, poCurrentWord);
		} else {
			word = OffsetIndex_GetKey(pMe->dictInfo[i].piOffsetIndex,
					pCurrentIndex[i].idx_suggest);
			back = prefix_match(sWord, word);
			if (back > best) {
				best = back;
				poCurrentWord = word;
			} else if (back == best) {
				int x = svn_strcmp(poCurrentWord, word);
				if (x > 0) {
					poCurrentWord = word;
				}
			}
		}
	}

	for (i = 0; i < pMe->dictCount; i++) {
		if (!(pMe->dictInfo[i].type & DICT_TYPE_INDEX)) // If not DICT_TYPE_INDEX, ignore this dictionary.
			continue;

		if (NULL == pMe->dictInfo[i].piIdxsynFile)
			continue;

		if (pCurrentIndex[i].synidx_suggest == INVALID_INDEX
				|| pCurrentIndex[i].synidx_suggest == UNSET_INDEX)
			continue;

		if (poCurrentWord == NULL) {
			poCurrentWord = IdxsynFile_GetKey(pMe->dictInfo[i].piIdxsynFile,
					pCurrentIndex[i].synidx_suggest);
			best = prefix_match(sWord, poCurrentWord);
		} else {
			word = IdxsynFile_GetKey(pMe->dictInfo[i].piIdxsynFile,
					pCurrentIndex[i].synidx_suggest);
			back = prefix_match(sWord, word);
			if (back > best) {
				best = back;
				poCurrentWord = word;
			} else if (back == best) {
				int x = svn_strcmp(poCurrentWord, word);
				if (x > 0) {
					poCurrentWord = word;
				}
			}
		}
	}

	return (char*) poCurrentWord;
}

const char *poGetCurrentWord(DictEng * pMe, CurrentIndex * pCurrentIndex) {
	int i = 0;
	const char *poCurrentWord = NULL;
	const char *word;

	for (i = 0; i < pMe->dictCount; i++) {
		if (!(pMe->dictInfo[i].type & DICT_TYPE_INDEX)) // If not DICT_TYPE_INDEX, ignore this dictionary.
			continue;

		if (pCurrentIndex[i].idx == INVALID_INDEX)
			continue;

		if (pCurrentIndex[i].idx >= pMe->dictInfo[i].piOffsetIndex->wordcount
				|| pCurrentIndex[i].idx < 0)
			continue;

		if (poCurrentWord == NULL) {
			poCurrentWord = OffsetIndex_GetKey(pMe->dictInfo[i].piOffsetIndex,
					pCurrentIndex[i].idx);
		} else {
			int x = 0;
			word = OffsetIndex_GetKey(pMe->dictInfo[i].piOffsetIndex,
					pCurrentIndex[i].idx);
			x = svn_strcmp(poCurrentWord, word);
			if (x > 0) {
				poCurrentWord = word;
			}
		}
	}

	for (i = 0; i < pMe->dictCount; i++) {
		if (!(pMe->dictInfo[i].type & DICT_TYPE_INDEX)) // If not DICT_TYPE_INDEX, ignore this dictionary.
			continue;

		if (NULL == pMe->dictInfo[i].piIdxsynFile)
			continue;

		if (pCurrentIndex[i].synidx == UNSET_INDEX
				|| pCurrentIndex[i].synidx == INVALID_INDEX)
			continue;

		if (pCurrentIndex[i].synidx >= pMe->dictInfo[i].piIdxsynFile->wordcount
				|| pCurrentIndex[i].synidx < 0)
			continue;

		if (poCurrentWord == NULL) {
			poCurrentWord = IdxsynFile_GetKey(pMe->dictInfo[i].piIdxsynFile,
					pCurrentIndex[i].synidx);
		} else {
			int x = 0;
			word = IdxsynFile_GetKey(pMe->dictInfo[i].piIdxsynFile,
					pCurrentIndex[i].synidx);
			x = svn_strcmp(poCurrentWord, word);
			if (x > 0) {
				poCurrentWord = word;
			}
		}
	}

	return poCurrentWord;
}

const char *poGetNextWord(DictEng * pMe, const char *sWord,
		CurrentIndex *pCurrentIndex) {
	// the input can be:
	// (sWord,pCurrentIndex),read word,write iNext to pCurrentIndex,and return next word. ;
	// (NULL,pCurrentIndex),read pCurrentIndex,write iNext to pCurrentIndex,and return next word. used by ListWords();
	const char *poCurrentWord = NULL;
	boolean isLib = FALSE;
	const char *word;
	int iCurrentDict = 0;
	int i = 0;

	for (i = 0; i < pMe->dictCount; i++) {
		if (!(pMe->dictInfo[i].type & DICT_TYPE_INDEX)) // If not DICT_TYPE_INDEX, ignore this dictionary.
			continue;

		if (sWord) {
			OffsetIndex_LookupSimilarWords(pMe->dictInfo[i].piOffsetIndex,
					sWord, &pCurrentIndex[i].idx,
					&pCurrentIndex[i].idx_suggest);
		}
		if (pCurrentIndex[i].idx == INVALID_INDEX)
			continue;

		if (pCurrentIndex[i].idx >= pMe->dictInfo[i].piOffsetIndex->wordcount
				|| pCurrentIndex[i].idx < 0)
			continue;

		if (poCurrentWord == NULL) {
			poCurrentWord = OffsetIndex_GetKey(pMe->dictInfo[i].piOffsetIndex,
					pCurrentIndex[i].idx);
			iCurrentDict = i;
			isLib = TRUE;
		} else {
			int x;
			word = OffsetIndex_GetKey(pMe->dictInfo[i].piOffsetIndex,
					pCurrentIndex[i].idx);
			x = svn_strcmp(poCurrentWord, word);
			if (x > 0) {
				poCurrentWord = word;
				iCurrentDict = i;
				isLib = TRUE;
			}
		}
	}

	for (i = 0; i < pMe->dictCount; i++) {
		if (!(pMe->dictInfo[i].type & DICT_TYPE_INDEX)) // If not DICT_TYPE_INDEX, ignore this dictionary.
			continue;

		if (NULL == pMe->dictInfo[i].piIdxsynFile)
			continue;

		if (sWord) {
			IdxsynFile_Lookup(pMe->dictInfo[i].piIdxsynFile, sWord,
					&pCurrentIndex[i].synidx, &pCurrentIndex[i].synidx_suggest);
		}

		if (pCurrentIndex[i].synidx == UNSET_INDEX
				|| pCurrentIndex[i].synidx == INVALID_INDEX)
			continue;

		if (pCurrentIndex[i].synidx >= pMe->dictInfo[i].piIdxsynFile->wordcount
				|| pCurrentIndex[i].synidx < 0)
			continue;

		if (poCurrentWord == NULL) {
			poCurrentWord = IdxsynFile_GetKey(pMe->dictInfo[i].piIdxsynFile,
					pCurrentIndex[i].synidx);
			iCurrentDict = i;
			isLib = FALSE;
		} else {
			int x;
			word = IdxsynFile_GetKey(pMe->dictInfo[i].piIdxsynFile,
					pCurrentIndex[i].synidx);
			x = svn_strcmp(poCurrentWord, word);
			if (x > 0) {
				poCurrentWord = word;
				iCurrentDict = i;
				isLib = FALSE;
			}
		}
	}

	if (poCurrentWord) {
		for (i = 0; i < pMe->dictCount; i++) {
			if (!(pMe->dictInfo[i].type & DICT_TYPE_INDEX)) // If not DICT_TYPE_INDEX, ignore this dictionary.
				continue;

			if (isLib && (i == iCurrentDict))
				continue;
			if (pCurrentIndex[i].idx == INVALID_INDEX)
				continue;
			if (pCurrentIndex[i].idx
					>= pMe->dictInfo[i].piOffsetIndex->wordcount
					|| pCurrentIndex[i].idx < 0)
				continue;

			word = OffsetIndex_GetKey(pMe->dictInfo[i].piOffsetIndex,
					pCurrentIndex[i].idx);

			if (strcmp(poCurrentWord, word) == 0) {
				OffsetIndex_GetWordNext(pMe->dictInfo[i].piOffsetIndex,
						&pCurrentIndex[i].idx);
			}
		}

		for (i = 0; i < pMe->dictCount; i++) {
			if (!(pMe->dictInfo[i].type & DICT_TYPE_INDEX)) // If not DICT_TYPE_INDEX, ignore this dictionary.
				continue;

			if (NULL == pMe->dictInfo[i].piIdxsynFile)
				continue;

			if ((!isLib) && (i == iCurrentDict))
				continue;
			if (pCurrentIndex[i].synidx == UNSET_INDEX
					|| pCurrentIndex[i].synidx == INVALID_INDEX)
				continue;
			if (pCurrentIndex[i].synidx
					>= pMe->dictInfo[i].piIdxsynFile->wordcount
					|| pCurrentIndex[i].synidx < 0)
				continue;

			word = IdxsynFile_GetKey(pMe->dictInfo[i].piIdxsynFile,
					pCurrentIndex[i].synidx);

			if (strcmp(poCurrentWord, word) == 0) {
				IdxsynFile_GetWordNext(pMe->dictInfo[i].piIdxsynFile,
						&pCurrentIndex[i].synidx);
			}
		}

		//GetWordNext will change poCurrentWord's content, so do it at the last.
		if (isLib) {
			OffsetIndex_GetWordNext(pMe->dictInfo[iCurrentDict].piOffsetIndex,
					&pCurrentIndex[iCurrentDict].idx);
		} else {
			IdxsynFile_GetWordNext(pMe->dictInfo[iCurrentDict].piIdxsynFile,
					&pCurrentIndex[iCurrentDict].synidx);
		}

		poCurrentWord = poGetCurrentWord(pMe, pCurrentIndex);
	}

	return poCurrentWord;
}

boolean DictEng_WordIndexs(DictEng * pMe, char * sWord,
		CurrentIndex * pCurrentIndex) {
	int i = 0;
	boolean bFound = FALSE;

	for (i = 0; i < pMe->dictCount; i++) {
		boolean bLookupWord = FALSE, bLookupSynonymWord = FALSE;

		if (!(pMe->dictInfo[i].type & DICT_TYPE_INDEX)) // If not DICT_TYPE_INDEX, ignore this dictionary.
			continue;

		pCurrentIndex[i].idx = 0;
		pCurrentIndex[i].idx_suggest = 0;
		pCurrentIndex[i].synidx = UNSET_INDEX;
		pCurrentIndex[i].synidx_suggest = UNSET_INDEX;

		bLookupWord = OffsetIndex_LookupSimilarWords(
				pMe->dictInfo[i].piOffsetIndex, sWord, &pCurrentIndex[i].idx,
				&pCurrentIndex[i].idx_suggest);
		if (NULL != pMe->dictInfo[i].piIdxsynFile) {
			bLookupSynonymWord = IdxsynFile_Lookup(
					pMe->dictInfo[i].piIdxsynFile, sWord,
					&pCurrentIndex[i].synidx, &pCurrentIndex[i].synidx_suggest);
		}

		if (TRUE == bLookupWord || TRUE == bLookupSynonymWord) {
			bFound = TRUE;
		}
	}

	return bFound;
}

int DictEng_ListWords(DictEng * pMe, char * sWord, char *** pWordsList) {
	int i = 0;
	boolean bFound = FALSE;
	CurrentIndex * pCurrentIndex = NULL;
	int wordcount = 0;
	const char * poCurrentWord = NULL;

	pCurrentIndex = (CurrentIndex*) malloc(
			pMe->dictCount * sizeof(CurrentIndex));
	if (NULL == pCurrentIndex)
		return 0;

	(*pWordsList) = (char**) malloc(sizeof(char*) * MAX_MATCH_ITEM);

	bFound = DictEng_WordIndexs(pMe, sWord, pCurrentIndex);

	if (FALSE == bFound) // If not found, we should show suggest words in the list.
			{
		const char *sug_word = DictEng_GetSuggestWord(pMe, sWord,
				pCurrentIndex);
		if (sug_word) {
			bFound = DictEng_WordIndexs(pMe, (char*) sug_word, pCurrentIndex);
		}
	}
	poCurrentWord = poGetCurrentWord(pMe, pCurrentIndex);
	if (poCurrentWord) {
		(*pWordsList)[wordcount] = strdup(poCurrentWord);
		wordcount++;

		while (wordcount < MAX_MATCH_ITEM
				&& (poCurrentWord = poGetNextWord(pMe, NULL, pCurrentIndex))) {
			(*pWordsList)[wordcount] = strdup(poCurrentWord);
			wordcount++;
		}
	}

	free(pCurrentIndex);

	return wordcount;
}

#define MAX_FULLTEXT_WORD_COUNT		5

void DictEng_FreeSearchInfo(DictEng * pMe, SearchInfo * pSearchInfo) {
	int i = 0, k = 0;

	for (i = 0; i < pSearchInfo->dictCount; i++) {
		if (pSearchInfo->searchData[i].wordcount <= 0)
			continue;

		for (k = 0; k < pSearchInfo->searchData[i].wordcount; k++) {
			free(pSearchInfo->searchData[i].pKeyWord[k]);
		}

		free(pSearchInfo->searchData[i].pKeyWord);
	}

	free(pSearchInfo->searchData);
}

void DictEng_SetLookupCancel(DictEng * pMe, boolean bCancel) {
	pMe->cancel = bCancel;
}

boolean DictEng_LookupData(DictEng * pMe, const char *sWord,
		SearchInfo * pSearchInfo, PFN_PROGRESS pfn) {
	int i = 0, k = 0, m = 0, cnt = 0;
	char * SearchWords[MAX_FULLTEXT_WORD_COUNT] = { NULL };
	char SearchWord[MAX_INDEX_KEY_SIZE] = { 0 };
	const char *p = sWord;
	char *p1 = SearchWord;
	uint32 max_size = 0;
	char *origin_data = NULL;
	long total_count = 0, search_count = 0;

	while (*p) {
		if (*p == '\\') {
			p++;
			if (i < MAX_INDEX_KEY_SIZE) {
				switch (*p) {
				case ' ':
					*p1 = ' ';
					break;
				case '\\':
					*p1 = '\\';
					break;
				case 't':
					*p1 = '\t';
					break;
				case 'n':
					*p1 = '\n';
					break;
				default:
					*p1 = *p;
				}
				p1++;
			}
		} else if (*p == ' ') {
			if (strlen(SearchWord) > 0 && cnt < MAX_FULLTEXT_WORD_COUNT) {
				SearchWords[cnt] = strdup(SearchWord);
				memset(SearchWord, 0, MAX_INDEX_KEY_SIZE);
				p1 = SearchWord;
				i = 0;
				cnt++;
			}
		} else {
			if (i < MAX_INDEX_KEY_SIZE) {
				*p1 = *p;
				p1++;
			}
		}

		i++;
		p++;
	}

	if (strlen(SearchWord) > 0 && cnt < MAX_FULLTEXT_WORD_COUNT) {
		SearchWords[cnt] = strdup(SearchWord);
		cnt++;
	}

	if (cnt == 0)
		return FALSE;

	i = 0;

	pSearchInfo->dictCount = 0;
	pSearchInfo->searchData = (SearchData*) malloc(
			sizeof(SearchData) * pMe->dictCount);

	for (k = 0; k < pMe->dictCount; k++) {
		if (!(pMe->dictInfo[k].type & DICT_TYPE_INDEX)) // If not DICT_TYPE_INDEX, ignore this dictionary.
			continue;

		total_count += pMe->dictInfo[k].piOffsetIndex->wordcount;
	}

	for (k = 0; k < pMe->dictCount; k++) {
		const unsigned long iwords = pMe->dictInfo[k].piOffsetIndex->wordcount;
		const char *key;
		uint32 offset = 0, size = 0;
		unsigned long j = 0;

		if (!(pMe->dictInfo[k].type & DICT_TYPE_INDEX)) // If not DICT_TYPE_INDEX, ignore this dictionary.
			continue;

		if (pMe->cancel)
			break;

		pSearchInfo->searchData[m].wordcount = 0;
		pSearchInfo->searchData[m].bookName = NULL;
		pSearchInfo->searchData[m].pKeyWord = (char**) malloc(
				sizeof(char*) * (MAX_MATCH_ITEM_PER_DIC));

		for (j = 0; j < MAX_MATCH_ITEM_PER_DIC; j++) {
			pSearchInfo->searchData[m].pKeyWord[j] = NULL;
		}

		for (j = 0; j < iwords; ++j) {
			boolean WordFind[MAX_FULLTEXT_WORD_COUNT] = { FALSE };
			int nfound = 0;
			uint32 sec_size = 0;

			if (pMe->cancel)
				break;

			// Notify current progress.
			if (pfn) {
				if (search_count % 10000 == 0) {
					pfn(((double) search_count / (double) total_count));
				}
				search_count++;
			}

			if (pSearchInfo->searchData[m].wordcount >= MAX_MATCH_ITEM_PER_DIC) {
				search_count += iwords - j - 1;

				// Notify current progress.
				if (pfn) {
					pfn(((double) search_count / (double) total_count));
				}
				break;
			}

			OffsetIndex_GetKeyNData(pMe->dictInfo[k].piOffsetIndex, j,
					(const char**) &key, (uint32 *) &offset, (uint32 *) &size);
			if (size > max_size) {
				if (NULL != origin_data)
					free(origin_data);
				origin_data = (char *) malloc(size + 1);
				max_size = size + 1;
			}

			DictZipLib_Read(pMe->dictInfo[k].piDictZipLib, origin_data, offset,
					size);
			origin_data[size] = '\0';

			p = origin_data;

			while ((uint32) (p - origin_data) < size) {

				for (i = 0; i < cnt; i++) {
					if (!WordFind[i] && strstr(p, SearchWords[i])) {
						WordFind[i] = TRUE;
						++nfound;
					}
				}

				if (nfound == cnt) {
					pSearchInfo->searchData[m].pKeyWord[pSearchInfo->searchData[m].wordcount] =
							strdup(key);
					pSearchInfo->searchData[m].wordcount++;
					if (NULL == pSearchInfo->searchData[m].bookName) {
						pSearchInfo->searchData[m].bookName =
								pMe->dictInfo[k].bookName;
						pSearchInfo->dictCount++;
					}
					break;
				}

				sec_size = strlen(p) + 1;
				p += sec_size;
			} // End while ((uint32)(p - origin_data) < size) {
		} // End for (j = 0; j < iwords; ++j) {

		if (m == pSearchInfo->dictCount) // Not found any words in this dictionary.
			free(pSearchInfo->searchData[m].pKeyWord);
		else
			m = pSearchInfo->dictCount;

	} // End for (k = 0; k < pMe->dictCount; k++) {

	if (NULL != origin_data)
		free(origin_data);

	for (i = 0; i < cnt; i++) {
		free(SearchWords[i]);
	}

	for (i = 0; i < pMe->dictCount; ++i) {
		if (!(pMe->dictInfo[i].type & DICT_TYPE_INDEX)) // If not DICT_TYPE_INDEX, ignore this dictionary.
			continue;

		if (pSearchInfo->searchData[i].wordcount > 0)
			break;
	}

	return i != pMe->dictCount;
}

int DictEng_LookupWithRule(DictEng * pMe, const char *word, char *** pWordsList,
		PFN_PROGRESS pfn) {
	long aiIndex[MAX_MATCH_ITEM_PER_DIC + 1];
	int iMatchCount = 0;
	const char * sMatchWord;
	boolean bAlreadyInList;
	int i = 0, j = 0, k = 0;
	char ** ppMatchWord = NULL;
	int listsize = sizeof(char*) * (MAX_MATCH_ITEM_PER_DIC * 2)
			* pMe->dictCount;
	long total_count = 0, search_count = 0;

	PatternSpec *pspec = pattern_spec_init(word);

	(*pWordsList) = (char **) malloc(listsize);
	ppMatchWord = (*pWordsList);
	memset(ppMatchWord, 0, listsize);

	for (i = 0; i < pMe->dictCount; i++) {
		if (!(pMe->dictInfo[i].type & DICT_TYPE_INDEX)) // If not DICT_TYPE_INDEX, ignore this dictionary.
			continue;

		total_count += pMe->dictInfo[i].piOffsetIndex->wordcount;
		if (pMe->dictInfo[i].piIdxsynFile)
			total_count += pMe->dictInfo[i].piIdxsynFile->wordcount;
	}

	for (k = 0; k < pMe->dictCount; k++) {
		int iIndexCount = 0;
		long m = 0;

		if (!(pMe->dictInfo[k].type & DICT_TYPE_INDEX)) // If not DICT_TYPE_INDEX, ignore this dictionary.
			continue;

		if (pMe->cancel)
			break;

		for (m = 0;
				m < pMe->dictInfo[k].piOffsetIndex->wordcount
						&& iIndexCount < MAX_MATCH_ITEM_PER_DIC; m++) {
			if (pMe->cancel)
				break;

			// Notify current progress.
			if (pfn) {
				if (search_count % 10000 == 0) {
					pfn(((double) search_count / (double) total_count));
				}
				search_count++;
			}

			if (pattern_match_string(pspec,
					OffsetIndex_GetKey(pMe->dictInfo[k].piOffsetIndex, m))) // Need to deal with same word in index? But this will slow down processing in most case.
				aiIndex[iIndexCount++] = m;

			if (iIndexCount >= MAX_MATCH_ITEM_PER_DIC) {
				search_count += pMe->dictInfo[k].piOffsetIndex->wordcount - m
						- 1;

				// Notify current progress.
				if (pfn) {
					pfn(((double) search_count / (double) total_count));
				}
			}
		}

		aiIndex[iIndexCount] = -1; // -1 is the end.

		if (iIndexCount > 0) {
			for (i = 0; aiIndex[i] != -1; i++) {
				sMatchWord = OffsetIndex_GetKey(pMe->dictInfo[k].piOffsetIndex,
						aiIndex[i]);
				bAlreadyInList = FALSE;
				for (j = 0; j < iMatchCount; j++) {
					if (strcmp(ppMatchWord[j], sMatchWord) == 0) { //already in list
						bAlreadyInList = TRUE;
						break;
					}
				}

				if (!bAlreadyInList)
					ppMatchWord[iMatchCount++] = strdup(sMatchWord);
			}
		}

		if (NULL == pMe->dictInfo[k].piIdxsynFile)
			continue;

		iIndexCount = 0;
		m = 0;

		for (m = 0;
				m < pMe->dictInfo[k].piIdxsynFile->wordcount
						&& iIndexCount < MAX_MATCH_ITEM_PER_DIC; m++) {
			if (pMe->cancel)
				break;

			// Notify current progress.
			if (pfn) {
				if (search_count % 10000 == 0) {
					pfn(((double) search_count / (double) total_count));
				}
				search_count++;
			}

			if (pattern_match_string(pspec,
					IdxsynFile_GetKey(pMe->dictInfo[k].piIdxsynFile, m))) // Need to deal with same word in index? But this will slow down processing in most case.
				aiIndex[iIndexCount++] = m;

			if (iIndexCount >= MAX_MATCH_ITEM_PER_DIC) {
				search_count += pMe->dictInfo[k].piIdxsynFile->wordcount - m
						- 1;

				// Notify current progress.
				if (pfn) {
					pfn(((double) search_count / (double) total_count));
				}
			}
		}

		aiIndex[iIndexCount] = -1; // -1 is the end.
		if (iIndexCount > 0) {
			for (i = 0; aiIndex[i] != -1; i++) {
				sMatchWord = IdxsynFile_GetKey(pMe->dictInfo[k].piIdxsynFile,
						aiIndex[i]);
				bAlreadyInList = FALSE;
				for (j = 0; j < iMatchCount; j++) {
					if (strcmp(ppMatchWord[j], sMatchWord) == 0) { //already in list
						bAlreadyInList = TRUE;
						break;
					}
				}

				if (!bAlreadyInList)
					ppMatchWord[iMatchCount++] = strdup(sMatchWord);
			}
		}
	}

	pattern_spec_free(pspec);
	if (iMatchCount > 0) {
		QSort(ppMatchWord, iMatchCount, sizeof(ppMatchWord[0]), NULL);
	}

	return iMatchCount;
}

int DictEng_LookupWithFuzzy(DictEng * pMe, const char *sWord,
		char *** pWordsList, PFN_PROGRESS pfn) {
	int iDistance;
	int i = 0;
	boolean Found = FALSE;
	int iMaxDistance = MAX_FUZZY_DISTANCE;
	int wordslistsize = MAX_FUZZY_MATCH_ITEM;
	Fuzzystruct * oFuzzystruct;
	long iCheckWordLen;
	const char *sCheck;
	wchar *wstr1, *wstr2;
	long wstr2len, wstr1len;
	int wstrsize = 0;
	int IsSynFile = 0;
	int wordcount = 0;
	int maxSizeOfWStr = MAX_INDEX_KEY_SIZE * 3 + 1;
	int * dBuf = NULL;
	long total_count = 0, search_count = 0;

	if (strlen(sWord) <= 0) {
		return 0;
	}

	oFuzzystruct = (Fuzzystruct *) malloc(sizeof(Fuzzystruct) * wordslistsize);
	(*pWordsList) = malloc(sizeof(char*) * MAX_FUZZY_MATCH_ITEM);

	for (i = 0; i < wordslistsize; i++) {
		oFuzzystruct[i].pMatchWord = NULL;
		oFuzzystruct[i].iMatchWordDistance = iMaxDistance;
	}

	wstr2len = UTF8StrLen(sWord, -1);
	wstrsize = wstr2len * 3 + 1;
	wstr2 = (wchar*) malloc(wstrsize);
	UTF8ToWStr((const byte*) sWord, strlen(sWord), wstr2, wstrsize);
	wstr2[wstr2len] = 0;
	WStrLower(wstr2);

	// Remove the letter from MAX_INDEX_KEY_SIZE,  the max length of the word should be MAX_INDEX_KEY_SIZE.
	if (wstr2len > MAX_INDEX_KEY_SIZE) {
		wstr2len = MAX_INDEX_KEY_SIZE;
		wstr2[MAX_INDEX_KEY_SIZE] = '\0';
	}

	// malloc heap for wstr1. Only malloc once for the performance.
	wstr1 = (wchar*) malloc(maxSizeOfWStr);

	// malloc heap for CalEditDistance(). Only malloc once for the performance.
	dBuf = (int*) malloc(
			sizeof(int)
					* (MAX_INDEX_KEY_SIZE * MAX_INDEX_KEY_SIZE * 2
							+ MAX_INDEX_KEY_SIZE + 1));

	for (i = 0; i < pMe->dictCount; i++) {
		if (!(pMe->dictInfo[i].type & DICT_TYPE_INDEX)) // If not DICT_TYPE_INDEX, ignore this dictionary.
			continue;

		total_count += pMe->dictInfo[i].piOffsetIndex->wordcount;
		if (pMe->dictInfo[i].piIdxsynFile)
			total_count += pMe->dictInfo[i].piIdxsynFile->wordcount;
	}

	for (i = 0; i < pMe->dictCount; i++) {
		if (!(pMe->dictInfo[i].type & DICT_TYPE_INDEX)) // If not DICT_TYPE_INDEX, ignore this dictionary.
			continue;

		if (pMe->cancel)
			break;

		for (IsSynFile = 0; IsSynFile < 2; IsSynFile++) {
			if (IsSynFile == 1) {
				if (NULL == pMe->dictInfo[i].piIdxsynFile)
					break;
			}

			if (TRUE) {
				long iwords;
				long index = 0;
				if (IsSynFile == 0)
					iwords = pMe->dictInfo[i].piOffsetIndex->wordcount;
				else
					iwords = pMe->dictInfo[i].piIdxsynFile->wordcount;

				for (index = 0; index < iwords; index++) {

					if (pMe->cancel)
						break;

					// Notify current progress.
					if (pfn) {
						if (search_count % 10000 == 0) {
							pfn(((double) search_count / (double) total_count));
						}
						search_count++;
					}

					// Need to deal with same word in index? But this will slow down processing in most case.
					if (IsSynFile == 0)
						sCheck = OffsetIndex_GetKey(
								pMe->dictInfo[i].piOffsetIndex, index);
					else
						sCheck = IdxsynFile_GetKey(
								pMe->dictInfo[i].piIdxsynFile, index);

					// tolower and skip too long or too short words
					iCheckWordLen = UTF8StrLen(sCheck, -1);
					if (iCheckWordLen - wstr2len >= iMaxDistance
							|| wstr2len - iCheckWordLen >= iMaxDistance)
						continue;

					wstr1len = UTF8StrLen(sCheck, -1);
					wstrsize = wstr1len * 3 + 1;
					if (wstrsize > maxSizeOfWStr)
						wstrsize = maxSizeOfWStr;
					UTF8ToWStr((const byte*) sCheck, strlen(sCheck), wstr1,
							wstrsize);
					if (iCheckWordLen > wstr2len)
						wstr1[wstr2len] = '\0';
					else
						wstr1[iCheckWordLen] = '\0';
					WStrLower(wstr1);

					iDistance = CalEditDistance(wstr1, wstr2, iMaxDistance,
							dBuf);

					if (iDistance < iMaxDistance && iDistance < wstr2len) {
						boolean bAlreadyInList = FALSE;
						int iMaxDistanceAt = 0;
						int j = 0;

						// when wstr2len=1,2 we need less fuzzy.
						Found = TRUE;

						for (j = 0; j < wordslistsize; j++) {
							if (oFuzzystruct[j].pMatchWord
									&& strcmp(oFuzzystruct[j].pMatchWord,
											sCheck) == 0) { //already in list
								bAlreadyInList = TRUE;
								break;
							}
							//find the position,it will certainly be found (include the first time) as iMaxDistance is set by last time.
							if (oFuzzystruct[j].iMatchWordDistance
									== iMaxDistance) {
								iMaxDistanceAt = j;
							}
						}
						if (!bAlreadyInList) {
							if (oFuzzystruct[iMaxDistanceAt].pMatchWord)
								free(oFuzzystruct[iMaxDistanceAt].pMatchWord);
							oFuzzystruct[iMaxDistanceAt].pMatchWord = strdup(
									sCheck);
							oFuzzystruct[iMaxDistanceAt].iMatchWordDistance =
									iDistance;
							// calc new iMaxDistance
							iMaxDistance = iDistance;
							for (j = 0; j < wordslistsize; j++) {
								if (oFuzzystruct[j].iMatchWordDistance
										> iMaxDistance)
									iMaxDistance =
											oFuzzystruct[j].iMatchWordDistance;
							} // calc new iMaxDistance
						} // add to list
					} // find one
				} // each word
			} // ok for search
		} // IsSynFile
	} // each lib

	free(wstr2);
	free(wstr1);
	free(dBuf);

	for (i = 0; i < wordslistsize; ++i) {
		if (NULL != oFuzzystruct[i].pMatchWord) {
			(*pWordsList)[wordcount] = oFuzzystruct[i].pMatchWord;
			wordcount++;
		}
	}

	free(oFuzzystruct);
	if (wordcount > 0) {
		QSort((*pWordsList), wordcount, sizeof((*pWordsList)[0]), NULL);
	}
	return wordcount;
}

// All the data in wordInfo need to be free by caller.
// pWordInfo->wordData, pWordInfo->wordData[i].pKeyWord, pWordInfo->wordData[i].pWordData,
// pWordInfo->wordData[i].pKeyWord[k], pWordInfo->wordData[i].pWordData[k] need to be freed.
// Max (i = pWordInfo->dictCount - 1); max (k = pWordInfo->wordData[i].wordcount).
// This function for these types: DICT_TYPE_INDEX, DICT_TYPE_CAPTURE and DICT_TYPE_MEMORIZE.
boolean DictEng_Lookup(DictEng * pMe, char * sWord, WordInfo * pWordInfo,
		int type) {
	int i = 0;
	boolean bFound = FALSE;

	pWordInfo->wordData = (WordData*) malloc(sizeof(WordData) * pMe->dictCount);
	pWordInfo->dictCount = 0;
	for (i = 0; i < pMe->dictCount; i++) {
		boolean bLookupWord = FALSE, bLookupSynonymWord = FALSE;
		long idx = 0, idx_suggest = 0, synidx = UNSET_INDEX, synidx_suggest =
				UNSET_INDEX;

		if (!(pMe->dictInfo[i].type & type)) // If not the correct type dictionary, ignore this dictionary.
			continue;
		pWordInfo->wordData[pWordInfo->dictCount].wordcount = 0;
		bLookupWord = OffsetIndex_LookupSimilarWords(
				pMe->dictInfo[i].piOffsetIndex, sWord, &idx, &idx_suggest);
		if (NULL != pMe->dictInfo[i].piIdxsynFile) {
			bLookupSynonymWord = IdxsynFile_Lookup(
					pMe->dictInfo[i].piIdxsynFile, sWord, &synidx,
					&synidx_suggest);
		}
		if (TRUE == bLookupWord || TRUE == bLookupSynonymWord) {
			int count = 0, syncount = 0;

			if (bLookupWord) {
				count = OffsetIndex_GetOrigWordCount(
						pMe->dictInfo[i].piOffsetIndex, &idx);
			}
			if (bLookupSynonymWord) {
				syncount = IdxsynFile_GetOrigWordCount(
						pMe->dictInfo[i].piIdxsynFile, &synidx);
			}
			if ((count + syncount) > 0) {
				int wordindex = 0;
				int k = 0, l = 0;
				char *key = NULL;
				int * pOffset = NULL;
				int * pSize = NULL;
				bFound = TRUE;

				pWordInfo->wordData[pWordInfo->dictCount].dictID = i;
				pWordInfo->wordData[pWordInfo->dictCount].bookName =
						pMe->dictInfo[i].bookName;
				pWordInfo->wordData[pWordInfo->dictCount].sameTypeSequence =
						pMe->dictInfo[i].sameTypeSequence;

				pWordInfo->wordData[pWordInfo->dictCount].pKeyWord =
						(char**) malloc(sizeof(char*) * (count + syncount));
				pWordInfo->wordData[pWordInfo->dictCount].pWordData =
						(char**) malloc(sizeof(char*) * (count + syncount));

				pOffset = (int*) malloc(sizeof(int) * (count + syncount));
				pSize = (int*) malloc(sizeof(int) * (count + syncount));

				for (k = 0; k < count; k++) { // Index file.
					OffsetIndex_GetKeyNData(pMe->dictInfo[i].piOffsetIndex,
							idx + k, (const char**) &key,
							(uint32 *) &pOffset[k], (uint32 *) &pSize[k]);
					pWordInfo->wordData[pWordInfo->dictCount].pKeyWord[k] =
							strdup(key);
				}

				for (l = 0; l < syncount; l++) // Syn file.
						{
					IdxsynFile_GetKeyNData(pMe->dictInfo[i].piIdxsynFile,
							synidx + l, (const char**) &key,
							(uint32 *) &wordindex);

					if (bLookupWord) {
						if (wordindex >= idx && wordindex < idx + count) {
							syncount--;
							continue;
						}
					}

					// Get offset and size from Index file by wordindex come from syn file.
					OffsetIndex_GetKeyNData(pMe->dictInfo[i].piOffsetIndex,
							wordindex, (const char**) &key,
							(uint32 *) &pOffset[k], (uint32 *) &pSize[k]);
					pWordInfo->wordData[pWordInfo->dictCount].pKeyWord[k] =
							strdup(key);
					k++;
				}

				pWordInfo->wordData[pWordInfo->dictCount].wordcount = count
						+ syncount;

				for (k = 0; k < count + syncount; k++) { // Index file.
					char * pWordData = (char*) malloc(pSize[k] + 1);
					DictZipLib_Read(pMe->dictInfo[i].piDictZipLib, pWordData,
							pOffset[k], pSize[k]);
					pWordData[pSize[k]] = '\0';

					pWordInfo->wordData[pWordInfo->dictCount].pWordData[k] =
							DictPlugs_ParseData(pWordData, pSize[k] + 1,
									pMe->dictInfo[i].sameTypeSequence);
				}

				free(pOffset);
				free(pSize);
				pWordInfo->dictCount++;
			} // End if((count + syncount) > 0)
		} // End if(TRUE == bLookupWord || TRUE == bLookupSynonymWord)
	} // End for (i = 0; i < pMe->dictCount; i++)

	return bFound;
}

void DictEng_FreeWordInfo(DictEng * pMe, WordInfo * pWordInfo) {
	int i = 0, k = 0;

	for (i = 0; i < pWordInfo->dictCount; i++) {
		if (pWordInfo->wordData[i].wordcount <= 0)
			continue;

		for (k = 0; k < pWordInfo->wordData[i].wordcount; k++) {
			free(pWordInfo->wordData[i].pKeyWord[k]);
			free(pWordInfo->wordData[i].pWordData[k]);
		}

		free(pWordInfo->wordData[i].pKeyWord);
		free(pWordInfo->wordData[i].pWordData);
	}
	free(pWordInfo->wordData);
}

int DictEng_DictCount(DictEng * pMe) {
	return pMe->dictCount;
}

void DictEng_Release(DictEng * pMe) {
	DictEng_UnloadDicts(pMe);
	free(pMe);
}

boolean DictEng_New(DictEng ** ppMe) {
	DictEng *pMe = 0; // Declare pointer to this extension.

	// Validate incoming parameters
	if (0 == ppMe) {
		return FALSE;
	}

	*ppMe = 0; //Initialize the module pointer to 0.

	// Allocate memory for size of class and function table:
	pMe = (DictEng *) malloc(sizeof(DictEng));
	memset((void*) pMe, 0, sizeof(DictEng));

	// If there wasn't enough memory left for this extension:
	if (0 == pMe) {
		return FALSE; //Return "no memory" error.
	}

	pMe->dictInfo = NULL;

	// Fill the pointer that will be output with this extension pointer:
	*ppMe = pMe;

	return TRUE;
}

