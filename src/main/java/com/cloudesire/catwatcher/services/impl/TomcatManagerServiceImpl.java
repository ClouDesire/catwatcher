package com.cloudesire.catwatcher.services.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudesire.catwatcher.entities.Webapp;
import com.cloudesire.catwatcher.services.TomcatManagerService;
import com.fasterxml.jackson.core.Base64Variants;

public class TomcatManagerServiceImpl implements TomcatManagerService
{
	private final String STATUS_STOPPED = "stopped";
	private final String STATUS_RUNNING = "running";

	private final String endpoint;
	private final String username;
	private final String password;
	private HttpClient httpClient;
	private SSLContext ctx;
	private final Logger log = LoggerFactory.getLogger(TomcatManagerServiceImpl.class);

	public TomcatManagerServiceImpl(String endpoint, String username, String password) throws MalformedURLException
	{
		this.username = username;
		this.password = password;
		this.endpoint = endpoint;
	}

	@Override
	public List<Webapp> listWebapps () throws Exception
	{
		URL url = new URL(endpoint + "/list");
		HttpGet get = new HttpGet(url.toURI());
		setupMethod(get, null);
		HttpResponse response = execute(get);
		return parseResponse(response);
	}

	@Override
	public boolean startWebapp ( Webapp webapp ) throws Exception
	{
		if (webapp.getStatus().equals(STATUS_RUNNING)) return true;
		log.info("Starting webapp " + webapp.getName());
		URL url = new URL(endpoint + "/start?path=/" + webapp.getName());
		HttpGet get = new HttpGet(url.toURI());
		setupMethod(get, null);
		return handleResult(get);
	}

	@Override
	public boolean stopWebapp ( Webapp webapp ) throws Exception
	{
		if (webapp.getStatus().equals(STATUS_STOPPED)) return true;
		log.info("Stopping webapp " + webapp.getName());
		URL url = new URL(endpoint + "/stop?path=/" + webapp.getName());
		HttpGet get = new HttpGet(url.toURI());
		setupMethod(get, null);
		return handleResult(get);

	}

	@Override
	public boolean undeployWebapp ( Webapp webapp ) throws Exception
	{
		log.info("undeploying webapp " + webapp.getName());
		URL url = new URL(endpoint + "/undeploy?path=/" + webapp.getName());
		HttpGet get = new HttpGet(url.toURI());
		setupMethod(get, null);
		return handleResult(get);
	}

	private void checkError ( HttpResponse response ) throws Exception
	{
		int responseCode = response.getStatusLine().getStatusCode();
		if (responseCode < 200 || responseCode >= 300) throw new Exception("RESPONSE: " + responseCode
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
			PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
			cm.closeIdleConnections(30, TimeUnit.SECONDS);
			httpClient = new DefaultHttpClient(cm);
			HttpParams params = httpClient.getParams();
			params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000);
			params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 60000);

			log.debug("Configuring HTTPS with no validation!");
			SSLSocketFactory sf = new SSLSocketFactory(getSSLContext(), new AllowAllHostnameVerifier());
			Scheme https = new Scheme("https", 443, sf);
			httpClient.getConnectionManager().getSchemeRegistry().register(https);

		}

		return httpClient;
	}

	private SSLContext getSSLContext () throws NoSuchAlgorithmException, KeyManagementException
	{
		if (ctx != null) return ctx;
		log.debug("Creating SSL context with no certificate validation");
		ctx = SSLContext.getInstance("SSL");
		TrustManager tm = new X509TrustManager()
		{

			@Override
			public void checkClientTrusted ( X509Certificate[] arg0, String arg1 ) throws CertificateException
			{

			}

			@Override
			public void checkServerTrusted ( X509Certificate[] arg0, String arg1 ) throws CertificateException
			{

			}

			@Override
			public X509Certificate[] getAcceptedIssuers ()
			{
				return null;
			}
		};
		ctx.init(null, new TrustManager[] { tm }, new SecureRandom());
		return ctx;
	}

	private boolean handleResult ( HttpGet get ) throws Exception, IOException
	{
		HttpResponse response = execute(get);
		try (InputStream content = response.getEntity().getContent())
		{
			return IOUtils.toString(content, "UTF-8").contains("OK");
		}
	}

	private Webapp parseLine ( String line )
	{
		String[] parts = line.split(":");
		if (parts.length != 4) return null;
		Webapp webapp = new Webapp();
		webapp.setName(parts[3]);
		webapp.setStatus(parts[1]);
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
