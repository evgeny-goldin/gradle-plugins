package com.github.goldin.plugins.gradle.play.tasks


/**
 *
 */
class GruntTask extends PlayBaseTask
{
    @Override
    void taskAction()
    {
        generatePackageJson()
        generateGruntFile()
    }


    void generatePackageJson()
    {
        final variables = ( Map<String,?> ) ext.versions.collectEntries { String key, String value ->
            [ key.replace( '-', '_' ), value ]
        } + [ name    : project.name,
              version : project.version ]

        write( project.file( 'package.json' ),
               renderTemplate( getResourceText( 'package.json' ), variables ))
    }


    void generateGruntFile()
    {

    }
}
