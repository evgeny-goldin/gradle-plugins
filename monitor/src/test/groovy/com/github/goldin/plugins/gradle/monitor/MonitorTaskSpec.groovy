package com.github.goldin.plugins.gradle.monitor

import com.github.goldin.plugins.gradle.common.BaseSpecification


/**
 * {@link MonitorTask} test spec.
 */
class MonitorTaskSpec extends BaseSpecification
{
    def 'Content matching' ()
    {
        expect:
        MonitorTask.contentMatches( content, pattern )

        where:
        content | pattern
        'aaaaa' | 'a'
        'aaaaa' | 'aa'
        'aaaaa' | 'aaa'
        'aaaaa' | 'aaaa'
        'aaaaa' | 'aaaaa'
        'aaaaa' | '-aaaaaa'
        'aaaaa' | '-b'
        'aaaaa' | '-1234'
        'aaaaa' | '/a/'
        'aaaaa' | '/a+/'
        'aaaaa' | '/^a+$/'
        'aaaaa' | '/^a{5}$/'
        'aaaaa' | '-/^b{5}$/'
        'aaaaa' | '-/\\d/'
        'aaaaa' | '/\\w/'
        'aaaaa' | '/\\w{5}/'
    }
}
