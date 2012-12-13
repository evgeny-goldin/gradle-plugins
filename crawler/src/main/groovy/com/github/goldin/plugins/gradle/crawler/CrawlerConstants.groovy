package com.github.goldin.plugins.gradle.crawler

import java.util.regex.Pattern


/**
 * {@link CrawlerTask} constants
 */
class CrawlerConstants
{
    // href="http://groovy.codehaus.org/style/custom.css"
    final static Pattern externalLinkPattern         = Pattern.compile( /(?:src|href|SRC|HREF)\s*=\s*('|")(https?:\/\/[^'"]+?)(?:\1)/ )

    // href="/style/custom.css"
    final static Pattern absoluteLinkPattern         = Pattern.compile( /(?:src|href|SRC|HREF)\s*=\s*('|")(\/[^'"]*?)(?:\1)/ )

    // href="style/custom.css"
    final static Pattern relativeLinkPattern         = Pattern.compile( /(?:src|href|SRC|HREF)\s*=\s*('|")([^\/#'"][^:'"]*?)(?:\1)/ )

    // "http://path/reminder" => matches "/reminder"
    final static Pattern relativeLinkReminderPattern = Pattern.compile( '(?<!(:|:/))/+[^/]*$' )

    // "<!-- .. -->"
    final static Pattern htmlCommentPattern          = Pattern.compile( '(?s)<!--(.*?)-->' )

    // "///link"
    final static Pattern slashesPattern              = Pattern.compile( '^/+' )
}
