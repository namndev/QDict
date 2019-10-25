#include "DictPlugs.h"

// WARNING!!!
// This file must be saved as UTF-8 character

// BUG: if there are nested flag like &I{f&9{º}n&9{º}tus}, then it will can't work correctly.


#define TAG_MAXLEN				30


#define HTML_TAG_HEAD_B			"<b><span style='color:#TOBEREPLACE;'>["
#define HTML_TAG_HEAD_E			"]</span></b><br>"
#define HTML_TAG_WORD_B			"<b><span style='color:#TOBEREPLACE;'>"
#define HTML_TAG_WORD_E			"</span></b><br>"
#define HTML_TAG_TEXT_B			""
#define HTML_TAG_TEXT_E			"<br>"
#define HTML_TAG_NEWWORD_B		""
#define HTML_TAG_NEWWORD_E		"<br>"
#define HTML_TAG_EXAMPLE_B		"<i>"
#define HTML_TAG_EXAMPLE_E		"</i><br>"


#define HTML_TAG_PHONETIC_B		"<span style='font-family:KPhonetic'>"
#define HTML_TAG_PHONETIC_E		"</span>"
#define HTML_TAG_B_B			"<b>"
#define HTML_TAG_B_E			"</b>"
#define HTML_TAG_I_B			"<i>"
#define HTML_TAG_I_E			"</i>"
#define HTML_TAG_SUP_B			"<sup>"
#define HTML_TAG_SUP_E			"</sup>"
#define HTML_TAG_SUB_B			"<sub>"
#define HTML_TAG_SUB_E			"</sub>"



#define TAG_TEXT_B				"<![CDATA["
#define TAG_TEXT_E				"]]>"
#define TAG_TEXT_B_LEN			9
#define TAG_TEXT_E_LEN			3


char g_pTag[TAG_MAXLEN];
int g_StrLen = 0;
int g_WordCount = 0;


int PowerWord_AddText(char * des, char * src, int len, char * tagB, char * tagE)
{
	int tagBLen = strlen(tagB);
	int tagELen = strlen(tagE);
	int offset = len + tagBLen + tagELen;
	char * pTmp = des;

	memcpy(pTmp, tagB, tagBLen);
	pTmp += tagBLen;
	memcpy(pTmp, src, len);
	pTmp += len;
	memcpy(pTmp, tagE, tagELen);

	return offset;
}

char * PowerWord_ParseText(char * tag, char * str, char * tagB, char * tagE)
{
	char * pStart = strstr(str, TAG_TEXT_B);
	char * pEnd = strstr(str, TAG_TEXT_E);
	char * pStr = NULL;
	char * pTmp = NULL;
	int len = 0;
	int tagBLen = 0, tagELen = 0;
	int count = 0;
	int offset = 0;
	int size = 0;

	if(NULL == pStart || NULL == pEnd)
	{
		return NULL;
	}

	pStart += TAG_TEXT_B_LEN;
	tagBLen = strlen(tagB);
	tagELen = strlen(tagE);

	if(pStart >= pEnd)
	{
		return NULL;
	}

	len = pEnd - pStart;
	g_StrLen += len + TAG_TEXT_B_LEN + TAG_TEXT_E_LEN;

	pTmp = pStart;

	// Count the flag number.
	while(TRUE)
	{
		char * str = NULL;

		pTmp = strchr(pTmp, '&');

		if(NULL == pTmp || pTmp >= pEnd)
			break;

		if(*(pTmp + 2) != '{')
		{
			pTmp += 2;	// Must be the format such as "&b{Hague}", "&2{zbl¾d}", "&I{Archaic}" and so on.
		}
		else
		{
			count ++;
			pTmp += 3;
		}
	}

	// Make sure the heap is enough for the additional TAG.
	size = len + 1 + tagBLen + tagELen + count * (strlen(HTML_TAG_PHONETIC_B) + strlen(HTML_TAG_PHONETIC_E));
	pStr = malloc(size);
	if(NULL == pStr)
	{
		return NULL;
	}
	memset(pStr, 0, size);

	memcpy(pStr, tagB, strlen(tagB));
    offset = strlen(tagB);

	pTmp = pStart;

	while(TRUE)
	{
		char * pTmpEnd = NULL;
		char * pTmpStart = pTmp;
		char pFlag = 0;

		pTmp = strchr(pTmp, '&');

		if(NULL == pTmp || pTmp >= pEnd)
		{
			offset += PowerWord_AddText(pStr + offset, pTmpStart, pEnd - pTmpStart,"", "");
			break;
		}

		if(*(pTmp + 2) != '{')
		{
			offset += PowerWord_AddText(pStr + offset, pTmpStart, 2,"", "");
			pTmp += 2;	// Must be the format such as "&b{Hague}", "&2{zbl¾d}", "&I{Archaic}" and so on.
			continue;
		}

		pFlag = *(pTmp + 1);

		pTmpEnd = strchr(pTmp + 3, '}');

		if(NULL == pTmpEnd)
		{
			offset += PowerWord_AddText(pStr + offset, pTmpStart, pEnd - pTmpStart,"", "");
			break;
		}

		// Add the text before "&"
		offset += PowerWord_AddText(pStr + offset, pTmpStart, pTmp - pTmpStart,"", "");
		pTmp += 3;

		switch (pFlag) {
			case 'X':
			case '2':
				{
					offset += PowerWord_AddText(pStr + offset, pTmp, pTmpEnd - pTmp, HTML_TAG_PHONETIC_B, HTML_TAG_PHONETIC_E);
					break;
				}

			case 'b':
			case 'B':
				{
					offset += PowerWord_AddText(pStr + offset, pTmp, pTmpEnd - pTmp, HTML_TAG_B_B, HTML_TAG_B_E);
					break;
				}

			case 'I':
				{
					offset += PowerWord_AddText(pStr + offset, pTmp, pTmpEnd - pTmp, HTML_TAG_I_B, HTML_TAG_I_E);
					break;
				}

			case '+':
				{
					offset += PowerWord_AddText(pStr + offset, pTmp, pTmpEnd - pTmp, HTML_TAG_SUP_B, HTML_TAG_SUP_E);
					break;
				}

			case '-':
				{
					offset += PowerWord_AddText(pStr + offset, pTmp, pTmpEnd - pTmp, HTML_TAG_SUB_B, HTML_TAG_SUB_E);
					break;
				}

			// 'x'
			// 'l':
			// 'D':
			// 'L':
			// 'U':
			// ' '
			// '9':
			// 'S:
			default:
				{
					offset += PowerWord_AddText(pStr + offset, pTmp, pTmpEnd - pTmp,"", "");
				break;
				}
		}

		pTmp = pTmpEnd + 1;
	}

	memcpy(pStr + offset, tagE, strlen(tagE));

	return pStr;
}

