
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "DictIndexUtil.h"

static int compare_str(const void * a, const void * b)
{
	char ** a1 = (char**)a;
	char ** b1 = (char**)b;

	return svn_strcmp(a1[0], b1[0]);
}

static void swap(void *base, size_t i, size_t j, size_t size)
{
    void *tmp = malloc(size);

    (void)memcpy(tmp, (char *)base + i * size, size);
    (void)memmove((char *)base + i * size, (char *)base + j * size, size);
    (void)memcpy((char *)base + j * size, tmp, size);
    free(tmp);
}

/* qsort: sort v[left]...v[right] into increasing order */
void QSort(void *base, size_t nmemb, size_t size, int (*compar) (const void *, const void *))
{
    int i, last;

	if (NULL == compar)	// Use default compare function.
	{
		compar = compare_str;
	}

    if (nmemb <= 1)
        return;

    swap(base, 0, nmemb / 2, size);

    last = 0;
    for (i = 1; i < (int)nmemb; i++)
        if (compar((char *)base + (i * size), base) < 0)
            swap(base, i, ++last, size);

    swap(base, 0, last, size);

    QSort(base, last, size, compar);
    QSort((char *)base + (last + 1) * size, nmemb - last - 1, size, compar);
}
