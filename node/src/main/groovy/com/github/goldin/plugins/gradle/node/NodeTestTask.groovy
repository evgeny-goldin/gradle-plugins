package com.github.goldin.plugins.gradle.node

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures

import java.text.DateFormat


/**
 * Tests Node.js application.
 */
class NodeTestTask extends BaseTask<NodeExtension>
{
    @Override
    void taskAction ( )
    {
    }


    @Ensures({ result })
    String latestNodeVersion()
    {
        final DateFormat          formatter = new java.text.SimpleDateFormat( 'dd-MMM-yyyy HH:mm' )
        final Map<String, String> map       =
            'http://nodejs.org/dist/'.toURL().text.
            findAll( />(v.+?)\/<\/a>\s+(\d{2}-\w{3}-\d{4} \d{2}:\d{2})\s+-/ ){ it[ 1 .. 2 ] }. // List of Lists, l[0] is Node version, l[1] is version release date
            inject([:]){ Map m, List l -> m[ l[1]] = l[0]; m }                                 // Map: release date => version

        final maxDate = map.keySet().max{ String date -> formatter.parse( date ).time }
        map[ maxDate ]
    }
}
