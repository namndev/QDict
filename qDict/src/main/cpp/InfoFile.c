#include "InfoFile.h"
#include "utils/DictUtils.h"
#include "utils/MapFile.h"
#include <sys/stat.h>
#include <stdlib.h>

static const char* skip_new_line(const char *p) {
	if (!p)
		return NULL;
	if (*p == '\n')
		return ++p;
	if (*p == '\r') {
		++p;
		if (*p == '\n')
			++p;
		return p;
	}
	return NULL;
}

static const char * get_key_value(const char * line_beg, char ** key,
		char ** value) {
	const char * key_beg;
	const char * equal_sign;
	const char * key_end;
	const char * val_beg;
	const char * val_end;
	int strLen = 0;
	while (TRUE) {
		const size_t n1 = strcspn(line_beg, "\r\n");
		const size_t n2 = strspn(line_beg, " \t");
		const char* const line_end = line_beg + n1;
		if (*line_end == '\0') { // EOF reached
			if (n1 != n2)
				// Last line is not terminated with new line char.
				return NULL;
		}
		// new line char found
		if (n1 == n2) { // empty line
			if (n1 == 0) // TODO: Why need to add this line?
				return NULL;

			line_beg = skip_new_line(line_end);
			continue;
		}
		key_beg = line_beg + n2; // first non-blank char
		equal_sign = key_beg;
		while (*equal_sign != '=' && equal_sign < line_end)
			++equal_sign;
		if (*equal_sign != '=') {
			// '=' not found.
			line_beg = skip_new_line(line_end);
			continue;
		}
		key_end = equal_sign;
		while (key_beg < key_end
				&& (*(key_end - 1) == ' ' || *(key_end - 1) == '\t'))
			--key_end;
		strLen = key_end - key_beg;
		(*key) = malloc(strLen + 1);
		strncpy(*key, key_beg, strLen);
		(*key)[strLen] = 0;

		val_beg = equal_sign + 1;
		val_end = line_end;
		while (val_beg < line_end && (*val_beg == ' ' || *val_beg == '\t'))
			++val_beg;
		while (val_beg < val_end
				&& (*(val_end - 1) == ' ' || *(val_end - 1) == '\t'))
			--val_end;

		strLen = val_end - val_beg;
		(*value) = malloc(strLen + 1);
		strncpy(*value, val_beg, strLen);
		(*value)[strLen] = 0;

		line_beg = skip_new_line(line_end);

		return line_beg;
	}
}

boolean InfoFile_Load(char * ifofile, long *wc, long *idxfs, long *swc,
		char ** bookname, char **version, char** description,
		char ** sametypesequence) {
	char * key = NULL;
	char * value = NULL;
	const char *p = NULL;
	const char *p1 = NULL;
	int fsize = 0;
	struct stat stats;
	FILE * pf = NULL;

	if (stat(ifofile, &stats))
		return FALSE;
	fsize = stats.st_size;

	if (0 == fsize)
		return FALSE;

	p = malloc(fsize + 1);
	memset((void*) p, 0, fsize + 1);
	pf = fopen(ifofile, "rb");
	fread((void*) p, fsize, 1, pf);
	fclose(pf);

	p1 = p;
	if (!p1) {
		return FALSE;
	}

	while (TRUE) {
		p1 = get_key_value(p1, &key, &value);

		if (!p1)
			break;

		if (0 == (stricmp(key, "version"))) {
			if (version) {
				*version = value;
				free(key);
				key = NULL;
				value = NULL;
				continue;
			}
		} else if (0 == (stricmp(key, "wordcount"))) {
			if (wc)
				*wc = atol(value);
		} else if (0 == (stricmp(key, "synwordcount"))) {
			if (swc)
				*swc = atol(value);
		} else if (0 == (stricmp(key, "idxfilesize"))) {
			if (idxfs)
				*idxfs = atol(value);
		} else if (0 == (stricmp(key, "bookname"))) {
			if (bookname) {
				*bookname = value;
				free(key);
				key = NULL;
				value = NULL;
				continue;
			}
		} else if (0 == (stricmp(key, "description"))) {
			if (description) {
				*description = value;
				free(key);
				key = NULL;
				value = NULL;
				continue;
			}
		} else if (0 == (stricmp(key, "sametypesequence"))) {
			// 'mtygxkwhnr'
			// m: Word's pure text meaning. The data should be a utf-8 string ending with '\0'.
			// t: English phonetic string. The data should be a utf-8 string ending with '\0'.
			// y: Chinese YinBiao or Japanese KANA. The data should be a utf-8 string ending with '\0'.
			// g: A utf-8 string which is marked up with the Pango text markup language.
			// x: A utf-8 string which is marked up with the xdxf language.
			// k: KingSoft PowerWord's data. The data is a utf-8 string ending with '\0'. It is in XML format.
			// w: MediaWiki markup language.
			// h: Html codes.
			// n: WordNet data.
			// r: Resource file list.
			if (sametypesequence) {
				*sametypesequence = value;
				free(key);
				key = NULL;
				value = NULL;
				continue;
			}
		}
		/*
		 else if(key == "idxoffsetbits") {
		 }
		 else if(key == "filecount") {
		 set_filecount(atol(value.c_str()));
		 }
		 else if(key == "tdxfilesize") {
		 set_index_file_size(atol(value.c_str()));
		 }
		 else if(key == "ridxfilesize") {
		 set_index_file_size(atol(value.c_str()));
		 }
		 else if(key == "dicttype") {
		 set_dicttype(value);
		 }
		 else if(key == "author") {
		 set_author(value);
		 }
		 else if(key == "email") {
		 set_email(value);
		 }
		 else if(key == "website") {
		 set_website(value);
		 }
		 else if(key == "date") {
		 set_date(value);
		 }
		 else if(key == "description") {
		 std::string temp;
		 decode_description(value.c_str(), value.length(), temp);
		 set_description(temp);
		 }
		 else {
		 // Warning: unknown option.
		 }*/

		if (NULL != key)
			free(key);
		if (NULL != value)
			free(value);
	}

	free((void*) p);

	return TRUE;
}

boolean InfoFile_GetValueByKey(char * pInfoBuffer, long * pValue, char * KEY) {
	char * key = NULL;
	char * value = NULL;
	const char *p = NULL;
	const char *p1 = NULL;
	boolean bFound = FALSE;

	p = pInfoBuffer;

	p1 = p;
	if (!p1) {
		return FALSE;
	}

	while (TRUE) {
		p1 = get_key_value(p1, &key, &value);

		if (!p1)
			break;

		if (0 == (stricmp(key, KEY))) {
			bFound = TRUE;

			if (pValue)
				*pValue = atol(value);
		}

		if (NULL != key)
			free(key);
		if (NULL != value)
			free(value);

		if (TRUE == bFound) {
			break;
		}
	}

	return bFound;
}
