#ifndef __DICT_ENG_H__
#define __DICT_ENG_H__

#include "utils/DictDef.h"
#include "DictZipLib.h"
#include "OffsetIndex.h"
#include "IdxsynFile.h"
#include "InfoFile.h"

#define DICT_TYPE_INDEX 		0x0001
//#define DICT_TYPE_CAPTURE 		0x0010
//#define DICT_TYPE_MEMORIZE 		0x0100

#define MAX_MATCH_ITEM 			30 		// how many words show in the list.
#define MAX_FUZZY_MATCH_ITEM	100		// how many fuzzy match words show in the list.
#define MAX_MATCH_ITEM_PER_DIC  100
#define MAX_FUZZY_DISTANCE		3 		// at most (MAX_FUZZY_DISTANCE - 1) differences allowed when find similar words.

typedef struct _SearchData {
	char ** pKeyWord;
	char * bookName;
	int wordcount;
} SearchData;

typedef struct _SearchInfo {
	SearchData * searchData; // One dict one searchData.
	int dictCount;
} SearchInfo;

typedef struct _WordData {
	char ** pKeyWord;
	char ** pWordData;
	char * sameTypeSequence;
	char * bookName;
	int wordcount;
	int dictID; // from 0 to (DictEng->dictCount - 1)
} WordData;

typedef struct _WordInfo {
	WordData * wordData; // One dict one dataInfo.
	int dictCount;
} WordInfo;

typedef struct _DictInfo {
	int type; // Get word/Search word/Memorize word.
	long wordcount;
	long idxfilesize;
	long synwordcount;
	char * sameTypeSequence;
	char * bookName;
	char * dictPath;
	char * dictName;
	char * version;
	char * description;
	OffsetIndex * piOffsetIndex;
	IdxsynFile * piIdxsynFile;
	DictZipLib * piDictZipLib;
} DictInfo;

typedef struct _DictEng {
	DictInfo * dictInfo;
	int dictCount;
	boolean cancel;
} DictEng;

#ifdef __cplusplus
extern "C" {
#endif

typedef void (*PFN_PROGRESS)(double progress);

void DictEng_SetLookupCancel(DictEng * pMe, boolean bCancel);
void DictEng_FreeWordInfo(DictEng * pMe, WordInfo * pWordInfo);
void DictEng_FreeSearchInfo(DictEng * pMe, SearchInfo * pSearchInfo);
boolean DictEng_New(DictEng ** ppiDictEng);
boolean DictEng_LoadDicts(DictEng * pMe, char ** pPaths, char ** pNames,
		int * pTypes, int count);
boolean DictEng_Lookup(DictEng * pMe, char * sWord, WordInfo * pWordInfo,
		int type);
boolean DictEng_LookupData(DictEng * pMe, const char *sWord,
		SearchInfo * pSearchInfo, PFN_PROGRESS pfn);
int DictEng_LookupWithRule(DictEng * pMe, const char *word, char *** pWordsList,
		PFN_PROGRESS pfn);
int DictEng_LookupWithFuzzy(DictEng * pMe, const char *sWord,
		char *** pWordsList, PFN_PROGRESS pfn);
int DictEng_ListWords(DictEng * pMe, char * sWord, char *** pWordsList);
int DictEng_DictCount(DictEng * pMe);
void DictEng_Release(DictEng * pMe);

#ifdef __cplusplus
}
#endif /* #ifdef __cplusplus */

#endif//!__DICT_ZIP_LIB_H__
