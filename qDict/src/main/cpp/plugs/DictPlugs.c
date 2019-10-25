#include "DictPlugs.h"
#include "Powerword.h"


#define HTML_LF					"<br>"

// pure text, replace "\n"(0A==LF) with "<br>"
char * DictPlugs_ParseData_M(char * data, int size)
{
	int i = 0, LFCount = 0, sz = 0;
	char * p = NULL;
	char * p1 = NULL;
	char * p2 = NULL;
	char * parseData = NULL;

	p = data;
	for (i = 0; i < size; i++)
	{
		if(*p == '\n')
		{
			LFCount++;
		}
		p++;
	}

	sz = size + (strlen(HTML_LF) - 1) * LFCount;
	parseData = malloc(sz);
	if(NULL == parseData)
		return data;

	p = data;
	p1 = p;
	p2 = parseData;

	for (i = 0; i < size; i++)
	{
		if(*p == '\n' || (i == size - 1))
		{
			strncpy(p2, p1, (p - p1));
			p2 += (p - p1);
			p1 = p + 1;
			if(i != size - 1)
			{
				strncpy(p2, HTML_LF, strlen(HTML_LF));
				p2 += strlen(HTML_LF);
			}
		}
		p++;
	}

	parseData[sz - 1] = '\0';
	free(data);
	return parseData;
}


#define WORD_NET_TYPE_N				"[Noun]"
#define WORD_NET_TYPE_V				"[Verb]"
#define WORD_NET_TYPE_A				"[Adjective]"
#define WORD_NET_TYPE_S				"[Adjective satellite]"
#define WORD_NET_TYPE_R				"[Adverb]"

#define WORD_NET_TYPE_HTML_B		"<span style='color:#00ccff;'>"
#define WORD_NET_TYPE_HTML_E		"</span><br>"
#define WORD_NET_WORD_HTML_B		"<span style='color:#;0000ff'>"
#define WORD_NET_WORD_HTML_E		"</span> &nbsp;&nbsp;&nbsp;"
#define WORD_NET_GLOSS_HTML_B		"<span style='color:#0909cc;'>"
#define WORD_NET_GLOSS_HTML_E		"</span>"
#define WORD_NET_HTML_BR			"<br>"

#define WORD_NET_TYPE_B				"<type>"
#define WORD_NET_TYPE_E				"</type>"
#define WORD_NET_WORD_B				"<word>"
#define WORD_NET_WORD_E				"</word>"
#define WORD_NET_GLOSS_B			"<gloss>"
#define WORD_NET_GLOSS_E			"</gloss>"
#define WORD_NET_WORDGROUP_B		"<wordgroup>"
#define WORD_NET_WORDGROUP_E		"</wordgroup>"

/* WordNet 3.0
 * Sample: <type>n</type><wordgroup><word>fine</word><word>mulct</word><word>amercement</word></wordgroup><gloss>money extracted as a penalty</gloss>
 * <type>: "n",Noun; "v",Verb; "a",Adjective; "s",Adjective satellite; "r",Adverb
 */
