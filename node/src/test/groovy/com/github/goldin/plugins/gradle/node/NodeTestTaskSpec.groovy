package com.github.goldin.plugins.gradle.node

import com.github.goldin.plugins.gradle.common.BaseSpecification
import groovy.util.logging.Slf4j


/**
 * {@link com.github.goldin.plugins.gradle.node.tasks.TestTask} test specification.
 */
@Slf4j
class NodeTestTaskSpec extends BaseSpecification
{
    def 'Latest Node.js version'() {

        expect:
        new NodeHelper( log ).latestNodeVersion( load( fileName )) == version

        where:
        fileName                   | version
        'node-version-v0.8.17.txt' | 'v0.8.17'
        'node-version-v0.8.18.txt' | 'v0.8.18'
    }
}
