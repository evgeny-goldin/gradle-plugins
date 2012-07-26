package com.test;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;


/**
 * Tests {@link YouTrackRest} implemented in Kotlin.
 */
public class TestKotlin
{

    @Test
    public void testYouTrackIssueExists()
    {
        assertTrue  ( new YouTrackRest( "http://rest-clients.myjetbrains.com/youtrack/" ).
                      issueExists( "pl-359" ));
        assertEquals( "Fixed",
                      new YouTrackRest( "http://evgeny-goldin.org/youtrack/" ).
                      issue( "pl-645" ).getCustomField( "State" ) );
    }
}
