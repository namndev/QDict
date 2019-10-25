#include <string.h>
#include <jni.h>
#include <errno.h>
#include<sys/stat.h>
#include "DictEng.h"
#include "InfoFile.h"
#include <android/log.h>
#define HTML_KEYWORD_B		"<span style='color:#TOBEREPLACE;'><b>"
#define HTML_KEYWORD_E		"</b></span><br>"
#define HTML_NEW_LINE		"<br><hr>"
#define HTML_BOOKNAME_B		"["
#define HTML_BOOKNAME_E		"]"

#define LOGE(TAG,...) __android_log_print(ANDROID_LOG_ERROR  , "QDictEng.c",__VA_ARGS__);
typedef enum {
	LIST_WORDS_NORMAL, LIST_WORDS_FUZZY, LIST_WORDS_PATTERN, LIST_WORDS_FULLTEXT,
} LIST_WORDS_TYPE;

#define KNOW_TYPE_DONTKNOW	0
#define KNOW_TYPE_ALMOST	1
#define KNOW_TYPE_KNEW		2

static DictEng * g_pDictEng = NULL;

static JNIEnv * g_env = NULL;

static void LookupProgressCB(double progress) {
	int i = progress * 100;
	jclass clazz = NULL;

	if (NULL == g_env)
		return;

	clazz = (*g_env)->FindClass(g_env, "com/annie/dictionary/QDictEng");

	(*g_env)->CallStaticVoidMethod(g_env, clazz,
			(*g_env)->GetStaticMethodID(g_env, clazz, "lookupProgressCB",
					"(I)V"), i);
}

/* '/'   FUZZY
 * '* ?' PATTERN
 * ':'   FULLTEXT
 */
jobjectArray LookupWords(JNIEnv* env, jobject thiz, jstring word,
		LIST_WORDS_TYPE listType) {
	jclass objClass;
	jobjectArray objArray = NULL;
	jstring jStrData;
	int wordcount = 0;
	char ** pWordsList = NULL;
	int i = 0;
	char * wordUTF = NULL;

	if (NULL == g_pDictEng)
		return NULL;

	if (DictEng_DictCount(g_pDictEng) <= 0)
		return objArray;

	wordUTF = (char*) (*env)->GetStringUTFChars(env, word, NULL);

	DictEng_SetLookupCancel(g_pDictEng, FALSE);

	g_env = env;

	if (LIST_WORDS_NORMAL == listType) {
		wordcount = DictEng_ListWords(g_pDictEng, wordUTF, &pWordsList);
	} else if (LIST_WORDS_FUZZY == listType) {
		wordcount = DictEng_LookupWithFuzzy(g_pDictEng, wordUTF, &pWordsList,
				(PFN_PROGRESS) LookupProgressCB);
	} else {
		wordcount = DictEng_LookupWithRule(g_pDictEng, wordUTF, &pWordsList,
				(PFN_PROGRESS) LookupProgressCB);
	}

	(*env)->ReleaseStringUTFChars(env, word, wordUTF);
//	LOGE("QDictEng::LookupWords", " Wordcount = %d ", wordcount);
	if (wordcount) {
		objClass = (*env)->FindClass(env, "java/lang/String");
		objArray = (*env)->NewObjectArray(env, (jsize) wordcount, objClass, 0);
		for (i = 0; i < wordcount; i++) {
			jStrData = (*env)->NewStringUTF(env, pWordsList[i]);
			(*env)->SetObjectArrayElement(env, objArray, i, jStrData);
			(*env)->DeleteLocalRef(env, jStrData);
			free(pWordsList[i]);
		}

	}

	free(pWordsList);
	return objArray;
}

