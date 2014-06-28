package com.cloudesire.catwatcher.exceptions;

import com.cloudesire.catwatcher.entities.Webapp;

public class WebappException extends Exception
{
    private static final long serialVersionUID = 1598675602707478671L;

    protected Webapp webapp;

    public WebappException(Webapp webapp, String message)
    {
        super(message);
        this.webapp = webapp;
    }
}
