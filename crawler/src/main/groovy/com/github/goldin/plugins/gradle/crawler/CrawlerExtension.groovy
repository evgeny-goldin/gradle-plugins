package com.github.goldin.plugins.gradle.crawler

import java.util.regex.Pattern


class CrawlerExtension
{
    // href="http://groovy.codehaus.org/style/custom.css"
    final Pattern externalLinkPattern = Pattern.compile( /(?:src|href)=(?:'|")(https?:\/\/.+?)(?:'|")/ )
    // href="/style/custom.css"
    final Pattern absoluteLinkPattern = Pattern.compile( /(?:src|href)=(?:'|")(\/.+?)(?:'|")/ )
    // href="style/custom.css"
    final Pattern relativeLinkPattern = Pattern.compile( /(?:src|href)=(?:'|")([^\/#][^:]+?)(?:'|")/ )
    // "#anchorName"
    final Pattern anchorPattern       = Pattern.compile( '#.*?$' )

    /**
     * Required properties, verified ib {@link CrawlerTask#verifyAndUpdateExtension()}
     */

    String        baseUrl
    int           threadPoolSize = Runtime.runtime.availableProcessors()
    int           connectTimeout = 10000
    int           readTimeout    = 10000

    /**
     * Optional properties
     */

    File          linksMapFile
    File          newLinksMapFile
    String        userAgent          = 'com.github.goldin.plugins.gradle.crawler'
    int           minimumLinks       = Integer.MIN_VALUE
    long          minimumBytes       = Long.MIN_VALUE
    List<String>  nonHtmlContains    = '.css? .js?'.tokenize()
    List<String>  nonHtmlExtensions  = 'css js ico gif jpg jpeg png doc pdf zip rar gz xml xsl svg flv mp4 mp3 avi mkv'.tokenize()
    List<Integer> ignoredStatusCodes = []
    List<String>  rootLinks          = []
    List<String>  cleanupRegexes     = []
    List<String>  ignoredContains    = []
    List<String>  ignoredEndsWith    = []
    List<String>  ignoredRegexes     = []
    boolean       checkExternalLinks = false
    boolean       checkAbsoluteLinks = true
    boolean       checkRelativeLinks = true
    boolean       displayLinks       = true
    boolean       verbose            = false
    boolean       failOnBrokenLinks  = false

    /**
     * Internal properties, calculated in {@link CrawlerTask#verifyAndUpdateExtension()}
     */

    String        host
    String        serverAddress
    Pattern       basePattern
    Pattern       linkPattern
    List<Pattern> cleanupPatterns
    List<Pattern> ignoredPatterns
}
