package com.github.goldin.plugins.gradle.node

import com.github.goldin.plugins.gradle.common.BaseSpecification

/**
 * {@link NodeTestTask} test specification.
 */
class NodeTestTaskSpec extends BaseSpecification
{
    def 'Latest Node.js version'() {

        expect:
        new NodeHelper().latestNodeVersion( load( fileName )) == version

        where:
        fileName                    | version
        'node-versions-v0.9.3.txt'  | 'v0.9.3'
        'node-versions-v0.8.14.txt' | 'v0.8.14'
    }
}
