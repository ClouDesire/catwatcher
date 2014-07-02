package com.cloudesire.catwatcher;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudesire.catwatcher.entities.Webapp;
import com.cloudesire.catwatcher.services.TomcatManagerService;
import com.cloudesire.catwatcher.services.impl.TomcatManagerServiceImpl;

public class CatwatcherListener implements ServletContextListener
{
	class Watcher extends Thread
	{
		long sleepTime;
		private final TomcatManagerService catService;

		Watcher(long sleepTime, TomcatManagerService catService)
		{
			this.sleepTime = sleepTime;
			this.catService = catService;
		}

		@Override
		public void run ()
		{
			while (!isInterrupted())
			{
				try
				{
					sleep(sleepTime * 1000);
					List<Webapp> webapps = catService.listStoppedWebapps();
					for (Webapp webapp : webapps)
					{
						catService.startWebapp(webapp);
					}

				} catch (InterruptedException e)
				{
					log.info("Shutdown sequence initiated");
					break;
				} catch (Exception e)
				{
					log.error(
							"TomcatWatchService is not working properly, catwatcher will stop. Problem was: "
									+ e.getMessage(), e);
					break;
				}

			}
		}
	}

	private Watcher watcher;
	private int sleepTime = 180;
	private String username = "";
	private String password = "";
	private String endpoint = "http://localhost:8080";
	private TomcatManagerService catService;
	private static final Logger log = LoggerFactory.getLogger(CatwatcherListener.class);
	private final String DEFAULT_CONFIG_FILE_PATH = "/etc/catwatcher/catwatcher.properties";

	@Override
	public void contextDestroyed ( ServletContextEvent arg0 )
	{
		if (watcher != null)
		{
			watcher.interrupt();
			try
			{
				watcher.join(5000);
			} catch (InterruptedException e)
			{
				log.warn("interrupted while waiting for watcher thread to terminate");
			}
		}
		log.info("Catwatcher service terminated");
	}

	@Override
	public void contextInitialized ( ServletContextEvent arg0 )
	{
		setupTomcatWatcher();
		log.info("Catwatcher service starting for {} with user {} and polling {}", endpoint, username, sleepTime);
		try
		{
			catService = new TomcatManagerServiceImpl(endpoint, username, password);
			watcher = new Watcher(sleepTime, catService);
			watcher.start();
		} catch (MalformedURLException e)
		{
			log.error("invalid endPoint", e);
		}
	}

	public void setEndpoint ( String endpoint )
	{
		this.endpoint = endpoint;
	}

	public void setPassword ( String password )
	{
		this.password = password;
	}

	public void setSleepTime ( int sleepTime )
	{
		this.sleepTime = sleepTime;
	}

	public void setUsername ( String username )
	{
		this.username = username;
	}

	private InputStream findConfigStream ()
	{
		try
		{
			log.info("Looking at external configuration file at " + DEFAULT_CONFIG_FILE_PATH);
			return new FileInputStream(DEFAULT_CONFIG_FILE_PATH);
		} catch (FileNotFoundException e)
		{
			log.warn("Could not find specified configuration file " + DEFAULT_CONFIG_FILE_PATH
					+ " using default configuration..");
			return this.getClass().getClassLoader().getResourceAsStream("catwatcher.properties");
		}
	}

	private void setupTomcatWatcher ()
	{
		try
		{
			try (InputStream configStream = findConfigStream())
			{
				if (configStream == null) return;
				Properties props = new Properties();
				props.load(configStream);
				this.username = props.getProperty("username");
				this.password = props.getProperty("password");
				this.endpoint = props.getProperty("endpoint");
				this.sleepTime = Integer.parseInt(props.getProperty("sleepTime"));
			}
		} catch (IOException e)
		{
			log.error("I/O problems", e);
			return;
		}
		log.info("Catwatcher configurations loaded.");
	}
}
