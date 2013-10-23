package com.github.goldin.plugins.gradle.common.node

import com.github.goldin.plugins.gradle.common.extensions.ShellExtension


/**
 * Base class for extension describing plugin running Node.js tasks
 */
class NodeBaseExtension extends ShellExtension
{
    boolean npmLocalCache   = true  // Whether results of 'npm install' are cached locally
    String  npmRemoteCache          // Remote repo URL for storing 'npm install' cache archives
    String  nodeVersion     = 'latest'
    boolean ensureForever   = true  // Whether 'forever' should be installed even if it doesn't appear in 'package.json'
    boolean npmCleanInstall = false // Internal property, whether 'npm install' was run on a clean directory
}
