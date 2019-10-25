#include "DictZipLib.h"
#include<sys/stat.h>


#define USE_CACHE 1

#define BUFFERSIZE 10240

/* 
 * Output buffer must be greater than or
 * equal to 110% of input buffer size, plus
 * 12 bytes. 
*/
#define OUT_BUFFER_SIZE 0xffffL

#define IN_BUFFER_SIZE ((unsigned long)((double)(OUT_BUFFER_SIZE - 12) * 0.89))

/* For gzip-compatible header, as defined in RFC 1952 */

				/* Magic for GZIP (rfc1952)                */
#define GZ_MAGIC1     0x1f	/* First magic byte                        */
#define GZ_MAGIC2     0x8b	/* Second magic byte                       */

				/* FLaGs (bitmapped), from rfc1952         */
#define GZ_FTEXT      0x01	/* Set for ASCII text                      */
#define GZ_FHCRC      0x02	/* Header CRC16                            */
#define GZ_FEXTRA     0x04	/* Optional field (random access index)    */
#define GZ_FNAME      0x08	/* Original name                           */
#define GZ_COMMENT    0x10	/* Zero-terminated, human-readable comment */
#define GZ_MAX           2	/* Maximum compression                     */
#define GZ_FAST          4	/* Fasted compression                      */

				/* These are from rfc1952                  */
#define GZ_OS_FAT        0	/* FAT filesystem (MS-DOS, OS/2, NT/Win32) */
#define GZ_OS_AMIGA      1	/* Amiga                                   */
#define GZ_OS_VMS        2	/* VMS (or OpenVMS)                        */
#define GZ_OS_UNIX       3      /* Unix                                    */
#define GZ_OS_VMCMS      4      /* VM/CMS                                  */
#define GZ_OS_ATARI      5      /* Atari TOS                               */
#define GZ_OS_HPFS       6      /* HPFS filesystem (OS/2, NT)              */
#define GZ_OS_MAC        7      /* Macintosh                               */
#define GZ_OS_Z          8      /* Z-System                                */
#define GZ_OS_CPM        9      /* CP/M                                    */
#define GZ_OS_TOPS20    10      /* TOPS-20                                 */
#define GZ_OS_NTFS      11      /* NTFS filesystem (NT)                    */
#define GZ_OS_QDOS      12      /* QDOS                                    */
#define GZ_OS_ACORN     13      /* Acorn RISCOS                            */
#define GZ_OS_UNKNOWN  255      /* unknown                                 */

#define GZ_RND_S1       'R'	/* First magic for random access format    */
#define GZ_RND_S2       'A'	/* Second magic for random access format   */

#define GZ_ID1           0	/* GZ_MAGIC1                               */
#define GZ_ID2           1	/* GZ_MAGIC2                               */
#define GZ_CM            2	/* Compression Method (Z_DEFALTED)         */
#define GZ_FLG	         3	/* FLaGs (see above)                       */
#define GZ_MTIME         4	/* Modification TIME                       */
#define GZ_XFL           8	/* eXtra FLags (GZ_MAX or GZ_FAST)         */
#define GZ_OS            9	/* Operating System                        */
#define GZ_XLEN         10	/* eXtra LENgth (16bit)                    */
#define GZ_FEXTRA_START 12	/* Start of extra fields                   */
#define GZ_SI1          12	/* Subfield ID1                            */
#define GZ_SI2          13      /* Subfield ID2                            */
#define GZ_SUBLEN       14	/* Subfield length (16bit)                 */
#define GZ_VERSION      16      /* Version for subfield format             */
#define GZ_CHUNKLEN     18	/* Chunk length (16bit)                    */
#define GZ_CHUNKCNT     20	/* Number of chunks (16bit)                */
#define GZ_RNDDATA      22	/* Random access data (16bit)              */

#define DICT_UNKNOWN    0
#define DICT_TEXT       1
#define DICT_GZIP       2
#define DICT_DZIP       3


