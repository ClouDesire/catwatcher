package com.cloudesire.catwatcher.entities;

public class Webapp
{
    private String path;
    private String status;
    private Integer activeSessions;
    private String name;

    public String getName ()
    {
        return name;
    }

    public String getStatus ()
    {
        return status;
    }

    public void setName ( String name )
    {
        this.name = name;
    }

    public void setStatus ( String status )
    {
        this.status = status;
    }

    public Integer getActiveSessions ()
    {
        return activeSessions;
    }

    public void setActiveSessions ( Integer activeSessions )
    {
        this.activeSessions = activeSessions;
    }

    public String getPath ()
    {
        return path;
    }

    public void setPath ( String path )
    {
        this.path = path;
    }

    @Override
    public String toString ()
    {
        return "Webapp [name=" + name + ", status=" + status + "]";
    }
}
