package com.cloudesire.catwatcher;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudesire.catwatcher.services.TomcatManagerService;
import com.cloudesire.catwatcher.services.impl.TomcatManagerServiceImpl;

public class CatwatcherMainTest
{
	private static final Logger log = LoggerFactory.getLogger(CatwatcherMainTest.class);

	public static void main ( String[] args ) throws Exception
	{
		TomcatManagerService service = new TomcatManagerServiceImpl("http://localhost:8080/manager/text", "user", "tomcat");
		List<Webapp> listWebapps = service.listWebapps();
		for (Webapp webapp : listWebapps)
		{
			if (webapp != null) log.info(webapp.toString());
			else log.info("webapp was null");
			if (webapp.getName().equals("deployer"))
			{
				service.startWebapp(webapp);
				// service.stopWebapp(webapp);
			}
		}

	}

}
