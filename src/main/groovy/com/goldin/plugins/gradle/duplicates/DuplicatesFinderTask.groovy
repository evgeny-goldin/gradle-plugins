package com.goldin.plugins.gradle.duplicates

import com.goldin.plugins.gradle.util.BaseTask
import org.gradle.api.artifacts.Configuration

/**
 * Serches for duplicates in scopes provided
 */
class DuplicatesFinderTask extends BaseTask
{
    List<String> scopes = null // Default - all scopes are searched


    @Override
    void taskAction ()
    {
       /**
        * Mapping of scope names to their files
        */
        Map<String, List<File>> filesMap = ( Map ) project.configurations.inject( [:] ){
            Map m, Configuration c ->
            if (( ! scopes ) || ( scopes.contains( c.name ))) {
                m[ c.name ] = c.iterator().collect{ it }
            }
            m
        }
    }

}
