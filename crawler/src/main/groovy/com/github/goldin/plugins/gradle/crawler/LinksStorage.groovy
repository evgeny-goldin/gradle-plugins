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
    private final Map<String, List<String>> linksPathMap
    private final Map<String, List<String>> newLinksMap
    private final Set<String>               linksSet       // Stores links processed if ext.displayLinks is true
    private       long[]                    checksumsArray // Stores checksums of links processed if ext.displayLinks is false
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
            this.checksumsArray = new long[ ext.checksumsChunkSize ]
            this.nextChecksum   = 0
            this.minChecksum    = Long.MAX_VALUE
            this.maxChecksum    = Long.MIN_VALUE
        }

        this.linksMap     = ext.linksMapFile     ? new ConcurrentHashMap<>() : null
        this.linksPathMap = ext.displayLinksPath ? new ConcurrentHashMap<>() : null
        this.newLinksMap  = ext.newLinksMapFile  ? new ConcurrentHashMap<>() : null
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


    @Requires({ link })
    @Ensures ({ result })
    List<String> linkPath( String link )
    {
        assert ( locked && ext.displayLinksPath )
        linksPathMap[ link ].asImmutable()
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


    @Requires({ ( parentLink != null ) && links })
    @Ensures({ result != null })
    List<String> addLinksToProcess ( String parentLink, List<String> links )
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
                    ! (( checksum == minChecksum ) ||
                       ( checksum == maxChecksum ) ||
                       (( checksum > minChecksum ) && ( checksum  < maxChecksum ) && contains( checksumsArray, checksum, nextChecksum )))
                }

                if ( newLinks )
                {
                    assert (( nextChecksum + (( long ) newLinks.size())) <= Integer.MAX_VALUE )
                    ensureLinksArrayCapacity( newLinks.size())

                    newLinks.each {
                        final long checksum              = linksChecksums[ it ]
                        checksumsArray[ nextChecksum++ ] = checksum
                        minChecksum                      = min ( minChecksum, checksum )
                        maxChecksum                      = max ( maxChecksum, checksum )
                    }
                }

                if (( nextChecksum > 10000 ) && ( ext.checksumsChunkSize < 10000 ))
                {
                    ext.checksumsChunkSize = 10 * 1024
                }

                assert ( nextChecksum <= checksumsArray.size()) && ( minChecksum <= maxChecksum )
            }

            if ( ext.displayLinksPath )
            {
                assert ( linksPathMap.containsKey( parentLink ) || ( ! parentLink ))
                newLinks.each {
                    linksPathMap[ it.intern() ] = linksPathMap.containsKey( parentLink ) ? linksPathMap[ parentLink ] + parentLink.intern() : []
                }
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
    @Ensures({ ( nextChecksum + newElements ) <= checksumsArray.length })
    private void ensureLinksArrayCapacity ( int newElements )
    {
        if (( nextChecksum + newElements ) <= checksumsArray.length ) { return }

        final newArray  = new long[ checksumsArray.length + max( ext.checksumsChunkSize, newElements ) ]
        System.arraycopy( checksumsArray, 0, newArray, 0, nextChecksum )
        this.checksumsArray = newArray
    }


    @Requires({ pageUrl && ( links != null ) })
    void updateLinksMap ( String pageUrl, List<String> links )
    {
        assert ext.linksMapFile
        updateMap( linksMap, pageUrl.intern(), links*.intern())
    }


    @Requires({ pageUrl && newLinks })
    void updateNewLinksMap ( String pageUrl, List<String> newLinks )
    {
        assert ext.newLinksMapFile
        updateMap( newLinksMap, pageUrl.intern(), newLinks*.intern())
    }


    @Requires({ brokenLink && referrerUrl })
    void addBrokenLink ( String brokenLink, String referrerUrl )
    {
        updateMap( brokenLinks, brokenLink.intern(), new ConcurrentLinkedQueue([ referrerUrl.intern() ]))
    }


    @Requires({ referrerUrl && ( links != null ) })
    void updateBrokenLinkReferrers( String referrerUrl, Collection<String> links )
    {
        links.each { brokenLinks[ it ]?.add( referrerUrl ) }
    }
}