char * DictPlugs_ParseData_N(char * data, int size)
{
	int typecount = 0;
	int wordcount = 0;
	int glosscount = 0;
	int sz = 0;
	int k = 0;
	char * type = NULL;
	char * p = NULL;
	char * p1 = NULL;
	char * parseData = NULL;

	p = data;

	while(TRUE)
	{
		char * str = strstr(p, WORD_NET_WORD_B);
		if(NULL == str)
			break;
		p = str + strlen(WORD_NET_WORD_B);
		wordcount++;
	}

	if(p = strstr(data, WORD_NET_TYPE_B))
	{
		typecount = 1;
		sz += strlen(WORD_NET_TYPE_HTML_B) + strlen(WORD_NET_TYPE_HTML_E);
		sz -= (strlen(WORD_NET_TYPE_B) + strlen(WORD_NET_TYPE_E));

		p += strlen(WORD_NET_TYPE_B);

		switch(*p)
		{
		case 'n':
			k = strlen(WORD_NET_TYPE_N);
			type = WORD_NET_TYPE_N;
			break;
		case 'v':
			k = strlen(WORD_NET_TYPE_V);
			type = WORD_NET_TYPE_V;
			break;
		case 'a':
			k = strlen(WORD_NET_TYPE_A);
			type = WORD_NET_TYPE_A;
			break;
		case 's':
			k = strlen(WORD_NET_TYPE_S);
			type = WORD_NET_TYPE_S;
			break;
		case 'r':
			k = strlen(WORD_NET_TYPE_R);
			type = WORD_NET_TYPE_R;
			break;
		default:
			type = "";
			break;
		}

		sz += (k - 1);
	}
	if(strstr(data, WORD_NET_WORDGROUP_B))
	{
		sz -= (strlen(WORD_NET_WORDGROUP_B) + strlen(WORD_NET_WORDGROUP_E));
	}
	if(strstr(data, WORD_NET_GLOSS_B))
	{
		glosscount = 1;
		sz += strlen(WORD_NET_GLOSS_HTML_B) + strlen(WORD_NET_GLOSS_HTML_E);
		if (wordcount > 0)	// Add a <br> before gloss content.
			sz += strlen(WORD_NET_HTML_BR);
		sz -= (strlen(WORD_NET_GLOSS_B) + strlen(WORD_NET_GLOSS_E));
	}
	sz += (strlen(WORD_NET_WORD_HTML_B) + strlen(WORD_NET_WORD_HTML_E)) * wordcount;
	sz -= (strlen(WORD_NET_WORD_B) + strlen(WORD_NET_WORD_E)) * wordcount;

	sz += size;

	parseData = malloc(sz);
	if (NULL == parseData)
		return data;

	p = data;
	p1 = parseData;

	if(typecount > 0)
	{
		strcpy(p1, WORD_NET_TYPE_HTML_B);
		p1 += strlen(WORD_NET_TYPE_HTML_B);
		strcpy(p1, type);
		p1 += strlen(type);
		strcpy(p1, WORD_NET_TYPE_HTML_E);
		p1 += strlen(WORD_NET_TYPE_HTML_E);
	}

	if(wordcount > 0)
	{
		while(TRUE)
		{
			char * str = NULL;
			char * strEnd = NULL;

			str = strstr(p, WORD_NET_WORD_B);
			if(NULL == str)
				break;
			str += strlen(WORD_NET_WORD_B);
			strEnd = strstr(str, WORD_NET_WORD_E);
			p = str + strlen(WORD_NET_WORD_B);

			strcpy(p1, WORD_NET_WORD_HTML_B);
			p1 += strlen(WORD_NET_WORD_HTML_B);
			strncpy(p1, str, strEnd - str);
			p1 += strEnd - str;
			strcpy(p1, WORD_NET_WORD_HTML_E);
			p1 += strlen(WORD_NET_WORD_HTML_E);
		}
	}

	if(glosscount > 0)
	{
		char * str = NULL;
		char * strEnd = NULL;

		str = strstr(p, WORD_NET_GLOSS_B);
		str += strlen(WORD_NET_GLOSS_B);
		strEnd = strstr(str, WORD_NET_GLOSS_E);

		if (wordcount > 0)
		{
			strcpy(p1, WORD_NET_HTML_BR);
			p1 += strlen(WORD_NET_HTML_BR);
		}
		strcpy(p1, WORD_NET_GLOSS_HTML_B);
		p1 += strlen(WORD_NET_GLOSS_HTML_B);
		strncpy(p1, str, strEnd - str);
		p1 += strEnd - str;
		strcpy(p1, WORD_NET_GLOSS_HTML_E);
		p1 += strlen(WORD_NET_GLOSS_HTML_E);
	}

	parseData[sz - 1] = '\0';

	free(data);

	return parseData;
}

// Chinese YinBiao or Japanese KANA, replace '\0' with '\n'
char * DictPlugs_ParseData_Y(char * data, int size)
{
	int i = 0;
	char * p = NULL;

	p = data;

	for (i = 0; i < size - 1; i++)
	{
		if(*p == '\0')
		{
			*p = '\n';
		}
		p++;
	}

	return data;
}

char * DictPlugs_ParseData_X(char * data, int size)
{
	return DictPlugs_ParseData_M(data, size);
}

char * DictPlugs_ParseData_T(char * data, int size)
{
	return DictPlugs_ParseData_Y(data, size);	
}

char * DictPlugs_ParseData_K(char * data, int size)
{
	return PowerWord_ParseData(data, size);
}

char * DictPlugs_ParseData(char * data, int size, char * sameTypeSequence)
{
	char * pData = data;

	if (strchr(sameTypeSequence, 'y'))
	{
		pData = DictPlugs_ParseData_Y(data, size);
	}

	if (strchr(sameTypeSequence, 't'))
	{
		pData = DictPlugs_ParseData_T(data, size);
	}

	if (strchr(sameTypeSequence, 'm'))
	{
		pData = DictPlugs_ParseData_M(data, size);
	}

	if (strchr(sameTypeSequence, 'x'))
	{
		pData = DictPlugs_ParseData_X(data, size);
	}

	if (strchr(sameTypeSequence, 'n'))
	{
		pData = DictPlugs_ParseData_N(data, size);
	}

	if (strchr(sameTypeSequence, 'k'))
	{
		pData = DictPlugs_ParseData_K(data, size);
	}

	return pData;
}