jboolean Java_com_annie_dictionary_QDictEng_LoadDicts(JNIEnv* env, jobject thiz,
		jobjectArray paths, jobjectArray names, jintArray types) {
	int i = 0, count = 0;
	char * strUTF = NULL;
	char ** pPaths = NULL;
	char ** pNames = NULL;
	int * pTypes = NULL;

	if (NULL == paths)
		return FALSE;

	count = (*env)->GetArrayLength(env, paths);
	if (count <= 0)
		return FALSE;

	pPaths = malloc(sizeof(char*) * count);
	pNames = malloc(sizeof(char*) * count);

	for (i = 0; i < count; i++) {
		strUTF = (char*) (*env)->GetStringUTFChars(env,
				(jstring)(*env)->GetObjectArrayElement(env, paths, i), NULL);
		pPaths[i] = strdup(strUTF);
		(*env)->ReleaseStringUTFChars(env,
				(jstring)(*env)->GetObjectArrayElement(env, paths, i), strUTF);
		strUTF = (char*) (*env)->GetStringUTFChars(env,
				(jstring)(*env)->GetObjectArrayElement(env, names, i), NULL);
		pNames[i] = strdup(strUTF);
		(*env)->ReleaseStringUTFChars(env,
				(jstring)(*env)->GetObjectArrayElement(env, names, i), strUTF);
	}

	pTypes = (int*) (*env)->GetIntArrayElements(env, types, 0);

	// Initialize the dict engine.
	DictEng_New(&g_pDictEng);
	DictEng_LoadDicts(g_pDictEng, pPaths, pNames, pTypes, count);

	(*env)->ReleaseIntArrayElements(env, types, pTypes, 0);

	for (i = 0; i < count; i++) {
		free(pPaths[i]);
		free(pNames[i]);
	}

	free(pPaths);
	free(pNames);

	return TRUE;
}

jboolean Java_com_annie_dictionary_QDictEng_UnloadDicts(JNIEnv* env,
		jobject thiz) {
	if (NULL != g_pDictEng) {
		DictEng_Release(g_pDictEng);
		g_pDictEng = NULL;
	}
	return TRUE;
}

void Java_com_annie_dictionary_QDictEng_CancelLookup(JNIEnv* env, jobject thiz) {
	DictEng_SetLookupCancel(g_pDictEng, TRUE);
}

jobjectArray Java_com_annie_dictionary_QDictEng_PatternListWords(JNIEnv* env,
		jobject thiz, jstring word) {
	return LookupWords(env, thiz, word, LIST_WORDS_PATTERN);
}

jobjectArray Java_com_annie_dictionary_QDictEng_FuzzyListWords(JNIEnv* env,
		jobject thiz, jstring word) {
	return LookupWords(env, thiz, word, LIST_WORDS_FUZZY);
}

jobjectArray Java_com_annie_dictionary_QDictEng_ListWords(JNIEnv* env,
		jobject thiz, jstring word) {
	return LookupWords(env, thiz, word, LIST_WORDS_NORMAL);
}

jobjectArray Java_com_annie_dictionary_QDictEng_FullTextListWords(JNIEnv* env,
		jobject thiz, jstring word) {
	jclass objClass;
	jobjectArray objArray = NULL;
	jstring jStrData;

	char * wordUTF = NULL;
	int i = 0, k = 0, j = 0;
	SearchInfo searchInfo = { 0 };
	boolean bFind = FALSE;
	int arraySize = 0;

	if (NULL == g_pDictEng)
		return NULL;

	if (DictEng_DictCount(g_pDictEng) <= 0)
		return objArray;

	wordUTF = (char*) (*env)->GetStringUTFChars(env, word, NULL);

	g_env = env;

	DictEng_SetLookupCancel(g_pDictEng, FALSE);
	bFind = DictEng_LookupData(g_pDictEng, wordUTF, &searchInfo,
			(PFN_PROGRESS) LookupProgressCB);

	(*env)->ReleaseStringUTFChars(env, word, wordUTF);

	if (FALSE == bFind) {
		DictEng_FreeSearchInfo(g_pDictEng, &searchInfo);
		return objArray;
	}
	for (i = 0; i < searchInfo.dictCount; i++) {
		arraySize += (1 + searchInfo.searchData[i].wordcount);
	}
	objClass = (*env)->FindClass(env, "java/lang/String");
	objArray = (*env)->NewObjectArray(env, (jsize) arraySize, objClass, 0);

	int totalWordcound = 0;
	for (i = 0; i < searchInfo.dictCount; i++) {
		char * pBookName = NULL;
		char * p = NULL;
		int dataSize = 0;
		int wordcount = searchInfo.searchData[i].wordcount;

		if (wordcount <= 0)
			continue;

		dataSize = strlen(searchInfo.searchData[i].bookName)
				+ strlen(HTML_BOOKNAME_B) + strlen(HTML_BOOKNAME_E);
		dataSize += 1;

		pBookName = malloc(dataSize);
		p = pBookName;

		strcpy(p, HTML_BOOKNAME_B);
		p += strlen(HTML_BOOKNAME_B);
		strcpy(p, searchInfo.searchData[i].bookName);
		p += strlen(searchInfo.searchData[i].bookName);
		strcpy(p, HTML_BOOKNAME_E);
		p += strlen(HTML_BOOKNAME_E);

		jStrData = (*env)->NewStringUTF(env, pBookName); // Add dict book name to the array.
		(*env)->SetObjectArrayElement(env, objArray, j++, jStrData);
		(*env)->DeleteLocalRef(env, jStrData);
		free(pBookName);
//		LOGE("", "Wordcount = %d", wordcount);
		for (k = 0; k < wordcount; k++) {
			jStrData = (*env)->NewStringUTF(env,
					searchInfo.searchData[i].pKeyWord[k]); // Add dict book name to the array.
			(*env)->SetObjectArrayElement(env, objArray, j++, jStrData);
			(*env)->DeleteLocalRef(env, jStrData);
		}
	}

	// searchInfo must be freed. Please refer to 'DictEng_LookupData' in 'DictEng.c'.
	DictEng_FreeSearchInfo(g_pDictEng, &searchInfo);
	return objArray;
}

