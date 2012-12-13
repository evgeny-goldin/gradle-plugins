package com.github.goldin.plugins.gradle.crawler

import static com.github.goldin.plugins.gradle.crawler.CrawlerConstants.*
import org.gcontracts.annotations.Requires
import spock.lang.Specification


/**
 * Regex-related tests
 */
class RegexSpec extends Specification
{

    @Requires({ link && content })
    def "Matching 'externalLinkPattern'"( String link, String content )
    {
        expect:
        content =~ externalLinkPattern
        link    == content.find( externalLinkPattern ){ it[ 2 ] }

        where:
        link | content
        'http://pagead2/show_ads.js' | '<script type="text/javascript" src="http://pagead2/show_ads.js" href="aaaa"></script>'
        'http://pagead2/show_ads.js' | "<script type='text/javascript' href='http://pagead2/show_ads.js'></script>"
    }


    @Requires({ content })
    def "Non-matching 'externalLinkPattern'"( String content )
    {
        expect:
        ! content.find( externalLinkPattern )

        where:
        content << [ '<script type="text/javascript" src="http://pagead2/show_ads.js\'></script>',
                     '<script type="text/javascript" src="http://pagead2/show_ads\'.js\'></script>',
                     '<script type="text/javascript" src="http://pagead2/show_ads\'.js"></script>',
                     "<script type='text/javascript' href='http://pagead2/\"show_ads.js'></script>",
                     "<script type='text/javascript' href='http://pagead2/show_ads.js\"></script>",
                     "<script type='text/javascript' href='http://pagead2/\"show_ads.js\"></script>",
                     "<script type='text/javascript' href='pagead2/show_ads.js\"></script>",
                     "<script type='text/javascript' href='/pagead2/\"show_ads.js\"></script>" ]
    }


    @Requires({ link && content })
    def "Matching 'absoluteLinkPattern'"( String link, String content )
    {
        expect:
        content =~ absoluteLinkPattern
        link    == content.find( absoluteLinkPattern ){ it[ 2 ] }

        where:
        link | content
        '/pagead2/show_ads.js' | '<script type="text/javascript" src="/pagead2/show_ads.js" href="aaaa"></script>'
        '/pagead2/show_ads.js' | "<script type='text/javascript' href='/pagead2/show_ads.js'></script>"
    }


    @Requires({ content })
    def "Non-matching 'absoluteLinkPattern'"( String content )
    {
        expect:
        ! content.find( absoluteLinkPattern )

        where:
        content << [ '<script type="text/javascript" src="http://pagead2/show_ads.js" href="aaaa"></script>',
                     '<script type="text/javascript" src="/pagead2/show_ads\'.js"></script>',
                     "<script type='text/javascript' href='/pagead2/\"show_ads.js'></script>",
                     '<script type="text/javascript" src="pagead2/show_ads"></script>',
                     "<script type='text/javascript' href='/pagead2/show_ads.js\"></script>",
                     "<script type='text/javascript' href=\"/pagead2/show_ads.js'></script>",
                     "<script type='text/javascript' href=\"/pagead2/'show_ads.js'></script>",
                     "<script type='text/javascript' href='/pagead2/\"show_ads.js\"></script>" ]
    }


    @Requires({ link && content })
    def "Matching 'relativeLinkPattern'"( String link, String content )
    {
        expect:
        content =~ relativeLinkPattern
        link    == content.find( relativeLinkPattern ){ it[ 2 ] }

        where:
        link | content
        'pagead2/show_ads.js' | '<script type="text/javascript" src="pagead2/show_ads.js" href="aaaa"></script>'
        'pagead2/show_ads.js' | "<script type='text/javascript' href='pagead2/show_ads.js'></script>"
    }


    @Requires({ content })
    def "Non-matching 'relativeLinkPattern'"( String content )
    {
        expect:
        ! content.find( relativeLinkPattern )

        where:
        content << [ '<script type="text/javascript" src="http://pagead2/show_ads.js"></script>',
                     '<script type="text/javascript" src="pagead2/show_ads\'.js"></script>',
                     "<script type='text/javascript' href='pagead2/\"show_ads.js'></script>",
                     '<script type="text/javascript" src="/pagead2/show_ads"></script>',
                     "<script type='text/javascript' href='pagead2/show_ads.js\"></script>",
                     "<script type='text/javascript' href=\"pagead2/show_ads.js'></script>",
                     "<script type='text/javascript' href=\"pagead2/'show_ads.js'></script>",
                     "<script type='text/javascript' href='pagead2/\"show_ads.js\"></script>" ]
    }
}
