package com.github.goldin.plugins.gradle.node

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.slf4j.Logger
import java.text.DateFormat


/**
 * Various helper methods for the Node plugin tasks.
 */
class NodeHelper
{
    private final Logger logger


    @Requires({ logger })
    @Ensures ({ this.logger })
    NodeHelper ( Logger logger )
    {
        this.logger = logger
    }


    /**
     * Retrieves latest Node.js version
     * @return latest Node.js version
     */
    @Ensures({ result })
    String latestNodeVersion(){ latestNodeVersion( NODE_VERSION_URL.toURL().getText( 'UTF-8' ) )}


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
        final latestVersion = content.find( ~/<p>Current Version: (.+?)<\/p>/ ){ it[ 1 ] }

        if ( logger.infoEnabled )
        {
            logger.info( "Latest Node.js version is [$latestVersion]" )
        }

        latestVersion
    }
}
