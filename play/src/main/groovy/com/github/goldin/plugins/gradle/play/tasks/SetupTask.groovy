package com.github.goldin.plugins.gradle.play.tasks

import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


class SetupTask extends PlayBaseTask
{
    @Override
    void taskAction ()
    {
        runTools([ 'wget --version', "$ext.shell --version", 'unzip -v' ])
        shellExec( startScript(), baseScript( this.name ))
    }


    @Ensures ({ result })
    private String startScript()
    {
        final playHome      = home( '.play' )
        final playDirectory = home( ".play/play-${ext.playVersion}" )
        final play          = "'${ playDirectory }/play'"

        """
        |if ! [ -d '${ playDirectory }' ]; then
        |  mkdir -p '${ playHome }'
        |  wget     '${ ext.playUrl }' -O '${ playHome }'
        |  unzip    '${ playHome }/${ ext.playZip }' -d '${ playHome }'
        |fi
        |echo "Running Play: [\$(cd / && $play license | grep 'built with Scala' 2>/dev/null)]"
        """.stripMargin().toString().trim()
    }
}
