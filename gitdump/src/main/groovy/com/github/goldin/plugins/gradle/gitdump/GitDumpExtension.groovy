package com.github.goldin.plugins.gradle.gitdump


class GitDumpExtension
{
    List<String> urls
    File         directory
    String       singleArchiveName

    boolean      useTarGzip          = true
    boolean      useZip              = false
    boolean      singleArchive       = false
    long         singleBackupMaxSize = Long.MAX_VALUE
    long         totalBackupMaxSize  = Long.MAX_VALUE
}
