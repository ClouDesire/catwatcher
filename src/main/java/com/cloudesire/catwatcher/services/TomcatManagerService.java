package com.cloudesire.catwatcher.services;

import java.util.List;

import com.cloudesire.catwatcher.entities.Webapp;

public interface TomcatManagerService
{
	List<Webapp> listWebapps () throws Exception;

	boolean startWebapp ( Webapp webapp ) throws Exception;

	boolean stopWebapp ( Webapp webapp ) throws Exception;

	boolean undeployWebapp ( Webapp webapp ) throws Exception;
}
