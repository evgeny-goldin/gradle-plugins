package com.github.goldin.plugins.gradle.play.tasks

import static com.github.goldin.plugins.gradle.play.PlayConstants.*
import org.gcontracts.annotations.Requires


/**
 * Runs various Grunt operations
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
        final List<String>              destinations = []
        final Map<String, List<String>> coffeeFiles  = [:]
        final Map<String, List<String>> uglifyFiles  = [:]

        for ( Map<String,?> map in ext.grunt )
        {
            final String  source      = map[ 'src' ]
            final String  destination = map[ 'dest' ]
            final boolean minify      = map[ 'minify' ]

            assert source && destination, \
                   "Both 'src' and 'dest' should be defined for each 'grunt' element"

            assert ( source instanceof String ) || ( source instanceof List ), \
                   "Illegal source '${source}' of type '${ source.getClass().name }' - " +
                   "should be of type '${ String.name }' or '${ List.name }'"

            final isJs  = destination.endsWith( '.js' )
            final isCss = destination.endsWith( '.css' )

            assert isJs || isCss, "Illegal grunt destination '${destination}' - should end with either '.js' or '.css'"

            destinations << destination

            isJs? updateJsFiles ( source, destination, minify, coffeeFiles, uglifyFiles ) :
                  updateCssFiles( source, destination )
        }

        final variables = [ packageJson  : PACKAGE_JSON,
                            destinations : destinations,
                            coffeeFiles  : coffeeFiles,
                            uglifyFiles  : uglifyFiles ]

        writeTemplate( GRUNT_FILE, GRUNT_FILE, variables )
    }


    @Requires({ source && destination.endsWith( '.js' ) && ( coffeeFiles != null ) && ( uglifyFiles != null ) })
    private void updateJsFiles ( Object source, String destination, boolean isMinify,
                                 Map<String, List<String>> coffeeFiles,
                                 Map<String, List<String>> uglifyFiles )
    {
        final isCoffeePath = {
            String path ->
            final f = project.file( path )
            f.file ? path.endsWith( DOT_COFFEE ) : f.list().any{ it.endsWith( DOT_COFFEE ) }
        }

        final coffeePath = {
            String path ->
            final f = project.file( path )
            f.file ? ( path.endsWith( DOT_COFFEE ) ? path : null ) : "${ path }${ path.endsWith( '/' ) ? '' : '/' }*${ DOT_COFFEE }"
        }

        final isSourceList = source instanceof List
        final isCoffee     = isSourceList ? source.any{ String s -> isCoffeePath( s )} :
                                            isCoffeePath(( String ) source )
        if ( isCoffee )
        {
            coffeeFiles[ ( destination ) ] = ( isSourceList ? source.collect { String s -> coffeePath( s ) } :
                                                              [ coffeePath(( String ) source ) ] ).grep()
        }

        if ( isMinify )
        {
            final destinationMinified              = destination[ 0 .. -( '.js'.size() + 1 ) ] + '.min.js'
            uglifyFiles[ ( destinationMinified ) ] = [ destination ]
        }
    }


    @Requires({ source && destination.endsWith( '.css' ) })
    private void updateCssFiles ( Object source, String destination )
    {
    }
}
