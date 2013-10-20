module.exports = ( grunt ) ->
  grunt.initConfig
    pkg: grunt.file.readJSON( '$packageJson' )

# https://github.com/gruntjs/grunt-contrib-clean
    clean:
      build: [ '${ cleanDestinations.join( "', '" ) }' ]

  ${ tasks.join( '\n\n  ' ) }

  grunt.loadNpmTasks 'grunt-contrib-clean'
  grunt.loadNpmTasks 'grunt-contrib-coffee'
  grunt.loadNpmTasks 'grunt-contrib-uglify'
  grunt.loadNpmTasks 'grunt-contrib-less'
  grunt.registerTask 'default', [ '${ taskNames.join( "', '" ) }' ]
