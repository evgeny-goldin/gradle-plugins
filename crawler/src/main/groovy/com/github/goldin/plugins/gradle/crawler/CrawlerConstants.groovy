package com.github.goldin.plugins.gradle.crawler

import java.util.regex.Pattern


/**
 * {@link CrawlerTask} constants
 */
class CrawlerConstants
{
    // href="http://groovy.codehaus.org/style/custom.css"
    final static Pattern externalLinkPattern         = ~/(?:src|href|SRC|HREF)\s*=\s*('|")(https?:\/\/[^'"]+?)(?:\1)/

    // href="/style/custom.css"
    final static Pattern absoluteLinkPattern         = ~/(?:src|href|SRC|HREF)\s*=\s*('|")(\/[^'"]*?)(?:\1)/

    // href="style/custom.css"
    final static Pattern relativeLinkPattern         = ~/(?:src|href|SRC|HREF)\s*=\s*('|")([^\/#'"][^:'"]*?)(?:\1)/

    // "http://path/reminder" => matches "/reminder"
    final static Pattern relativeLinkReminderPattern = ~'(?<!(:|:/))/+[^/]*$'

    // "<!-- .. -->"
    final static Pattern htmlCommentPattern          = ~'(?s)<!--(.*?)-->'

    // "///link"
    final static Pattern slashesPattern              = ~'^/+'
}
