package com.github.goldin.plugins.gradle.crawler

import static java.lang.Math.max
import static java.lang.Math.min

import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.zip.Adler32
import java.util.zip.Checksum


/**
 * Links reporting storage
 */
class LinksStorage
{
    private volatile boolean                         locked      = false
    private final    Map<String, Collection<String>> brokenLinks = new ConcurrentHashMap<>()
    private final    Object                          linksLock   = new Object()

    private final CrawlerExtension          ext
    private final Map<String, List<String>> linksMap
    private final Map<String, List<String>> newLinksMap
    private final Set<String>               linksSet   // Stores links processed if ext.displayLinks
    private       long[]                    linksArray // Stores checksums of links processed otherwise
    private       int                       nextChecksum
    private       long                      minChecksum
    private       long                      maxChecksum


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
            this.linksArray   = new long[ ext.checksumsChunkSize ]
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
        brokenLinks.size()
    }


    @Ensures({ result != null })
    Set<String> brokenLinks()
    {
        assert locked
        brokenLinks.keySet().asImmutable()
    }


    @Ensures({ result != null })
    Map<String, List<String>> linksMap()
    {
        assert ( locked && ext.linksMapFile )
        linksMap.asImmutable()
    }


    @Ensures({ result != null })
    Map<String, List<String>> newLinksMap()
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
    List<String> addLinksToProcess ( List<String> links )
    {
        assert ( ! locked )

        List<String> newLinks

        final Checksum         ch             = new Adler32()
        final Map<String,Long> linksChecksums = ( Map<String,Long> )( ext.displayLinks ) ?
            null : links.inject([:]){ Map m, String link -> m[ link ] = checksum( ch, link ); m }

        synchronized ( linksLock )
        {
            if ( ext.displayLinks )
            {
                newLinks = links.findAll { linksSet.add( it.toString())}
            }
            else
            {   // New links have new checksums
                newLinks = links.findAll {
                    final long checksum = linksChecksums[ it ]
                    ! (( checksum == minChecksum ) || ( checksum == maxChecksum ) ||
                       (( checksum > minChecksum ) && ( checksum  < maxChecksum ) && contains( linksArray, checksum, nextChecksum )))
                }

                if ( newLinks )
                {
                    assert (( nextChecksum + (( long ) newLinks.size())) <= Integer.MAX_VALUE )
                    ensureLinksArrayCapacity( newLinks.size())

                    newLinks.each {
                        final long checksum          = linksChecksums[ it ]
                        linksArray[ nextChecksum++ ] = checksum
                        minChecksum                  = min ( minChecksum, checksum )
                        maxChecksum                  = max ( maxChecksum, checksum )
                    }
                }

                if (( nextChecksum > 10000 ) && ( ext.checksumsChunkSize < 10000 ))
                {
                    ext.checksumsChunkSize = 10 * 1024
                }

                assert ( nextChecksum <= linksArray.size()) && ( minChecksum <= maxChecksum )
            }
        }

        newLinks
    }


    @Requires({ checksum && s })
    @Ensures({ result > 0 })
    private long checksum( Checksum checksum, String s )
    {
        final bytes = s.getBytes( 'UTF-8' )
        checksum.reset()
        checksum.update( bytes, 0, bytes.length )
        checksum.value
    }


    @Requires({ array && ( endIndex > -1 ) })
    private boolean contains( long[] array, long element, int endIndex )
    {
        for( int j = 0; j < endIndex; j++ ){ if ( array[ j ] == element ) { return true }}
        false
    }


    @Requires({ newElements > 0 })
    @Ensures({ ( nextChecksum + newElements ) <= linksArray.length })
    private void ensureLinksArrayCapacity ( int newElements )
    {
        if (( nextChecksum + newElements ) <= linksArray.length ) { return }

        final newArray  = new long[ linksArray.length + max( ext.checksumsChunkSize, newElements ) ]
        System.arraycopy( linksArray, 0, newArray, 0, nextChecksum )
        this.linksArray = newArray
    }


    @Requires({ pageUrl && ( links != null ) })
    void updateLinksMap ( String pageUrl, List<String> links )
    {
        assert ext.linksMapFile
        updateMap( linksMap, pageUrl, links )
    }


    @Requires({ pageUrl && newLinks })
    void updateNewLinksMap ( String pageUrl, List<String> newLinks )
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
