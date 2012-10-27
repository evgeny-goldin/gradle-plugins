package com.github.goldin.plugins.gradle.crawler
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.zip.Adler32

import static java.lang.Math.max
import static java.lang.Math.min


/**
 * Links reporting storage
 */
class LinksStorage
{
    private volatile boolean                         locked      = false
    private final    Map<String, Collection<String>> brokenLinks = new ConcurrentHashMap<>()
    private final    Object                          linksLock   = new Object()

    private final CrawlerExtension         ext
    private final Map<String, Set<String>> linksMap
    private final Map<String, Set<String>> newLinksMap
    private final Set<String>              linksSet
    private       long[]                   linksArray
    private       int                      nextChecksum
    private       long                     minChecksum
    private       long                     maxChecksum


    @Requires({ ext })
    LinksStorage ( CrawlerExtension ext )
    {
        this.ext = ext

        if ( ext.displayLinks )
        {
            this.linksSet = new HashSet<>()
        }
        else
        {
            this.linksArray   = new long[ 1024 ]
            this.nextChecksum = 0
            this.minChecksum  = Long.MAX_VALUE
            this.maxChecksum  = Long.MIN_VALUE
        }

        this.linksMap    = ext.linksMapFile    ? new ConcurrentHashMap<>() : null
        this.newLinksMap = ext.newLinksMapFile ? new ConcurrentHashMap<>() : null
    }


    @Requires({ ( map != null ) && key && value })
    private <T> void updateMap( Map<String, T> map, String key, T value )
    {
        assert ( ! ( locked || map.containsKey( key.toString())))
        map[ key.toString() ] = value
    }


    void lock()
    {
        locked = true
    }


    @Ensures({ result != null })
    Set<String> processedLinks()
    {
        assert ( locked && ext.displayLinks )
        linksSet.asImmutable()
    }


    @Ensures({ result >= 0 })
    int brokenLinksNumber()
    {
        assert locked
        brokenLinks.size()
    }


    @Ensures({ result != null })
    Set<String> brokenLinks()
    {
        assert locked
        brokenLinks.keySet().asImmutable()
    }


    @Ensures({ result != null })
    Map<String, Set<String>> linksMap()
    {
        assert ( locked && ext.linksMapFile )
        linksMap.asImmutable()
    }


    @Ensures({ result != null })
    Map<String, Set<String>> newLinksMap()
    {
        assert ( locked && ext.newLinksMapFile )
        newLinksMap.asImmutable()
    }


    @Requires({ link })
    @Ensures({ result })
    Collection<String> brokenLinkReferrers ( String link )
    {
        assert locked
        brokenLinks[ link ]
    }


    @Requires({ links })
    @Ensures({ result != null })
    Set<String> addLinksToProcess ( Collection<String> links )
    {
        assert ( ! locked )

        Set<String> result

        synchronized ( linksLock )
        {
            if ( ext.displayLinks )
            {
                result = links.findAll { linksSet.add( it.toString())}.toSet()
            }
            else
            {
                final Map<String, Long> checksums = links.inject([:]){ Map m, String link -> m[ link ] = checksum( link ); m }
                result                            = links.findAll {
                    final checksum = checksums[ it ]
                    ! (( checksum == minChecksum ) || ( checksum == maxChecksum ) ||
                       (( checksum > minChecksum ) && ( checksum  < maxChecksum ) && contains( linksArray, checksum, nextChecksum )))
                }.toSet()

                if ( result )
                {
                    assert (( nextChecksum + (( long ) result.size())) <= Integer.MAX_VALUE )
                    linksArray = ensureCapacity( linksArray, nextChecksum + result.size())
                    result.each {
                        final checksum               = checksums[ it ]
                        linksArray[ nextChecksum++ ] = checksum
                        minChecksum                  = min ( minChecksum, checksum )
                        maxChecksum                  = max ( maxChecksum, checksum )
                    }
                }

                assert ( nextChecksum <= linksArray.size()) && ( minChecksum <= maxChecksum )
            }
        }

        result
    }


    @Requires({ s })
    private long checksum( String s )
    {
        final checksum = new Adler32()
        checksum.update( s.getBytes( 'UTF-8' ))
        checksum.value
    }


    @Requires({ array && ( endIndex > -1 ) })
    private boolean contains( long[] array, long element, int endIndex )
    {
        for( int j = 0; j < endIndex; j++ ){ if ( array[ j ] == element ) { return true }}
        false
    }


    @Requires({ source && ( size > 0 ) })
    @Ensures({ result.length >= size })
    private long[] ensureCapacity( long[] source, int size )
    {
        if ( size <= source.length ) { return source }

        final newArray = new long[ source.length + max( 1024, ( size - source.length )) ]
        System.arraycopy( source, 0, newArray, 0, source.length )
        newArray
    }


    @Requires({ pageUrl && ( links != null ) })
    void updateLinksMap ( String pageUrl, Set<String> links )
    {
        assert ext.linksMapFile
        updateMap( linksMap, pageUrl, links )
    }


    @Requires({ pageUrl && newLinks })
    void updateNewLinksMap ( String pageUrl, Set<String> newLinks )
    {
        assert ext.newLinksMapFile
        updateMap( newLinksMap, pageUrl, newLinks )
    }


    @Requires({ brokenLink && referrer })
    void addBrokenLink ( String brokenLink, String referrer )
    {
        updateMap( brokenLinks, brokenLink, new ConcurrentLinkedQueue([ referrer ]))
    }


    @Requires({ referrer && ( links != null ) })
    void updateBrokenLinkReferrers( String referrer, Collection<String> links )
    {
        links.each { brokenLinks[ it ]?.add( referrer ) }
    }
}