char * PowerWord_ParseHead(char * tag, char * tagB, char * tagE)
{
	char * pStr = NULL;
	int tagBLen = strlen(tagB);
	int tagELen = strlen(tagE);
	int tagLen = strlen(tag);
	int len = tagLen + 1 + tagBLen + tagELen;

	pStr = malloc(len);
	if(NULL == pStr)
	{
		return NULL;
	}

	memset(pStr, 0, len);
	memcpy(pStr, tagB, tagBLen);
	memcpy(pStr + tagBLen, tag, tagLen);
	memcpy(pStr + tagBLen + tagLen, tagE, tagELen);

	return pStr;
}

char * PowerWord_HandleTag(char * tag, char * str)
{
	// 词典音标
	// 相关词
	// 跟随解释
	// 繁体写法
	// 台湾音标
	// 图片名称
	// 跟随注释
	// 惯用型原型
	// 惯用型解释
	if(0 == strcmp(tag, (const char *)"音节分段"))
	{
		return PowerWord_ParseText(tag, str, HTML_TAG_WORD_B, HTML_TAG_WORD_E);
	}
	if(0 == strcmp(tag, (const char *)"单词原型"))
	{
		return PowerWord_ParseText(tag, str, HTML_TAG_WORD_B, HTML_TAG_WORD_E);
	}
	else if(0 == strcmp(tag, (const char *)"AHD音标"))
	{
		return PowerWord_ParseText(tag, str, HTML_TAG_TEXT_B, HTML_TAG_TEXT_E);
	}
	else if(0 == strcmp(tag, (const char *)"国际音标"))
	{
		return PowerWord_ParseText(tag, str, HTML_TAG_TEXT_B, HTML_TAG_TEXT_E);
	}
	else if(0 == strcmp(tag, (const char *)"美国音标"))
	{
		return PowerWord_ParseText(tag, str, HTML_TAG_TEXT_B, HTML_TAG_TEXT_E);
	}
	else if(0 == strcmp(tag, (const char *)"汉语拼音"))
	{
		return PowerWord_ParseText(tag, str, HTML_TAG_TEXT_B, HTML_TAG_TEXT_E);
	}
	else if(0 == strcmp(tag, (const char *)"日文发音"))
	{
		return PowerWord_ParseText(tag, str, HTML_TAG_TEXT_B, HTML_TAG_TEXT_E);
	}
	else if(0 == strcmp(tag, (const char *)"子解释项"))
	{
		return PowerWord_ParseText(tag, str, HTML_TAG_TEXT_B, HTML_TAG_TEXT_E);
	}
	else if(0 == strcmp(tag, (const char *)"例句原型"))
	{
		return PowerWord_ParseText(tag, str, HTML_TAG_EXAMPLE_B, HTML_TAG_EXAMPLE_E);
	}
	else if(0 == strcmp(tag, (const char *)"例句解释"))
	{
		return PowerWord_ParseText(tag, str, HTML_TAG_TEXT_B, HTML_TAG_TEXT_E);
	}
	else if(0 == strcmp(tag, (const char *)"另见"))
	{
		return PowerWord_ParseText(tag, str, HTML_TAG_TEXT_B, HTML_TAG_TEXT_E);
	}
#ifndef _WIN32	// Can't compire successfully with VS.NET.
	else if(0 == strcmp(tag, (const char *)"单词词性"))
	{
		return PowerWord_ParseText(tag, str, HTML_TAG_EXAMPLE_B, HTML_TAG_EXAMPLE_E);
	}
	else if(0 == strcmp(tag, (const char *)"预解释"))
	{
		return PowerWord_ParseText(tag, str, HTML_TAG_TEXT_B, HTML_TAG_TEXT_E);
	}
	else if(0 == strcmp(tag, (const char *)"解释项"))
	{
		return PowerWord_ParseText(tag, str, HTML_TAG_TEXT_B, HTML_TAG_TEXT_E);
	}
	else if(0 == strcmp(tag, (const char *)"同义词"))
	{
		return PowerWord_ParseText(tag, str, HTML_TAG_TEXT_B, HTML_TAG_TEXT_E);
	}
#endif

	// The following is the paragraph head.
	else if(0 == strcmp(tag, (const char *)"基本词义"))
	{
		g_WordCount ++;
		if(g_WordCount <= 1)
		{
			return NULL;
		}
		else
		{
			return PowerWord_ParseHead("", HTML_TAG_NEWWORD_B, HTML_TAG_NEWWORD_E);		// Only add a blank line before this.
		}
	}
	else if(0 == strcmp(tag, (const char *)"继承用法"))
	{
		return PowerWord_ParseHead(tag, HTML_TAG_HEAD_B, HTML_TAG_HEAD_E);
	}
	else if(0 == strcmp(tag, (const char *)"习惯用语"))
	{
		return PowerWord_ParseHead(tag, HTML_TAG_HEAD_B, HTML_TAG_HEAD_E);
	}
	else if(0 == strcmp(tag, (const char *)"特殊用法"))
	{
		return PowerWord_ParseHead(tag, HTML_TAG_HEAD_B, HTML_TAG_HEAD_E);
	}
	else if(0 == strcmp(tag, (const char *)"常用词组"))
	{
		return PowerWord_ParseHead(tag, HTML_TAG_HEAD_B, HTML_TAG_HEAD_E);
	}
	else if(0 == strcmp(tag, (const char *)"语源"))
	{
		return PowerWord_ParseHead(tag, HTML_TAG_HEAD_B, HTML_TAG_HEAD_E);
	}
	else if(0 == strcmp(tag, (const char *)"派生"))
	{
		return PowerWord_ParseHead(tag, HTML_TAG_HEAD_B, HTML_TAG_HEAD_E);
	}
	else if(0 == strcmp(tag, (const char *)"用法"))
	{
		return PowerWord_ParseHead(tag, HTML_TAG_HEAD_B, HTML_TAG_HEAD_E);
	}
	else if(0 == strcmp(tag, (const char *)"注释"))
	{
		return PowerWord_ParseHead(tag, HTML_TAG_HEAD_B, HTML_TAG_HEAD_E);
	}
#ifndef _WIN32	// Can't compire successfully with VS.NET.
	else if(0 == strcmp(tag, (const char *)"参考词汇"))
	{
		return PowerWord_ParseHead(tag, HTML_TAG_HEAD_B, HTML_TAG_HEAD_E);
	}
	else if(0 == strcmp(tag, (const char *)"词性变化"))
	{
		return PowerWord_ParseHead(tag, HTML_TAG_HEAD_B, HTML_TAG_HEAD_E);
	}
#endif

	return NULL;
}


