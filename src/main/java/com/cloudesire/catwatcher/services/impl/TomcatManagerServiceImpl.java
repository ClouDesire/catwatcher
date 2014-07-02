package com.cloudesire.catwatcher.services.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudesire.catwatcher.entities.Webapp;
import com.cloudesire.catwatcher.exceptions.AlreadyRunningWebappException;
import com.cloudesire.catwatcher.exceptions.AlreadyStoppedWebappException;
import com.cloudesire.catwatcher.services.TomcatManagerService;
import com.fasterxml.jackson.core.Base64Variants;

public class TomcatManagerServiceImpl implements TomcatManagerService
{
	private static final String MANAGER_PATH = "/manager/text";
	private static final String LIST_PATH = "/list";
	private static final String START_PATH = "/start?path=/";
	private static final String STOP_PATH = "/stop?path=/";
	private static final String UNDEPLOY_PATH = "/undeploy?path=/";
	public static final String STATUS_STOPPED = "stopped";
	public static final String STATUS_RUNNING = "running";

	private final String endpoint;
	private final String username;
	private final String password;

	private HttpClient httpClient;

	private final Logger log = LoggerFactory.getLogger(TomcatManagerServiceImpl.class);

	public TomcatManagerServiceImpl(String endpoint, String username, String password) throws MalformedURLException
	{
		this.username = username;
		this.password = password;
		this.endpoint = endpoint + MANAGER_PATH;
	}

	private URI buildURI (String path) throws MalformedURLException, URISyntaxException
	{
		return new URL(endpoint + path).toURI();
	}

	private HttpGet buildHttpGet (String url) throws MalformedURLException, URISyntaxException
	{
		HttpGet get = new HttpGet(buildURI(url));
		return get;
	}

	@Override
	public List<Webapp> listWebapps () throws Exception
	{
		HttpGet get = buildHttpGet(LIST_PATH);
		setupMethod(get, null);
		HttpResponse response = execute(get);
		return parseResponse(response);
	}

	@Override
	public List<Webapp> listStoppedWebapps () throws Exception
	{
		List<Webapp> webapps = listWebapps();
		filterStopped(webapps);
		return webapps;
	}

	private void filterStopped ( List<Webapp> webapps )
	{
		int i = 0;
		while (i < webapps.size())
		{
			Webapp webapp = webapps.get(i);
			if (webapp.getStatus().equals(STATUS_RUNNING))
			{
				webapps.remove(i);
			}
			else
			{
				i++;
			}
		}
	}

	@Override
	public boolean startWebapp ( Webapp webapp ) throws Exception
	{
		if (webapp.getStatus().equals(STATUS_RUNNING)) throw new AlreadyRunningWebappException(webapp);
		log.info("Starting webapp " + webapp.getName());
		HttpGet get = buildHttpGet(START_PATH + webapp.getName());
		setupMethod(get, null);
		return executeAndCheckIfSuccess(get);
	}

	@Override
	public boolean stopWebapp ( Webapp webapp ) throws Exception
	{
		if (webapp.getStatus().equals(STATUS_STOPPED)) throw new AlreadyStoppedWebappException(webapp);
		log.info("Stopping webapp " + webapp.getName());
		HttpGet get = buildHttpGet(STOP_PATH + webapp.getName());
		setupMethod(get, null);
		return executeAndCheckIfSuccess(get);

	}

	@Override
	public boolean undeployWebapp ( Webapp webapp ) throws Exception
	{
		log.info("undeploying webapp " + webapp.getName());
		HttpGet get = buildHttpGet(UNDEPLOY_PATH + webapp.getName());
		setupMethod(get, null);
		return executeAndCheckIfSuccess(get);
	}

	private void checkError ( HttpResponse response ) throws Exception
	{
		int responseCode = response.getStatusLine().getStatusCode();
		if (responseCode < 200 || responseCode >= 300)
			throw new Exception("RESPONSE: " + responseCode
				+ " - ".concat(response.getStatusLine().getReasonPhrase()));
	}

	private HttpResponse execute ( HttpUriRequest request ) throws ClientProtocolException, IOException, Exception
	{
		log.debug(">>>> " + request.getRequestLine());
		for (Header header : request.getAllHeaders())
		{
			log.trace(">>>> " + header.getName() + ": " + header.getValue());
		}

		HttpResponse response = getHttpClient().execute(request);

		log.debug("<<<< " + response.getStatusLine());
		for (Header header : response.getAllHeaders())
		{
			log.trace("<<<< " + header.getName() + ": " + header.getValue());
		}
		checkError(response);
		return response;
	}

	private synchronized HttpClient getHttpClient () throws KeyManagementException, NoSuchAlgorithmException
	{
		if (httpClient == null)
		{
			HttpClientConnectionManager cm = new BasicHttpClientConnectionManager();
			RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(60).setConnectionRequestTimeout(60).build();
			httpClient = HttpClientBuilder.create().setConnectionManager(cm).setDefaultRequestConfig(requestConfig).build();
		}

		return httpClient;
	}

	private boolean executeAndCheckIfSuccess ( HttpGet get ) throws Exception
	{
		HttpResponse response = execute(get);
		try (InputStream content = response.getEntity().getContent())
		{
			return IOUtils.toString(content, "UTF-8").contains("OK");
		}
	}

	/**
	 * A single line usually consist of 4 token separated by a :
	 *
	 * OK - Listed applications for virtual host localhost /:running:0:ROOT
	 * /host-manager:running:0:/usr/share/tomcat7-admin/host-manager
	 * /manager:running:3:/usr/share/tomcat7-admin/manager
	 *
	 * @param line
	 * @return Webapp
	 */
	private Webapp parseLine ( String line )
	{
		String[] parts = line.split(":");
		if (parts.length != 4) return null;
		Webapp webapp = new Webapp();
		webapp.setPath(parts[0]);
		webapp.setStatus(parts[1]);
		webapp.setActiveSessions(Integer.parseInt(parts[2]));
		webapp.setName(parts[3]);
		return webapp;
	}

	private List<Webapp> parseResponse ( HttpResponse response ) throws IllegalStateException, IOException
	{
		if (response.getEntity() == null) return null;
		try (InputStream content = response.getEntity().getContent())
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(content));
			List<Webapp> webapps = new LinkedList<>();
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				Webapp webapp = null;
				if ((webapp = parseLine(line)) != null)
				{
					webapps.add(webapp);
				}
			}
			return webapps;
		}
	}

	private void setupMethod ( HttpRequest request, Map<String, String> headers )
	{
		if (headers != null)
		{
			for (String k : headers.keySet())
			{
				request.addHeader(k, headers.get(k));
			}
		}
		String authorization = "Basic";
		String encoded = Base64Variants.MIME_NO_LINEFEEDS.encode((username + ":" + password).getBytes());
		authorization = "Basic " + encoded;
		request.addHeader("Authorization", authorization);
	}
}
