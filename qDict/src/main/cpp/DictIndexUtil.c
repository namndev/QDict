#include "utils/DictUtils.h"
#include "DictIndexUtil.h"

boolean check_key_str_len(const char* str, size_t buf_size) {
	size_t i = 0, max = MAX_INDEX_KEY_SIZE;

	if (buf_size < max)
		max = buf_size;

	for (i = 0; i < max; ++i)
		if (!str[i])
			return TRUE;

	return FALSE;
}

uint32 get_byte(const char * addr) {
	byte result;
	memcpy(&result, addr, sizeof(byte));
	return result;
}

uint32 get_uint32(const char * addr) {
	uint32 result;
	memcpy(&result, addr, sizeof(uint32));
	return result;
}

int svn_strcmp(const char *s1, const char *s2) {
	int a = stricmp(s1, s2);
//	if (a == 0) {
//		return strcmp(s1, s2);
//	} else
	return a;
}

/* return the length of the common prefix of two strings in characters
 * comparison is case-insensitive */
int prefix_match(const char *s1, const char *s2) {
	int ret = -1;
	wchar sz1[MAX_INDEX_KEY_SIZE] = { 0 }, sz2[MAX_INDEX_KEY_SIZE] = { 0 };
	wchar *p1 = sz1;
	wchar *p2 = sz2;
	wchar u1, u2;

	if (!s1 || !s2)
		return 0;

	UTF8ToWStr((const byte *) s1, strlen(s1), sz1, MAX_INDEX_KEY_SIZE);
	UTF8ToWStr((const byte *) s2, strlen(s2), sz2, MAX_INDEX_KEY_SIZE);
	WStrLower(sz1);
	WStrLower(sz2);

	do {
		u1 = *p1;
		u2 = *p2;
		p1++;
		p2++;
		ret++;
	} while (u1 && u1 == u2);

	return ret;
}