char * PowerWord_ParseTag(char * str)
{
  char * pTmp = strchr(str, '>');
  int len = 0;

  if(NULL == pTmp)
  {
	  return NULL;
  }

  len = pTmp - str - 1;

  g_StrLen = len + 2;

  if(len >= TAG_MAXLEN)
  {
	  return NULL;
  }

  memcpy(g_pTag, str + 1, len);
  g_pTag[len] = '\0';

  return PowerWord_HandleTag(g_pTag, str + len);
}

char * PowerWord_ParseData(char * data, int size)
{
	char * pStr = malloc(size);
	char * pTmp = data;
	int len = 0;
	g_WordCount = 0;

	if (NULL == pStr)
	{
		free(data);

		return NULL;
	}

	while(TRUE)
	{
		char * str = NULL;

		pTmp = strchr(pTmp, '<');

		if(NULL == pTmp)
			break;

		if(*(pTmp + 1) == '!' || *(pTmp + 1) == '/')
		{
			pTmp += 2;	// Ignor "<![CDATA[" and "</"
			continue;
		}

		g_StrLen = 1;
		str = PowerWord_ParseTag(pTmp);

		if(NULL != str)
		{
			if((len + (int)strlen(str) + 1) > size)
			{
				pStr = realloc(pStr, strlen(str) + len * 2);
			}

			memcpy(pStr + len, str, strlen(str));
			len += strlen(str);
			free(str);
		}

		pTmp += g_StrLen;
	}

	pStr[len] = '\0';
	free(data);

	return pStr;
}

