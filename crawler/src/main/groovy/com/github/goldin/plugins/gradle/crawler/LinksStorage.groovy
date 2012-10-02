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
    private volatile boolean                  locked         = false


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
        assert ! ( locked || ( pageUrl.toString() in linksMap.keySet()))
        linksMap[ pageUrl.toString() ] = links
    }


    @Requires({ brokenLink && referrer })
    void addBrokenLink ( String brokenLink, String referrer )
    {
        assert ! ( locked || ( brokenLink.toString() in brokenLinks.keySet()))
        brokenLinks[ brokenLink.toString() ] = referrer
    }
}
