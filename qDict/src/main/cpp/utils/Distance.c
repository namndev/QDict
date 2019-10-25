#include "Distance.h"


static int minimum( const int a, const int b, const int c )
{
	int min = a;
	if (b < min )
		min = b;
	if (c < min )
		min = c;
	return min;
}

#define COVER_TRANSPOSITION
//#define OPTIMIZE_ED

#ifdef OPTIMIZE_ED
/* Compute levenshtein distance between s and t, this is using QUICK algorithm */
int CalEditDistance(const wchar *s, const wchar *t, const int limit, int * d)
{
    int n = 0, m = 0, iLenDif, k, i, j, cost;

    // Remove leftmost matching portion of strings
    while ( *s && (*s==*t) )
    {
        s++;
		t++;
    }

	while (s[n])
	{
		n++;
	}
	while (t[m])
	{
		m++;
	}

    // Remove rightmost matching portion of strings by decrement n and m.
    while ( n && m && (*(s+n-1)==*(t+m-1)) )
    {
        n--;m--;
    }
    if ( m==0 || n==0 || d==(int*)0 )
        return (m+n);
    if ( m < n )
    {
        const wchar * temp = s;
        int itemp = n;
        s = t;
        t = temp;
        n = m;
        m = itemp;
    }
    iLenDif = m - n;
    if ( iLenDif >= limit )
        return iLenDif;
    // step 1
    n++;m++;

    if ((int*)0 == d)
        return (m+n);
    // step 2, init matrix
    for (k=0;k<n;k++)
        d[k] = k;
    for (k=1;k<m;k++)
        d[k*n] = k;
    // step 3
    for (i=1;i<n;i++)
    {
        // first calculate column, d(i,j)
        for ( j=1; j<iLenDif+i; j++ )
        {
            cost = s[i-1]==t[j-1]?0:1;
            d[j*n+i] = minimum(d[(j-1)*n+i]+1,d[j*n+i-1]+1,d[(j-1)*n+i-1]+cost);
#ifdef COVER_TRANSPOSITION
            if ( i>=2 && j>=2 && (d[j*n+i]-d[(j-2)*n+i-2]==2)
                 && (s[i-2]==t[j-1]) && (s[i-1]==t[j-2]) )
                d[j*n+i]--;
#endif
        }
        // second calculate row, d(k,j)
        // now j==iLenDif+i;
        for ( k=1;k<=i;k++ )
        {
            cost = s[k-1]==t[j-1]?0:1;
            d[j*n+k] = minimum(d[(j-1)*n+k]+1,d[j*n+k-1]+1,d[(j-1)*n+k-1]+cost);
#ifdef COVER_TRANSPOSITION
            if ( k>=2 && j>=2 && (d[j*n+k]-d[(j-2)*n+k-2]==2)
                 && (s[k-2]==t[j-1]) && (s[k-1]==t[j-2]) )
                d[j*n+k]--;
#endif
        }
        // test if d(i,j) limit gets equal or exceed
        if ( d[j*n+i] >= limit )
        {
        	free(d);
            return d[j*n+i];
        }
    }

    free(d);
    // d(n-1,m-1)
    return d[n*m-1];
}
#else
int CalEditDistance(const wchar *s, const wchar *t, const int limit, int * d)
{
    int n = 0,m = 0,k,i,j,cost;
	int dis = 0;

    //Step 1
	while (s[n])
	{
		n++;
	}
	while (t[m])
	{
		m++;
	}

    if ( (int*)0 == d )
        return (m+n);

    if( n!=0 && m!=0 && d!=(int*)0 )
    {
        m++;n++;
        //Step 2
        for(k=0;k<n;k++)
            d[k]=k;
        for(k=0;k<m;k++)
            d[k*n]=k;
        //Step 3 and 4
        for(i=1;i<n;i++)
            for(j=1;j<m;j++)
            {
                //Step 5
                if(s[i-1]==t[j-1])
                    cost=0;
                else
                    cost=1;
                //Step 6
                d[j*n+i]=minimum(d[(j-1)*n+i]+1,d[j*n+i-1]+1,d[(j-1)*n+i-1]+cost);
#ifdef COVER_TRANSPOSITION
                if ( i>=2 && j>=2 && (d[j*n+i]-d[(j-2)*n+i-2]==2)
                     && (s[i-2]==t[j-1]) && (s[i-1]==t[j-2]) )
                    d[j*n+i]--;
#endif
            }

		dis = d[n*m-1];
        //free(d);
        return dis;
    }
    else
    {
    	//free(d);
        return (n+m);
    }
}
#endif
