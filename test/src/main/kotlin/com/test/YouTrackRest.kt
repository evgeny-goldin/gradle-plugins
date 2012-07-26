package com.test

import com.github.goldin.rest.youtrack.YouTrack
import com.github.goldin.rest.youtrack.Issue

class YouTrackRest( youTrackUrl: String )
{
    private val youtrack = YouTrack( youTrackUrl )

    fun issueExists( issueId: String ): Boolean = youtrack.issueExists( issueId )
    fun issue      ( issueId: String ): Issue   = youtrack.issue      ( issueId )
}