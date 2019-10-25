#include "Pattern.h"


/* --- functions --- */
static boolean pattern_ph_match(const char *match_pattern, const char *match_string, boolean *wildcard_reached_p)
{
	register const char *pattern, *string;
	register char ch;

	pattern = match_pattern;
	string = match_string;

	ch = *pattern;
	pattern++;
	while (ch) {
		switch (ch) {
		case '?':
			if (!*string)
				return FALSE;
			string = utf8_next_char(string);
			break;

		case '*':
			*wildcard_reached_p = TRUE;
			do {
				ch = *pattern;
				pattern++;
				if (ch == '?') {
					if (!*string)
						return FALSE;
					string = utf8_next_char(string);
				}
			} while (ch == '*' || ch == '?');

			if (!ch)
				return TRUE;

			do {
				boolean next_wildcard_reached = FALSE;
				while (ch != *string) {
					if (!*string)
						return FALSE;
					string = utf8_next_char(string);
				}
				string++;
				if (pattern_ph_match(pattern, string, &next_wildcard_reached))
					return TRUE;
				if (next_wildcard_reached)
					return FALSE;
			} while (*string);
			break;

		default:
			if (ch == *string)
				string++;
			else
				return FALSE;
			break;
		}

		ch = *pattern;
		pattern++;
	}

	return *string == 0;
}

boolean pattern_match(PatternSpec *pspec, uint32 string_length, const char *string, const char *string_reversed)
{
	if (string_length < pspec->min_length || string_length > pspec->max_length)
		return FALSE;

	switch (pspec->match_type)
	{
		boolean dummy;
		case G_MATCH_ALL:
			return pattern_ph_match(pspec->pattern, string, &dummy);
		case G_MATCH_ALL_TAIL:
			if (string_reversed)
				return pattern_ph_match(pspec->pattern, string_reversed, &dummy);
			else {
				boolean result;
				char *tmp;
				tmp = UTF8StrReverse(string, string_length);
				result = pattern_ph_match(pspec->pattern, tmp, &dummy);
				free(tmp);
				return result;
			}
		case G_MATCH_HEAD:
			if (pspec->pattern_length == string_length)
				return stricmp(pspec->pattern, string) == 0;
			else if (pspec->pattern_length)
				return strnicmp(pspec->pattern, string, pspec->pattern_length) == 0;
			else
				return TRUE;
		case G_MATCH_TAIL:
			if (pspec->pattern_length)
				return stricmp(pspec->pattern, string + (string_length - pspec->pattern_length)) == 0;
			else
				return TRUE;
		case G_MATCH_EXACT:
			if (pspec->pattern_length != string_length)
				return FALSE;
			else
				return stricmp(pspec->pattern, string) == 0;
		default:
			return FALSE;
			}
}

PatternSpec* pattern_spec_init(const char *pattern)
{
	PatternSpec *pspec;
	boolean seen_joker = FALSE, seen_wildcard = FALSE, more_wildcards = FALSE;
	int hw_pos = -1, tw_pos = -1, hj_pos = -1, tj_pos = -1;
	boolean follows_wildcard = FALSE;
	uint32 pending_jokers = 0;
	const char *s;
	char *d;
	uint32 i;

	/* canonicalize pattern and collect necessary stats */
	pspec = malloc(sizeof(PatternSpec));
	pspec->pattern_length = strlen(pattern);
	pspec->min_length = 0;
	pspec->max_length = 0;
	pspec->pattern = malloc(sizeof(char) * pspec->pattern_length + 1);
	d = pspec->pattern;
	for (i = 0, s = pattern; *s != 0; s++) {
		switch (*s) {
		case '*':
			if (follows_wildcard) /* compress multiple wildcards */
			{
				pspec->pattern_length--;
				continue;
			}
			follows_wildcard = TRUE;
			if (hw_pos < 0)
				hw_pos = i;
			tw_pos = i;
			break;
		case '?':
			pending_jokers++;
			pspec->min_length++;
			pspec->max_length += 4; /* maximum UTF-8 character length */
			continue;
		default:
			for (; pending_jokers; pending_jokers--, i++) {
				*d++ = '?';
				if (hj_pos < 0)
					hj_pos = i;
				tj_pos = i;
			}
			follows_wildcard = FALSE;
			pspec->min_length++;
			pspec->max_length++;
			break;
		}
		*d++ = *s;
		i++;
	}
	for (; pending_jokers; pending_jokers--) {
		*d++ = '?';
		if (hj_pos < 0)
			hj_pos = i;
		tj_pos = i;
	}
	*d++ = 0;
	seen_joker = hj_pos >= 0;
	seen_wildcard = hw_pos >= 0;
	more_wildcards = seen_wildcard && hw_pos != tw_pos;
	if (seen_wildcard)
		pspec->max_length = MAX_UINT32;

	/* special case sole head/tail wildcard or exact matches */
	if (!seen_joker && !more_wildcards) {
		if (pspec->pattern[0] == '*') {
			pspec->match_type = G_MATCH_TAIL;
			memmove(pspec->pattern, pspec->pattern + 1, --pspec->pattern_length);
			pspec->pattern[pspec->pattern_length] = 0;
			return pspec;
		}
		if (pspec->pattern_length > 0 && pspec->pattern[pspec->pattern_length - 1] == '*') {
			pspec->match_type = G_MATCH_HEAD;
			pspec->pattern[--pspec->pattern_length] = 0;
			return pspec;
		}
		if (!seen_wildcard) {
			pspec->match_type = G_MATCH_EXACT;
			return pspec;
		}
	}

	/* now just need to distinguish between head or tail match start */
	tw_pos = pspec->pattern_length - 1 - tw_pos; /* last pos to tail distance */
	tj_pos = pspec->pattern_length - 1 - tj_pos; /* last pos to tail distance */
	if (seen_wildcard)
		pspec->match_type = tw_pos > hw_pos ? G_MATCH_ALL_TAIL : G_MATCH_ALL;
	else
		/* seen_joker */
		pspec->match_type = tj_pos > hj_pos ? G_MATCH_ALL_TAIL : G_MATCH_ALL;
	if (pspec->match_type == G_MATCH_ALL_TAIL) {
		char *tmp = pspec->pattern;
		pspec->pattern = UTF8StrReverse(pspec->pattern, pspec->pattern_length);
		free(tmp);
	}
	return pspec;
}

void pattern_spec_free(PatternSpec *pspec)
{
	free(pspec->pattern);
	free(pspec);
}

boolean pattern_spec_equal(PatternSpec *pspec1, PatternSpec *pspec2)
{
	return (pspec1->pattern_length == pspec2->pattern_length && pspec1->match_type == pspec2->match_type && stricmp(pspec1->pattern, pspec2->pattern) == 0);
}

boolean pattern_match_string(PatternSpec *pspec, const char *string)
{
	return pattern_match(pspec, strlen(string), string, NULL);
}

boolean pattern_match_simple(const char *pattern, const char *string)
{
	PatternSpec *pspec;
	boolean ergo;

	pspec = pattern_spec_init(pattern);
	ergo = pattern_match(pspec, strlen(string), string, NULL);
	pattern_spec_free(pspec);

	return ergo;
}
