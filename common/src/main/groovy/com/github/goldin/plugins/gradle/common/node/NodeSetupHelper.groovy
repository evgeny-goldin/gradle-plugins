package com.github.goldin.plugins.gradle.common.node

import static com.github.goldin.plugins.gradle.common.node.NodeConstants.*
import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.common.helpers.BaseHelper
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.Project


/**
 * Helper for Node.js setup
 */
class NodeSetupHelper extends BaseHelper<NodeBaseExtension>
{
    @Delegate NpmCacheHelper npmHelper


    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    @Requires({ project && task && ext })
    @Ensures ({ this.project && this.task && this.ext })
    NodeSetupHelper ( Project project, BaseTask task, NodeBaseExtension ext )
    {
        super( project, task, ext )
        npmHelper = new NpmCacheHelper( project, task, ext )
    }


    void setupNode()
    {
        runTools([ 'git --version', 'tar --version', "$ext.shell --version", 'whoami' ])
        restoreNodeModulesFromCache()
        runNodeSetupScript()
        createNodeModulesCache()
    }


    void runNodeSetupScript ()
    {
        final setupScript = getResourceText( 'node-setup.sh', [
            nvmRepo            : NVM_GIT_REPO,
            nvmCommit          : NVM_COMMIT,
            LOG_DELIMITER      : LOG_DELIMITER,
            SCRIPT_LOCATION    : SCRIPT_LOCATION,
            REMOVE_COLOR_CODES : REMOVE_COLOR_CODES,
            nodeVersion        : ext.nodeVersion,
            ensureForever      : ext.ensureForever as String,
            forever            : FOREVER_EXECUTABLE,
            shell              : ext.shell,
            Q                  : Q ])

        shellExec( setupScript, '', scriptFileForTask( 'node-setup' ), false )
    }
}
