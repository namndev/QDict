#ifndef __G_PATTERN_H__
#define __G_PATTERN_H__

#include "DictDef.h"
#include "DictUtils.h"


#ifdef __cplusplus
extern "C" {
#endif

/* keep enum and structure of gpattern.c and patterntest.c in sync */
typedef enum {
	G_MATCH_ALL, /* "*A?A*" */
	G_MATCH_ALL_TAIL, /* "*A?AA" */
	G_MATCH_HEAD, /* "AAAA*" */
	G_MATCH_TAIL, /* "*AAAA" */
	G_MATCH_EXACT, /* "AAAAA" */
	G_MATCH_LAST
} GMatchType;

typedef struct _PatternSpec{
	GMatchType match_type;
	uint32 pattern_length;
	uint32 min_length;
	uint32 max_length;
	char *pattern;
}PatternSpec;


PatternSpec* pattern_spec_init       (const char  *pattern);
void          pattern_spec_free      (PatternSpec *pspec);
boolean      pattern_spec_equal     (PatternSpec *pspec1, PatternSpec *pspec2);
boolean      pattern_match          (PatternSpec *pspec, uint32 string_length, const char  *string, const char  *string_reversed);
boolean      pattern_match_string   (PatternSpec *pspec, const char  *string);
boolean      pattern_match_simple   (const char  *pattern, const char  *string);

#ifdef __cplusplus
}
#endif

#endif /* __G_PATTERN_H__ */
