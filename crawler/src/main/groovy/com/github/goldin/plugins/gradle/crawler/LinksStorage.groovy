package com.github.goldin.plugins.gradle.crawler

import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires

import static java.lang.Math.min
import static java.lang.Math.max
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.zip.Adler32


/**
 * Links reporting storage
 */
class LinksStorage
{
    private volatile boolean                         locked             = false
    private final    Map<String, Collection<String>> brokenLinks        = new ConcurrentHashMap<>()
    private final    Object                          processedLinksLock = new Object()

    private final CrawlerExtension         ext
    private final Map<String, Set<String>> linksMap
    private final Map<String, Set<String>> newLinksMap
    private final Set<String>              processedLinksS
    private       long[]                   processedLinksL
    private       int                      nextChecksum
    private       long                     minChecksum
    private       long                     maxChecksum


    @Requires({ ext })
    LinksStorage ( CrawlerExtension ext )
    {
        this.ext = ext

        if ( ext.displayLinks )
        {
            this.processedLinksS = new HashSet<>()
        }
        else
        {
            this.processedLinksL = new long[ 1024 ]
            this.nextChecksum    = 0
            this.minChecksum     = Long.MAX_VALUE
            this.maxChecksum     = Long.MIN_VALUE
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
        processedLinksS.asImmutable()
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

        synchronized ( processedLinksLock )
        {
            if ( ext.displayLinks )
            {
                result = links.findAll { processedLinksS.add( it.toString())}.toSet()
            }
            else
            {
                final Map<String, Long> checksums = links.inject([:]){ Map m, String link -> m[ link ] = checksum( link ); m }
                result                            = links.findAll {
                    final checksum = checksums[ it ]
                    if (( checksum >= minChecksum ) && ( checksum <= maxChecksum ))
                    {
                        for( int j = 0; j < nextChecksum; j++ ){ if ( processedLinksL[ j ] == checksum ) { return false }}
                    }
                    minChecksum = min ( minChecksum, checksum )
                    maxChecksum = max ( maxChecksum, checksum )
                    true
                }.toSet()

                if ( result )
                {
                    assert (( nextChecksum + result.size()) < Integer.MAX_VALUE )
                    processedLinksL = ensureCapacity( processedLinksL, nextChecksum + result.size())
                    result.each { processedLinksL[ nextChecksum++ ] = checksums[ it ] }
                }
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
