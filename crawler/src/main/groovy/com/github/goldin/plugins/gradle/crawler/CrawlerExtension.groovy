package com.github.goldin.plugins.gradle.crawler

import java.util.regex.Pattern


class CrawlerExtension
{
    // href="http://groovy.codehaus.org/style/custom.css"
    final Pattern externalLinkPattern         = Pattern.compile( /(?:src|href|SRC|HREF)=(?:'|")(https?:\/\/.+?)(?:'|")/ )

    // href="/style/custom.css"
    final Pattern absoluteLinkPattern         = Pattern.compile( /(?:src|href|SRC|HREF)=(?:'|")(\/.+?)(?:'|")/ )

    // href="style/custom.css"
    final Pattern relativeLinkPattern         = Pattern.compile( /(?:src|href|SRC|HREF)=(?:'|")([^\/#'"][^:]+?)(?:'|")/ )

    // "http://path/reminder" => matches "/reminder"
    final Pattern relativeLinkReminderPattern = Pattern.compile( '(?<!(:|:/))/+[^/]*$' )

    /**
     * Internal properties, set in {@link CrawlerTask#verifyAndUpdateExtension()}
     */

    String  rootUrl
    Pattern internalLinkPattern

    /**
     * Other properties, verified in {@link CrawlerTask#verifyAndUpdateExtension()}
     */

    String        baseUrl

    String        userAgent           = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.4 (KHTML, like Gecko) Chrome/22.0.1229.94 Safari/537.4'
    int           threadPoolSize      = Runtime.runtime.availableProcessors()
    int           connectTimeout      = 15000
    int           readTimeout         = 15000
    int           minimumLinks        = -1
    long          minimumBytes        = -1
    int           retries             = 3
    long          retryDelay          = 5000
    long          requestDelay        = 0
    File          linksMapFile        = null
    File          newLinksMapFile     = null
    List<String>  nonHtmlExtensions   = 'css js ico logo gif jpg jpeg png ps eps doc pdf zip jar war ear hpi rar gz xml xsd xsl svg flv swf mp4 mp3 avi mkv'.tokenize()
    List<String>  rootLinks           = []
    List<Closure> pageTransformers    = []
    List<Closure> nonHtmlLinks        = []
    List<Closure> ignoredLinks        = []
    List<Closure> brokenLinkCallbacks = []
    boolean       printSummary        = true
    boolean       checkAbsoluteLinks  = true
    boolean       checkRelativeLinks  = true
    boolean       checkExternalLinks  = false
    boolean       displayLinks        = false
    boolean       failOnBrokenLinks   = false

    List<Integer>                    retryStatusCodes = [ -1, 500 ]
    List<Class<? extends Exception>> retryExceptions  = [ SocketTimeoutException ]
}
