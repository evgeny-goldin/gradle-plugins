package com.github.goldin.plugins.gradle.node

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import com.github.goldin.plugins.gradle.common.helper.BaseHelper
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


/**
 * Various helper methods for the Node plugin tasks.
 */
class NodeHelper extends BaseHelper<NodeExtension>
{
    /**
     * Retrieves latest Node.js version
     */
    @Ensures({ result })
    String latestNodeVersion(){ latestNodeVersion( httpRequest( NODE_VERSION_URL ).contentAsString()) }


    /**
     * Retrieves latest Node.js version reading the content provided.
     */
    @Requires({ content })
    @Ensures ({ result  })
    String latestNodeVersion( String content )
    {
        content.find( ~/<p>Current Version: (.+?)<\/p>/ ){ it[ 1 ] }
    }
}
