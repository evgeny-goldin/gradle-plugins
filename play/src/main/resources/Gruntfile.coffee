module.exports = ( grunt ) ->
  grunt.initConfig
    pkg: grunt.file.readJSON( '$packageJson' )

    # -----------------------------------------------
    # https://github.com/gruntjs/grunt-contrib-clean
    # -----------------------------------------------

    clean:
      build: [ '${ destinations.join( "', '" ) }' ]

    # -----------------------------------------------
    # https://github.com/gruntjs/grunt-contrib-coffee
    # -----------------------------------------------

    coffee:
      compileJoined:
        options:
          join     : true
          sourceMap: true<% if ( coffeeFiles ) { %>
        files:
          <%= renderMap( coffeeFiles ) %>
        <% } %>

    # -----------------------------------------------
    # https://github.com/gruntjs/grunt-contrib-uglify
    # -----------------------------------------------

    uglify:
      options:
        mangle: false<% if ( coffeeFilesMinified ) { %>
      build:
        files:
          <%= renderMap( coffeeFilesMinified ) %>
      <% } %>

    # -----------------------------------------------
    # https://github.com/gruntjs/grunt-contrib-less
    # -----------------------------------------------

    less:
      build:
        options:
          yuicompress: false<% if ( lessFiles ) { %>
        files:
          <%= renderMap( lessFiles ) %>
        <% } %>
      buildMinified:
        options:
          yuicompress: true<% if ( lessFilesMinified ) { %>
        files:
          <%= renderMap( lessFilesMinified ) %>
        <% } %>

  grunt.loadNpmTasks 'grunt-contrib-clean'
  grunt.loadNpmTasks 'grunt-contrib-coffee'
  grunt.loadNpmTasks 'grunt-contrib-uglify'
  grunt.loadNpmTasks 'grunt-contrib-less'
  grunt.registerTask 'default', [ 'clean', 'coffee', 'uglify', 'less' ]

<%
String renderMap( Map m ){ m.collect{ key, value -> "'$key' : [ '${ value.join( "', '" ) }' ]" }.join( '\n          ' )}
%>