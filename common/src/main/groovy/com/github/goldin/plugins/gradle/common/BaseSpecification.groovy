package com.github.goldin.plugins.gradle.common

import org.gcontracts.annotations.Requires
import spock.lang.Specification


/**
 * Base class for all Spock specifications.
 */
class BaseSpecification extends Specification
{

    @Requires({ resourcePath })
    String load( String resourcePath )
    {
        final  inputStream = this.class.classLoader.getResourceAsStream( resourcePath )
        assert inputStream, "Unable to load resource [$resourcePath]"
        inputStream.text
    }
}
