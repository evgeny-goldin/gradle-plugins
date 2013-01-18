package com.github.goldin.plugins.gradle.node

import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires

import java.text.DateFormat


/**
 * Various helper methods for the Node plugin tasks.
 */
class NodeHelper
{
    /**
     * Retrieves latest Node.js version
     * @return latest Node.js version
     */
    @Ensures({ result })
    String latestNodeVersion(){ latestNodeVersion( 'http://nodejs.org/dist/'.toURL().getText( 'UTF-8' ) )}


    /**
     * Retrieves latest Node.js version reading the content provided.
     *
     * @param content 'http://nodejs.org/dist/' content
     * @return latest Node.js version
     */
    @Requires({ content })
    @Ensures ({ result  })
    String latestNodeVersion( String content )
    {
        // Map: release date => version
        final Map<String, String> dateToVersionMap =
            content.
            // List of Lists, l[0] is Node version, l[1] is version release date
            findAll( />(v.+?)\/<\/a>\s+(\d{2}-\w{3}-\d{4} \d{2}:\d{2})\s+-/ ){ it[ 1 .. 2 ] }.
            inject([:]){ Map m, List l -> m[ l[1] ] = l[0]; m }

        final DateFormat formatter = new java.text.SimpleDateFormat( 'dd-MMM-yyyy HH:mm' )
        final latestDate           = dateToVersionMap.keySet().max{ String date -> formatter.parse( date ).time }
        final latestVersion        = dateToVersionMap[ latestDate ]
        latestVersion
    }
}
