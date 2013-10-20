package com.github.goldin.plugins.gradle.play.tasks

import static com.github.goldin.plugins.gradle.play.PlayConstants.*


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
              version : '0.0.1' ]

        writeTemplate( PACKAGE_JSON, PACKAGE_JSON, variables )
    }


    void generateGruntFile()
    {
        final cleanDestinations = []
        final tasks             = [ 'clean' ]

        for ( Map<String,?> map in ext.grunt )
        {
            final source      = map['src']
            final destination = map['dest']

            assert source && destination, \
                   "Both 'src' and 'dest' should be defined for each 'grunt' element"
            cleanDestinations << destination
        }

        final variables = [ packageJson       : PACKAGE_JSON,
                            cleanDestinations : cleanDestinations,
                            tasks             : tasks ]

        writeTemplate( GRUNT_FILE, GRUNT_FILE, variables )
    }
}
