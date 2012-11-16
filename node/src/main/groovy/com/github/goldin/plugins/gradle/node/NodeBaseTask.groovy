package com.github.goldin.plugins.gradle.node

import com.github.goldin.plugins.gradle.common.BaseTask


/**
 * Base class for all Node tasks.
 */
abstract class NodeBaseTask extends BaseTask<NodeExtension>
{
    final NodeHelper helper = new NodeHelper()


    @Override
    NodeExtension verifyExtension( NodeExtension ext, String description )
    {
        assert ext.nodeVersion, "'nodeVersion' should be defined in $description"
        assert ext.NODE_ENV,    "'NODE_ENV' should be defined in $description"
        ext
    }


    abstract void nodeTaskAction()

    @Override
    final void taskAction()
    {
        setupNode()
        nodeTaskAction()
    }


    private void setupNode()
    {
        final ext                 = ext()
        final setupFile           = new File( project.buildDir, 'setup-node.sh' )
        final setupScriptTemplate = this.class.classLoader.getResourceAsStream( 'setup-node.sh' ).text
        final nodeVersion         = ( ext.nodeVersion == 'latest' ) ? helper.latestNodeVersion() : ext.nodeVersion

        assert setupFile.parentFile.with { directory || mkdirs() }
        setupFile.text = setupScriptTemplate.replace( '${nodeVersion}', nodeVersion  ).
                                             replace( '${NODE_ENV}',    ext.NODE_ENV )
        assert setupFile.with { file && size() }

        log { "Running [$setupFile.canonicalPath] .." }
        final output = exec( 'bash', [ setupFile.canonicalPath ] )
        log { output }
    }
}
