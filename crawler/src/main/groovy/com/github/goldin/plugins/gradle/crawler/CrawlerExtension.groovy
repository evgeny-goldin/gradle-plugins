package com.github.goldin.plugins.gradle.crawler

import java.util.regex.Pattern


/**
 * {@link CrawlerPlugin} extension.
 */
class CrawlerExtension
{
    /**
     * Required properties, verified ib {@link CrawlerTask#verifyAndUpdateExtension()}
     */

    String        baseUrl
    int           threadPoolSize = 5
    int           connectTimeout = 10000
    int           readTimeout    = 10000

    /**
     * Optional properties
     */

    List<String>  nonHtmlExtensions = 'css js gif jpg jpeg png pdf zip rar xml'.tokenize().asImmutable()
    List<String>  rootLinks         = []
    List<String>  cleanupRegexes    = []
    List<String>  ignoredContains   = []
    List<String>  ignoredEndsWith   = []
    List<String>  ignoredRegexes    = []
    boolean       verbose           = false
    boolean       failOnBrokenLinks = false

    /**
     * Internal properties, calculated in {@link CrawlerTask#verifyAndUpdateExtension()}
     */

    String        host
    String        serverAddress
    Pattern       basePattern
    Pattern       linkPattern
    Pattern       relativeLinkPattern = Pattern.compile( 'href="/(.+?)"' )
    List<Pattern> cleanupPatterns     = []
    List<Pattern> ignoredPatterns     = []
}
