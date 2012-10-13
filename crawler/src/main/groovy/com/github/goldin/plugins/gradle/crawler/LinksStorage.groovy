package com.github.goldin.plugins.gradle.crawler
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires

import java.util.concurrent.ConcurrentHashMap


/**
 * Links reporting storage
 */
class LinksStorage
{
    private final    Set<String>              processedLinks = [] as Set
    private final    Map<String, String>      brokenLinks    = new ConcurrentHashMap<String, String>()
    private final    Map<String, Set<String>> linksMap       = new ConcurrentHashMap<String, Set<String>>()
    private final    Map<String, Set<String>> newLinksMap    = new ConcurrentHashMap<String, Set<String>>()
    private volatile boolean                  locked         = false


    @Requires({ map && key && value })
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
    String brokenLinkReferrer ( String link )
    {
        assert locked
        brokenLinks[ link ]
    }


    @Ensures({ result >= 0 })
    int processedLinksNumber()
    {
        processedLinks.size()
    }


    @Ensures({ result >= 0 })
    int brokenLinksNumber()
    {
        brokenLinks.size()
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
        updateMap( brokenLinks, brokenLink, referrer )
    }
}
