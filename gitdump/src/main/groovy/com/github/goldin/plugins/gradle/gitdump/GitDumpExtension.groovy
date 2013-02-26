package com.github.goldin.plugins.gradle.gitdump

import java.util.regex.Pattern


class GitDumpExtension
{
    List<String>  urls
    String        githubUser
    String        githubOrganization
    String        githubPassword
    String        bitbucketUser
    String        bitbucketPassword
    Closure       collectProjects

    File          outputDirectory
    boolean       useZip                  = true
    boolean       bareClone               = true
    boolean       runGitGc                = true
    boolean       runAggressiveGitGc      = false
    boolean       addAbout                = true
    List<String>  cloneFlags              = []
    boolean       singleArchive           = false
    String        singleArchiveName       = 'gitdump'
    long          singleBackupMinSize     = 1L
    long          totalBackupMinSize      = 1L
    long          singleBackupMaxSize     = Long.MAX_VALUE
    long          totalBackupMaxSize      = Long.MAX_VALUE
    final Pattern gitProjectNamePattern   = ~'/([^/]+)\\.git'
    final Pattern gitUrlWithCommitPattern = ~'^(.+\\.git):(.+)$'
    File          aboutFile
}
