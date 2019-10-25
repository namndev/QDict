#ifndef __DICT_DEF_H__
#define __DICT_DEF_H__

#include <stdio.h>
#include <stdlib.h>
#include "utils/mylog.h"


#ifdef _WIN32
#include <vld.h>
#pragma warning(disable:4996)
#pragma warning(disable:4273)
#pragma warning(disable:4028)
#endif

#ifndef _WIN32
enum { FALSE, TRUE };
#endif

#ifndef NULL
#define NULL  0
#endif

#define MAX_UINT32 4294967295u
#define MAX_UINT16 65535
#define MAX_UINT8 255

typedef  unsigned char  byte;			/* Unsigned 8  bit value type. */
typedef  unsigned short word;			/* Unsigned 16 bit value type. */
typedef  unsigned int   dword;			/* Unsigned 32 bit value type. */

typedef  unsigned char  uint8;			/* Unsigned 8  bit value type. */
typedef  unsigned short uint16;			/* Unsigned 16 bit value type. */
typedef  unsigned int   uint32;			/* Unsigned 32 bit value type. */

typedef  signed char    int8;			/* Signed 8  bit value type. */
typedef  signed short   int16;			/* Signed 16 bit value type. */
typedef  signed int     int32;			/* Unsigned 32 bit value type. */

typedef  unsigned char  boolean;		/* Boolean value type. */

typedef  uint16         wchar;


#endif//!__DICT_DEF_H__
