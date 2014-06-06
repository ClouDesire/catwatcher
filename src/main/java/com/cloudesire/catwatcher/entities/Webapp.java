package com.cloudesire.catwatcher.entities;

public class Webapp
{
	private String name;
	private String status;

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

	@Override
	public String toString ()
	{
		return "Webapp [name=" + name + ", status=" + status + "]";
	}

}