static boolean DictZipLib_ReadHeader(DictZipLib * pMe, char * fname)
{
    struct stat stats;
	FILE          *str;
	int           id1, id2, si1, si2;
	char          buffer[BUFFERSIZE];
	int           extraLength, subLength;
	int           i;
	char          *pt;
	int           c;
	unsigned long offset;

	if (!(str = fopen(fname, "rb"))) {
		return FALSE;
	}

	pMe->headerLength = GZ_XLEN - 1;
	pMe->type         = DICT_UNKNOWN;

	id1                  = getc( str );
	id2                  = getc( str );

	if (id1 != GZ_MAGIC1 || id2 != GZ_MAGIC2) {
		pMe->type = DICT_TEXT;

	    if(stat(fname, &stats))
		   return FALSE;

		pMe->compressedLength = pMe->length = stats.st_size;
		pMe->origFilename = fname;
		pMe->mtime = stats.st_mtime;

		fclose( str );

		return TRUE;
	}

	pMe->type = DICT_GZIP;
  
	pMe->method       = getc( str );
	pMe->flags        = getc( str );
	pMe->mtime        = getc( str ) <<  0;
	pMe->mtime       |= getc( str ) <<  8;
	pMe->mtime       |= getc( str ) << 16;
	pMe->mtime       |= getc( str ) << 24;
	pMe->extraFlags   = getc( str );
	pMe->os           = getc( str );
  
	if (pMe->flags & GZ_FEXTRA) {
		extraLength          = getc( str ) << 0;
		extraLength         |= getc( str ) << 8;
		pMe->headerLength   += extraLength + 2;
		si1                  = getc( str );
		si2                  = getc( str );

		if (si1 == GZ_RND_S1 || si2 == GZ_RND_S2) {
			subLength        = getc( str ) << 0;
			subLength       |= getc( str ) << 8;
			pMe->version     = getc( str ) << 0;
			pMe->version    |= getc( str ) << 8;
			
			if (pMe->version != 1) {
				return FALSE; // dzip header version is not supported.
			}

			pMe->chunkLength  = getc( str ) << 0;
			pMe->chunkLength |= getc( str ) << 8;
			pMe->chunkCount   = getc( str ) << 0;
			pMe->chunkCount  |= getc( str ) << 8;
			
			if (pMe->chunkCount <= 0) {
				fclose( str );
				return FALSE;
			}
			pMe->chunks = (int *)malloc(sizeof( pMe->chunks[0] ) * pMe->chunkCount );
			for (i = 0; i < pMe->chunkCount; i++) {
				pMe->chunks[i]  = getc( str ) << 0;
				pMe->chunks[i] |= getc( str ) << 8;
			}
			pMe->type = DICT_DZIP;
		} else {
			fseek( str, pMe->headerLength, SEEK_SET );
		}
	}

	if (pMe->flags & GZ_FNAME) { /* FIXME! Add checking against header len */
		pt = buffer;
		while ((c = getc( str )) && c != EOF)
			*pt++ = c;
		*pt = '\0';

		pMe->origFilename = buffer;
		pMe->headerLength += strlen(pMe->origFilename) + 1;
	} else {
		pMe->origFilename = "";
	}

   if (pMe->flags & GZ_COMMENT) { /* FIXME! Add checking for header len */
      pt = buffer;
      while ((c = getc( str )) && c != EOF)
	  *pt++ = c;
      *pt = '\0';
      pMe->comment = buffer;
      pMe->headerLength += strlen(pMe->comment) + 1;
   } else {
      pMe->comment = "";
   }

   if (pMe->flags & GZ_FHCRC) {
      getc( str );
      getc( str );
      pMe->headerLength += 2;
   }

   if (ftell( str ) != pMe->headerLength + 1) {
      return FALSE; // File position != header length + 1.
   }

   fseek( str, -8, SEEK_END );
   pMe->crc     = getc( str ) <<  0;
   pMe->crc    |= getc( str ) <<  8;
   pMe->crc    |= getc( str ) << 16;
   pMe->crc    |= getc( str ) << 24;
   pMe->length  = getc( str ) <<  0;
   pMe->length |= getc( str ) <<  8;
   pMe->length |= getc( str ) << 16;
   pMe->length |= getc( str ) << 24;
   pMe->compressedLength = ftell( str );

   pMe->offsets = (unsigned long *)malloc( sizeof( pMe->offsets[0] ) * pMe->chunkCount );  // Compute offsets.
   for (offset = pMe->headerLength + 1, i = 0; i < pMe->chunkCount; i++) {
      pMe->offsets[i] = offset;
      offset += pMe->chunks[i];
   }

   fclose( str );
   return TRUE;
}

boolean DictZipLib_Open(DictZipLib * pMe, char * fname)
{
   struct stat stats;
   int         j;

   pMe->initialized = 0;

   for (j = 0; j < DICT_CACHE_SIZE; j++) {
		 pMe->cache[j].chunk    = -1;
		 pMe->cache[j].stamp    = -1;
		 pMe->cache[j].inBuffer = NULL;
		 pMe->cache[j].count    = 0;
   }

   // Check if the file 'fname' exists.
   if(stat(fname, &stats))
      return FALSE;

   if ( !DictZipLib_ReadHeader(pMe, fname) ) {
      return FALSE;	// Not in dzip format.
	}

   pMe->size = stats.st_size;
   if (!MapFile_Open(&pMe->mapfile, fname, pMe->size))
      return FALSE;

   pMe->start = MapFile_Begin(&pMe->mapfile);
   pMe->end = pMe->start + pMe->size;

   return TRUE;
}

static void DictZipLib_Close(DictZipLib * pMe)
{
	int i;   

	if (pMe->chunks)
	{
		free(pMe->chunks);
		pMe->chunks = NULL;
	}

	if (pMe->offsets)
	{
		free(pMe->offsets);
		pMe->offsets = NULL;
	}

	if (pMe->initialized) {
        if (inflateEnd( &pMe->zStream )) {
			// Error.  Cannot shut down inflation engine.
        }
	}

	for (i = 0; i < DICT_CACHE_SIZE; ++i){
		if (pMe->cache[i].inBuffer)
			free (pMe->cache[i].inBuffer);
	}

	MapFile_Close(&pMe->mapfile);
}

