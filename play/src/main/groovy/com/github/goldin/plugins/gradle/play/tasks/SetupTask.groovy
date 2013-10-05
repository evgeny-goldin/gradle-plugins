package com.github.goldin.plugins.gradle.play.tasks

import org.gcontracts.annotations.Ensures


class SetupTask extends PlayBaseTask
{
    @Override
    void taskAction ()
    {
        runTools([ 'wget --version', "$ext.shell --version", 'unzip -v' ])
        shellExec( setupScript(), baseScript())
    }


    @Ensures ({ result })
    private String setupScript ()
    {
        final playHome    = home( ext.playHome )
        final playZipPath = "$playHome/$ext.playZip"

        """
        |if ! [ -d '$ext.playDirectory' ]; then
        |  mkdir -p '$playHome'
        |  wget  '$ext.playUrl' -O '$playZipPath'
        |  unzip '$playZipPath' -d '$playHome'
        |fi
        |
        |echo "Running Sbt:  [\$($ext.play --version)]"
        |echo "Running Play: [\$(cd / && $ext.play license | grep 'built with Scala' 2>/dev/null)]"
        """.stripMargin().toString().trim()
    }
}
