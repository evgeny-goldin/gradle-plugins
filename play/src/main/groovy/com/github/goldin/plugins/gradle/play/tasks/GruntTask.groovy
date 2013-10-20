package com.github.goldin.plugins.gradle.play.tasks

import static com.github.goldin.plugins.gradle.play.PlayConstants.*
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


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


    private void generatePackageJson()
    {
        final variables = ( Map<String,?> ) ext.versions.collectEntries { String key, String value ->
            [ key.replace( '-', '_' ), value ]
        } + [ name    : project.name,
              version : '0.0.1' ]

        writeTemplate( PACKAGE_JSON, PACKAGE_JSON, variables )
    }


    private void generateGruntFile()
    {
        final cleanDestinations = []
        final tasks             = []
        final taskNames         = [ 'clean' ]
        int counter             = 1

        for ( Map<String,?> map in ext.grunt )
        {
            final String source      = map[ 'src' ]
            final String destination = map[ 'dest' ]

            assert source && destination, \
                   "Both 'src' and 'dest' should be defined for each 'grunt' element"
            cleanDestinations << destination

            final isJs        = destination.endsWith( '.js' )
            final isMinifyJs  = destination.endsWith( '.min.js' )
            final isCss       = destination.endsWith( '.css' )
            final isMinifyCss = destination.endsWith( '.min.css' )

            assert isJs || isCss, "Illegal grunt destination '${destination}' - should end with '.js' or '.css'"

            tasks << this."${ isJs ? 'jsTask' : 'cssTask' }"( source, destination, isJs ? isMinifyJs : isMinifyCss, counter++ )
        }

        final variables = [ packageJson       : PACKAGE_JSON,
                            cleanDestinations : cleanDestinations,
                            tasks             : tasks,
                            taskNames         : taskNames ]

        writeTemplate( GRUNT_FILE, GRUNT_FILE, variables )
    }


    @Requires({ source && destination && ( taskCounter > 0 ) })
    @Ensures ({ result })
    private String jsTask( Object source, String destination, boolean isMinify, int taskCounter )
    {
        """
        |js:
        |   ccc
        """.stripMargin().trim()
    }


    @Requires({ source && destination && ( taskCounter > 0 ) })
    @Ensures ({ result })
    private String cssTask( Object source, String destination, boolean isMinify, int taskCounter )
    {
        """
        |css:
        |   ccc
        """.stripMargin().trim()
    }
}
