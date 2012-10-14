package com.github.goldin.plugins.gradle.gitdump

import java.util.regex.Pattern


class GitDumpExtension
{
    List<String> urls
    File         outputDirectory
    boolean      useZip                 = true
    boolean      bareClone              = true
    List<String> cloneFlags             = []
    boolean      singleArchive          = false
    String       singleArchiveName      = 'gitdump'
    long         singleBackupMaxSize    = Long.MAX_VALUE
    long         totalBackupMaxSize     = Long.MAX_VALUE
    final Pattern gitProjectNamePattern = Pattern.compile( /\/([^\/]+).git$/ )
}
