package com.github.goldin.plugins.gradle.node

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Requires


/**
 * Base class for all Node tasks.
 */
abstract class NodeBaseTask extends BaseTask<NodeExtension>
{
    final NodeHelper helper = new NodeHelper()


    @Override
    void verifyExtension( String description )
    {
        assert ext.nodeVersion,  "'nodeVersion' should be defined in $description"
        assert ext.NODE_ENV,     "'NODE_ENV' should be defined in $description"
        assert ext.testCommand,  "'testCommand' should be defined in $description"
        assert ext.startCommand, "'startCommand' should be defined in $description"
    }


    /**
     * Passes a new extensions object to the closure specified.
     * Registers new extension under task's name.
     */
    @Requires({ c })
    void config( Closure c )
    {
        this.extensionName = this.name
        this.ext           = project.extensions.create( this.extensionName, NodeExtension )
        c( this.ext )
    }

    abstract void nodeTaskAction()

    @Override
    final void taskAction()
    {
        verifyGitAvailable()
        setupNode()
        nodeTaskAction()
    }


    private void setupNode()
    {
        final setupScriptTemplate = this.class.classLoader.getResourceAsStream( 'setup-node.sh' ).text
        final nodeVersion         = ( ext.nodeVersion == 'latest' ) ? helper.latestNodeVersion() : ext.nodeVersion
        final setupScript         = setupScriptTemplate.replace( '${nodeVersion}', nodeVersion  ).
                                                        replace( '${NODE_ENV}',    ext.NODE_ENV )

        bashExec( setupScript, "$project.buildDir/${ NodeConstants.SETUP_SCRIPT }" )
    }
}
