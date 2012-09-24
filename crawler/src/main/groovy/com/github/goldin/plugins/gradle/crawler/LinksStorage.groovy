package com.github.goldin.plugins.gradle.crawler

import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires

import java.util.concurrent.ConcurrentHashMap


/**
 * Links reporting storage
 */
class LinksStorage
{
    private final    Set<String>         processedLinks = [] as Set
    private final    Map<String, String> brokenLinks    = new ConcurrentHashMap<String, String>()
    private volatile boolean             locked         = false


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
    List<String> addLinksToProcess ( Collection<String> links )
    {
        assert ( ! locked )

        synchronized ( processedLinks )
        {
            links.findAll { processedLinks.add( it.toString())}  /* To convert possible GStrings */
        }
    }


    @Requires({ link && referrer })
    @Ensures({ link in brokenLinks.keySet() })
    void addBrokenLink ( String link, String referrer )
    {
        assert ( ! locked )
        brokenLinks[ link.toString() ] = referrer  /* To convert possible GStrings */
    }
}
