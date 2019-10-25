#include "DictUtils.h"


#ifndef MAX_INT32
#define MAX_INT32 2147483647
#endif

static __inline int x_casecmp(unsigned char c1, unsigned char c2)
{
   int diff = c1 - c2;
   if (c1 >= 'A' && c1 <= 'Z') {
      diff += 32;
   }
   if (c2 >= 'A' && c2 <= 'Z') {
      diff -= 32;
   }
   return diff;
}

int strnicmp(const char* s1, const char* s2, int n)
{
   if (n > 0) {
      int i = -n;

      s1 += n;
      s2 += n;

      do {
         unsigned char c1 = (unsigned char)s1[i];
         unsigned char c2 = (unsigned char)s2[i];

         int diff = x_casecmp(c1,c2);
         if (diff) {
            return diff;
         }
         if ('\0' == c1) {
            break;
         }
      } while (++i);
   }
   return 0;
}

int stricmp(const char* s1, const char* s2)
{
   return strnicmp(s1, s2, MAX_INT32);
}

// Only for memory leak check in MFC dictionary application.
#ifdef _WIN32
char *strdup(const char * str)
{
  char *new_str;
  int length = 0;

  if (str)
    {
      length = strlen(str) + 1;
      new_str = malloc(sizeof(char) * length);
      memcpy(new_str, str, length);
    }
  else
    new_str = NULL;

  return new_str;
}
#endif


#define UTF8_B0            0xEF
#define UTF8_B1            0xBB
#define UTF8_B2            0xBF

boolean UTF8ToWStr(const byte * pSrc,int nLen, wchar * pDst, int nSize)
{
   byte    b;
   uint16  wChar;

   if(!pSrc || !pDst || nSize <= 0)
      return FALSE;

   if (nLen > 3 && pSrc[0] == UTF8_B0 && pSrc[1] == UTF8_B1 && pSrc[2] == UTF8_B2){
      nLen -= 3;
      pSrc += 3;
   }

   while (nLen > 0) {

      b = *pSrc++;

      if (b & 0x80) {
         if (b & 0x40) {
            if (b & 0x20) {
               wChar = (unsigned short)(b&0x0F);
               b = *pSrc++;
               if ((b & 0xC0) != 0x80)
                  return(FALSE);
               wChar = ((wChar << 6)|(b & 0x3F)) & 0xffff;
               b = *pSrc++;
               if ((b & 0xC0) != 0x80)
                  return(FALSE);
               wChar = ((wChar << 6)|(b & 0x3F)) & 0xffff;
               nLen -= 3;
            }
            else {
               wChar = (unsigned short)(b & 0x1F);
               b = *pSrc++;
               if ((b & 0xc0) != 0x80)
                  return(FALSE);
               wChar = ((wChar << 6)|( b & 0x3F)) & 0xffff;
               nLen -= 2;
            }
         }
         else
            return(FALSE);
      }
      else {
         wChar = (unsigned short)b;
         --nLen;
      }

      if(nSize < sizeof(wchar))
         return(FALSE);

      *pDst = wChar;
      pDst++;
      nSize -= sizeof(wchar);
   }
   return(TRUE);
}

boolean WStrToUTF8(const wchar * pSrc,int nLen, byte * pDst, int nSize)
{
   int      i;
   int      nBytes = 0;
   uint16   wChar;

   if(!pSrc || !pDst || nSize <= 0)
      return FALSE;

   for (i = 0; i < nLen; ++i) {

      wChar = pSrc[i]; 

      if (wChar < 0x80) {
         ++nBytes;
         if (nBytes > nSize)
            break;
         *pDst++ = (byte)(wChar & 0x7F);
      }
      else {
         if(wChar < 0x0800) {
            nBytes += 2;
            if (nBytes > nSize)
               break;
            *pDst++ = (byte)((wChar >> 6) & 0x1F) | 0xC0;
            *pDst++ = (byte)(wChar & 0x3F) | 0x80;
         }
         else {
            nBytes += 3;
            if (nBytes > nSize)
               break;
            *pDst++ = (byte)((wChar >> 12) & 0x0F) | 0xE0;
            *pDst++ = (byte)((wChar >> 6) & 0x3F) | 0x80;
            *pDst++ = (byte)(wChar & 0x3F) | 0x80;
         }
      }
   }
   return(TRUE);
}


#define ACCENT_UPPER_START 192
#define ACCENT_UPPER_END   221
#define ACCENT_LOWER_START 224
#define ACCENT_LOWER_END   253

void WStrLower(wchar * pszDest)
{
   wchar   ch;

   if(pszDest){
      while((ch = *pszDest) != (wchar)0){
         if(ch >= (wchar)'A' && ch <= (wchar)'Z')
            ch += (wchar)('a' - 'A');
         else{
            if(ch >= ACCENT_UPPER_START && ch <= ACCENT_UPPER_END)
               ch += (wchar)(ACCENT_LOWER_START - ACCENT_UPPER_START);
         }
         *pszDest = ch;
         pszDest++;
      }
   }
}

static const char utf8_skip_data[256] = {
  1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
  1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
  1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
  1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
  1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
  1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
  2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
  3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,4,4,4,4,4,4,4,4,5,5,5,5,6,6,1,1
};

const char * const utf8_skip = utf8_skip_data;


long UTF8StrLen (const char *p, long max)
{
  long len = 0;
  const char *start = p;

  if(p == NULL || max == 0)
	  return 0;

  if (max < 0)
    {
      while (*p)
        {
          p = utf8_next_char (p);
          ++len;
        }
    }
  else
    {
      if (max == 0 || !*p)
        return 0;

      p = utf8_next_char (p);

      while (p - start < max && *p)
        {
          ++len;
          p = utf8_next_char (p);
        }

      /* only do the last len increment if we got a complete
       * char (don't count partial chars)
       */
      if (p - start <= max)
        ++len;
    }

  return len;
}

char * UTF8StrReverse (const char *str, int len)
{
  char *r, *result;
  const char *p;

  if (len < 0)
    len = strlen (str);

  result = malloc(sizeof(char) * (len + 1));
  r = result + len;
  p = str;
  while (r > result)
    {
      char *m, skip = utf8_skip[*(unsigned char*) p];
      r -= skip;
      for (m = r; skip; skip--)
        *m++ = *p++;
    }
  result[len] = 0;

  return result;
}
