package com.github.goldin.plugins.gradle.monitor

import com.github.goldin.plugins.gradle.common.BaseSpecification


/**
 * {@link MonitorTask} test spec.
 */
class MonitorTaskSpec extends BaseSpecification
{
    def 'Content matches single matcher' ()
    {
        expect:
        MonitorTask.contentMatches( content, pattern, '' )

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


    def 'Content matches multiple matchers' ()
    {
        expect:
        MonitorTask.contentMatches( content, pattern, '*' )

        where:
        content | pattern
        'aaaaa' | 'a*-b*-c*/a/*-/c/'
        'aaaaa' | '-b*-1234*/^a+$/*'
        'aaaaa' | '/^a{5}$/*-/^b{5}$/*-/\\d/'
        'aaaaa' | '/\\w/*/\\w{5}/'
    }
}