void DictZipLib_Read(DictZipLib * pMe, char *buffer, unsigned long start, unsigned long size)
{
	char          *pt;
	unsigned long end;
	int           count;
	char          *inBuffer;
	char          outBuffer[OUT_BUFFER_SIZE];
	int           firstChunk, lastChunk;
	int           firstOffset, lastOffset;
	int           i, j;
	int           found, target, lastStamp;
	static int    stamp = 0;
	
	end  = start + size;
  
	switch (pMe->type) {
	case DICT_GZIP:
		// Error.  Cannot seek on pure gzip format files. Use plain text (for performance) or dzip format (for space savings).
		break;

	case DICT_TEXT:
		memcpy( buffer, pMe->start + start, size );
		break;

	case DICT_DZIP:
		if (!pMe->initialized) {
			++pMe->initialized;
			pMe->zStream.zalloc    = NULL;
			pMe->zStream.zfree     = NULL;
			pMe->zStream.opaque    = NULL;
			pMe->zStream.next_in   = 0;
			pMe->zStream.avail_in  = 0;
			pMe->zStream.next_out  = NULL;
			pMe->zStream.avail_out = 0;
			if (inflateInit2( &pMe->zStream, -15 ) != Z_OK) {
				// Error.  Cannot initialize inflation engine.
			}
		}
		firstChunk  = start / pMe->chunkLength;
		firstOffset = start - firstChunk * pMe->chunkLength;
		lastChunk   = end / pMe->chunkLength;
		lastOffset  = end - lastChunk * pMe->chunkLength;

		for (pt = buffer, i = firstChunk; i <= lastChunk; i++) {

			/* Access cache */
			found  = 0;
			target = 0;
			lastStamp = INT_MAX;
			for (j = 0; j < DICT_CACHE_SIZE; j++) {
#if USE_CACHE
				if (pMe->cache[j].chunk == i) {
					found  = 1;
					target = j;
					break;
				}
#endif
				if (pMe->cache[j].stamp < lastStamp) {
					lastStamp = pMe->cache[j].stamp;
					target = j;
				}
			}
			
			pMe->cache[target].stamp = ++stamp;
			if (found) {
				count = pMe->cache[target].count;
				inBuffer = pMe->cache[target].inBuffer;
			} else {
				pMe->cache[target].chunk = i;
				if (!pMe->cache[target].inBuffer)
					pMe->cache[target].inBuffer = (char *)malloc( IN_BUFFER_SIZE );
				inBuffer = pMe->cache[target].inBuffer;
				
				if (pMe->chunks[i] >= OUT_BUFFER_SIZE ) {
					// Error.
				}

				memcpy( outBuffer, pMe->start + pMe->offsets[i], pMe->chunks[i] );
				
				pMe->zStream.next_in   = (Bytef *)outBuffer;
				pMe->zStream.avail_in  = pMe->chunks[i];
				pMe->zStream.next_out  = (Bytef *)inBuffer;
				pMe->zStream.avail_out = IN_BUFFER_SIZE;
				if (inflate( &pMe->zStream,  Z_PARTIAL_FLUSH ) != Z_OK) {
					// Error.
				}

				if (pMe->zStream.avail_in) {
					// Error. inflate did not flush.
				}

				count = IN_BUFFER_SIZE - pMe->zStream.avail_out;

				pMe->cache[target].count = count;
			}
			
			if (i == firstChunk) {
				if (i == lastChunk) {
					memcpy( pt, inBuffer + firstOffset, lastOffset-firstOffset);
					pt += lastOffset - firstOffset;
				} else {
					if (count != pMe->chunkLength ) {
						// Error.
					}
					memcpy( pt, inBuffer + firstOffset,  pMe->chunkLength - firstOffset );
					pt += pMe->chunkLength - firstOffset;
				}
			} else if (i == lastChunk) {
				memcpy( pt, inBuffer, lastOffset );
				pt += lastOffset;
			} else {
				if( count != pMe->chunkLength ) {
					// Error.
				}
				memcpy( pt, inBuffer, pMe->chunkLength );
				pt += pMe->chunkLength;
			}
		}
		break;

	case DICT_UNKNOWN:
		// Error. Cannot read unknown file type.
		break;
	}
}

void DictZipLib_Release(DictZipLib * pMe)
{
	DictZipLib_Close(pMe);
	free( pMe );
}

boolean DictZipLib_New(DictZipLib ** ppMe)
{
    DictZipLib *pMe = 0; // Declare pointer to this extension.

    // Validate incoming parameters
    if(0 == ppMe)
    {
        return FALSE;
    }

    *ppMe = 0; //Initialize the module pointer to 0.

    // Allocate memory for size of class and function table:
    pMe = (DictZipLib *)malloc( sizeof(DictZipLib) );
	memset((void*)pMe, 0, sizeof(DictZipLib));

    // If there wasn't enough memory left for this extension:
    if(0 == pMe) {
        return FALSE;   //Return "no memory" error.
    }

    pMe->chunks = NULL;
    pMe->offsets = NULL;

    // Fill the pointer that will be output with this extension pointer:
    *ppMe = pMe; 

    return TRUE; 
}