jobjectArray Java_com_annie_dictionary_QDictEng_Lookup(JNIEnv* env,
		jobject thiz, jstring word, jint type) {
	jclass objClass;
	jobjectArray objArray = NULL;
	jstring jStrData;

	char * wordUTF = NULL;
	int i = 0, k = 0, j = 0;
	int dictCount = 0;
	char * pHtmlData = NULL;
	int dataSize = 0;
	boolean bFound = FALSE;
	WordInfo wordInfo = { 0 };

	if (NULL == g_pDictEng)
		return NULL;

	dictCount = DictEng_DictCount(g_pDictEng);
	if (dictCount <= 0)
		return objArray;

	wordUTF = (char*) (*env)->GetStringUTFChars(env, word, NULL);
	bFound = DictEng_Lookup(g_pDictEng, wordUTF, &wordInfo, type);
	(*env)->ReleaseStringUTFChars(env, word, wordUTF);

	if (FALSE == bFound)
		return NULL;

	objClass = (*env)->FindClass(env, "java/lang/String");
	objArray = (*env)->NewObjectArray(env, (jsize) dictCount * 3, objClass, 0);
	for (i = 0; i < wordInfo.dictCount; i++) {
		int wordcount = wordInfo.wordData[i].wordcount;
		char * p = NULL;
		char dictID[12] = { 0 };

		if (wordcount <= 0)
			continue;

		for (k = 0; k < wordcount; k++) {
			dataSize += strlen(wordInfo.wordData[i].pKeyWord[k]);
			dataSize += strlen(wordInfo.wordData[i].pWordData[k]);
			dataSize += strlen(HTML_KEYWORD_B) + strlen(HTML_KEYWORD_E);
			dataSize += strlen(HTML_NEW_LINE);
		}
		dataSize -= strlen(HTML_NEW_LINE); // Last line don't add new line.

		sprintf(dictID, "%d", wordInfo.wordData[i].dictID);
		jStrData = (*env)->NewStringUTF(env, dictID); // Add dict ID to the array.
		(*env)->SetObjectArrayElement(env, objArray, j++, jStrData);
		(*env)->DeleteLocalRef(env, jStrData);

		jStrData = (*env)->NewStringUTF(env, wordInfo.wordData[i].bookName); // Add dict book name to the array.
		(*env)->SetObjectArrayElement(env, objArray, j++, jStrData);
		(*env)->DeleteLocalRef(env, jStrData);

		pHtmlData = malloc(dataSize + 1);
		pHtmlData[dataSize] = '\0';
		p = pHtmlData;

		for (k = 0; k < wordcount; k++) {
			strcpy(p, HTML_KEYWORD_B);
			p += strlen(HTML_KEYWORD_B);
			strcpy(p, wordInfo.wordData[i].pKeyWord[k]);
			p += strlen(wordInfo.wordData[i].pKeyWord[k]);
			strcpy(p, HTML_KEYWORD_E);
			p += strlen(HTML_KEYWORD_E);

			strcpy(p, wordInfo.wordData[i].pWordData[k]);
			p += strlen(wordInfo.wordData[i].pWordData[k]);

			if (k < wordcount - 1) {
				strcpy(p, HTML_NEW_LINE);
				p += strlen(HTML_NEW_LINE);
			}
		}

		jStrData = (*env)->NewStringUTF(env, pHtmlData); // Add dict context(include keywork and dictdata) to the array.
		(*env)->SetObjectArrayElement(env, objArray, j++, jStrData);
		(*env)->DeleteLocalRef(env, jStrData);
		free(pHtmlData);
	}

	// wordInfo must be freed. Please refer to 'DictEng_Lookup' in 'DictEng.c'.
	DictEng_FreeWordInfo(g_pDictEng, &wordInfo);

	return objArray;
}

