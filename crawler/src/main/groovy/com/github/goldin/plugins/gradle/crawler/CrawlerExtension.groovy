package com.github.goldin.plugins.gradle.crawler

import java.util.regex.Pattern


class CrawlerExtension
{
    final Pattern relativeLinkPattern = Pattern.compile( 'href="/(.+?)"' )
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
    List<String>  nonHtmlContains    = '.css? .js?'.tokenize()
    List<String>  nonHtmlExtensions  = 'css js ico gif jpg jpeg png doc pdf zip rar xml svg flv mp4 mp3 avi'.tokenize()
    List<Integer> ignoredStatusCodes = []
    List<String>  rootLinks          = []
    List<String>  cleanupRegexes     = []
    List<String>  ignoredContains    = []
    List<String>  ignoredEndsWith    = []
    List<String>  ignoredRegexes     = []
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
