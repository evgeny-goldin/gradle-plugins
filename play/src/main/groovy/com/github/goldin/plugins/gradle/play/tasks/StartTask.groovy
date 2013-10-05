package com.github.goldin.plugins.gradle.play.tasks


class StartTask extends PlayBaseTask
{
    @Override
    void taskAction()
    {
        log{ 'Start task' }
    }
}