jobjectArray Java_com_annie_dictionary_QDictEng_GetInfo(JNIEnv* env,
		jobject thiz, jstring ifoPath) {
	jclass objClass;
	jobjectArray objArray = NULL;
	char * strIfoPath = NULL;
	char * strBookName = NULL;
	char * strVersion = NULL;
	char * strDescription = NULL;
	long wordcount;
	long idxfs;
	char jwordcount[12] = { 0 };
	char jidxfs[12] = { 0 };
	jstring jStrData;
	boolean bFind = FALSE;

	strIfoPath = (char*) (*env)->GetStringUTFChars(env, ifoPath, NULL);
	objClass = (*env)->FindClass(env, "java/lang/String");
	objArray = (*env)->NewObjectArray(env, (jsize) 5, objClass, 0);
	bFind = InfoFile_Load(strIfoPath, NULL, &wordcount, &idxfs, &strBookName,
			&strVersion, &strDescription, NULL);
	(*env)->ReleaseStringUTFChars(env, ifoPath, strIfoPath);

	if (bFind) {
		int j = 0;
		jStrData = (*env)->NewStringUTF(env, strBookName);
		(*env)->SetObjectArrayElement(env, objArray, j++, jStrData);
		(*env)->DeleteLocalRef(env, jStrData);
		jStrData = (*env)->NewStringUTF(env, strVersion);
		(*env)->SetObjectArrayElement(env, objArray, j++, jStrData);
		(*env)->DeleteLocalRef(env, jStrData);
		jStrData = (*env)->NewStringUTF(env, strDescription);
		(*env)->SetObjectArrayElement(env, objArray, j++, jStrData);
		(*env)->DeleteLocalRef(env, jStrData);
		sprintf(jwordcount, "%ld", wordcount);
		jStrData = (*env)->NewStringUTF(env, jwordcount);
		(*env)->SetObjectArrayElement(env, objArray, j++, jStrData);
		(*env)->DeleteLocalRef(env, jStrData);
		sprintf(jidxfs, "%ld", idxfs);
		jStrData = (*env)->NewStringUTF(env, jidxfs);
		(*env)->SetObjectArrayElement(env, objArray, j++, jStrData);
		(*env)->DeleteLocalRef(env, jStrData);
		return objArray;
	} else
		return NULL;
}
jstring Java_com_annie_dictionary_QDictEng_GetBookName(JNIEnv* env,
		jobject thiz, jstring ifoPath) {
	char * strIfoPath = NULL;
	char * strBookName = NULL;
	boolean bFind = FALSE;

	strIfoPath = (char*) (*env)->GetStringUTFChars(env, ifoPath, NULL);
	bFind = InfoFile_Load(strIfoPath, NULL, NULL, NULL, &strBookName, NULL,
			NULL, NULL);
	(*env)->ReleaseStringUTFChars(env, ifoPath, strIfoPath);

	if (bFind)
		return (*env)->NewStringUTF(env, strBookName);
	else
		return NULL;
}

//-----------------------------------------------------------------------------------------------------//

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
	JNIEnv* env = NULL;
	jint result = -1;

	if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4) != JNI_OK) {
		// ERROR: GetEnv failed.
		return result;
	}

	result = JNI_VERSION_1_4;

	return result;
}

void JNI_OnUnload(JavaVM *vm, void *reserved) {
	Java_com_annie_dictionary_QDictEng_UnloadDicts(NULL, NULL);
}
