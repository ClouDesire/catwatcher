package com.cloudesire.catwatcher.exceptions;

import com.cloudesire.catwatcher.entities.Webapp;

public class AlreadyStoppedWebappException extends WebappException
{
    private static final long serialVersionUID = -8529890322792404937L;

    public AlreadyStoppedWebappException(Webapp webapp)
    {
        super(webapp, "Webapp already in status STOPPED");
    }
}
