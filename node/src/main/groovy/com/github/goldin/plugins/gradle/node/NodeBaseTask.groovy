package com.github.goldin.plugins.gradle.node

import com.github.goldin.plugins.gradle.common.BaseTask


/**
 * Base class for all Node tasks.
 */
abstract class NodeBaseTask extends BaseTask<NodeExtension>
{
    final NodeHelper helper = new NodeHelper()

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
        final setupScriptTemplate = this.class.classLoader.getResourceAsStream( 'setup.sh' ).text
        final nodeVersion         = ( ext.nodeVersion == 'latest' ) ? helper.latestNodeVersion() : ext.nodeVersion
        final setupScript         = setupScriptTemplate.replace( '${nodeVersion}', nodeVersion  ).
                                                        replace( '${NODE_ENV}',    ext.NODE_ENV )
        int j = 5
    }


    @Override
    NodeExtension verifyExtension( NodeExtension ext, String description )
    {
        assert ext.nodeVersion, "'nodeVersion' should be defined in $description"
        assert ext.NODE_ENV,    "'NODE_ENV' should be defined in $description"
        ext
    }
}
