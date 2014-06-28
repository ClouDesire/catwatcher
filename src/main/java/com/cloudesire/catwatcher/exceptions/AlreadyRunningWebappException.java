package com.cloudesire.catwatcher.exceptions;

import com.cloudesire.catwatcher.entities.Webapp;

public class AlreadyRunningWebappException extends WebappException
{
    private static final long serialVersionUID = 2398579031909426384L;

    public AlreadyRunningWebappException(Webapp webapp)
    {
        super(webapp, "Webapp already in status RUNNING");
    }
}
