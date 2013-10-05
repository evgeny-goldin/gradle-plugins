package com.github.goldin.plugins.gradle.duplicates

import com.github.goldin.plugins.gradle.common.extensions.BaseExtension


class DuplicatesExtension  extends BaseExtension
{
    List<String> configurations  // Default - all configurations are checked
    boolean      fail    = true  // Whether execution should fail when duplicates are found
    boolean      verbose = false // Whether logging is verbose
}
