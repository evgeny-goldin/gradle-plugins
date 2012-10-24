package com.github.goldin.plugins.gradle.crawler
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue


/**
 * Links reporting storage
 */
class LinksStorage
{
    private final    Set<String>                     processedLinks = [] as Set
    private final    Map<String, Collection<String>> brokenLinks    = new ConcurrentHashMap<>()
    private final    Map<String, Set<String>>        linksMap       = new ConcurrentHashMap<>()
    private final    Map<String, Set<String>>        newLinksMap    = new ConcurrentHashMap<>()
    private volatile boolean                         locked         = false


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
        assert locked
        processedLinks.asImmutable()
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
        assert locked
        linksMap.asImmutable()
    }


    @Ensures({ result != null })
    Map<String, Set<String>> newLinksMap()
    {
        assert locked
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

        def result

        synchronized ( processedLinks )
        {
            result = links.findAll { processedLinks.add( it.toString())}.toSet()
        }

        result
    }


    @Requires({ pageUrl && ( links != null ) })
    void updateLinksMap ( String pageUrl, Set<String> links )
    {
        updateMap( linksMap, pageUrl, links )
    }


    @Requires({ pageUrl && newLinks })
    void updateNewLinksMap ( String pageUrl, Set<String> newLinks )
    {
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
        for ( link in links )
        {
            brokenLinks[ link ]?.add( referrer )
        }
    }
}
