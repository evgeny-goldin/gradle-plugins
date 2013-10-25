package com.github.goldin.plugins.gradle.common.node

import static com.github.goldin.plugins.gradle.common.CommonConstants.*
import static com.github.goldin.plugins.gradle.common.node.NodeConstants.*
import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.common.helpers.BaseHelper
import com.github.goldin.plugins.gradle.common.helpers.ShellHelper
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.Project


/**
 * Helper for Node.js setup
 */
class NodeSetupHelper extends BaseHelper<NodeBaseExtension>
{
    @Delegate ShellHelper    shellHelper
    @Delegate NpmCacheHelper npmHelper


    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    @Requires({ project && task && ext })
    @Ensures ({ this.project && this.task && this.ext })
    NodeSetupHelper ( Project project, BaseTask task, NodeBaseExtension ext )
    {
        super( project, task, ext )
        shellHelper = new ShellHelper   ( project, task, ext )
        npmHelper   = new NpmCacheHelper( project, task, ext )
    }


    void setupNode( boolean ensureForever )
    {
        runTools( 'git --version', 'tar --version', "$ext.shell --version", 'whoami' )
        restoreNodeModulesFromCache()
        runNodeSetupScript( ensureForever )
        createNodeModulesCache()
    }


    void runNodeSetupScript ( boolean ensureForever )
    {
        final setupScript = getResourceText( 'node-setup.sh', [
            nvmRepo            : NVM_GIT_REPO,
            nvmCommit          : NVM_COMMIT,
            LOG_DELIMITER      : LOG_DELIMITER,
            SCRIPT_LOCATION    : SCRIPT_LOCATION,
            REMOVE_COLOR_CODES : REMOVE_COLOR_CODES,
            nodeVersion        : ext.nodeVersion,
            ensureForever      : ensureForever as String,
            forever            : FOREVER_EXECUTABLE,
            shell              : ext.shell,
            Q                  : Q ])

        shellExec( setupScript, '', scriptFileForTask( 'node-setup' ), false )
    }
}
