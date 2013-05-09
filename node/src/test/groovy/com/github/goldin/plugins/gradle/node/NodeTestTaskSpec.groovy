package com.github.goldin.plugins.gradle.node

import com.github.goldin.plugins.gradle.common.BaseSpecification


/**
 * {@link com.github.goldin.plugins.gradle.node.tasks.TestTask} test specification.
 */
class NodeTestTaskSpec extends BaseSpecification
{
    @Delegate final NodeHelper helper = new NodeHelper()

    def 'Latest Node.js version'() {

        expect:
        latestNodeVersion( load( fileName )) == version

        where:
        fileName                   | version
        'node-version-v0.8.17.txt' | 'v0.8.17'
        'node-version-v0.8.18.txt' | 'v0.8.18'
        'node-version-v0.10.5.txt' | 'v0.10.5'
    }
}
