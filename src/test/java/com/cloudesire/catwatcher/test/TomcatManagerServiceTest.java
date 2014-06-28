package com.cloudesire.catwatcher.test;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.localserver.LocalTestServer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.cloudesire.catwatcher.entities.Webapp;
import com.cloudesire.catwatcher.services.impl.TomcatManagerServiceImpl;

import static org.junit.Assert.assertTrue;

public class TomcatManagerServiceTest
{

    private String hostName;
    private int port;
    private TomcatManagerServiceImpl tomcatManagerService;

    @Before
    public void setUp () throws Exception
    {
        LocalTestServer server = new LocalTestServer(null, null);
        server.register("/manager/text/*", new HttpRequestHandler()
        {

            @Override
            public void handle ( HttpRequest request, HttpResponse response, HttpContext content )
                    throws HttpException, IOException
            {
                final String uri = request.getRequestLine().getUri();

                if (uri.contains("list"))
                {
                    response.setEntity(new StringEntity("OK - Listed applications for virtual host localhost\n" +
                            "/:running:0:ROOT\n" +
                            "/host-manager:running:0:/usr/share/tomcat7-admin/host-manager\n" +
                            "/manager:running:3:/usr/share/tomcat7-admin/manager\n" +
                            "/good-app:running:4:/var/lib/tomcat7/webapps/good-app\n" +
                            "/bad-app:stopped:0:/var/lib/tomcat7/webapps/bad-app\n"
                            ));
                    response.setStatusCode(200);
                }
                else if (uri.contains("start") || uri.contains("stop") || uri.contains("undeploy"))
                {
                    response.setEntity(new StringEntity("OK - Operation executed"));
                    response.setStatusCode(200);
                }
                else
                {
                    response.setStatusCode(400);
                }
            }
        });
        server.start();
        hostName = server.getServiceAddress().getHostName();
        port = server.getServiceAddress().getPort();

        tomcatManagerService = new TomcatManagerServiceImpl("http://" + hostName + ":" + port + "/manager/text", null, null);
    }

    @After
    public void tearDown () throws Exception
    {
    }

    @Test
    public void testList () throws Exception
    {
        List<Webapp> webapps = tomcatManagerService.listWebapps();
        assertTrue(webapps.size() == 5);

        assertTrue("name=" + webapps.get(0).getPath(), webapps.get(0).getPath().equals("/"));
        assertTrue("name=" + webapps.get(1).getPath(), webapps.get(1).getPath().equals("/host-manager"));
        assertTrue("name=" + webapps.get(2).getPath(), webapps.get(2).getPath().equals("/manager"));

        assertTrue("status=" + webapps.get(0).getStatus(), webapps.get(0).getStatus().equals("running"));
        assertTrue("status=" + webapps.get(1).getStatus(), webapps.get(1).getStatus().equals("running"));
        assertTrue("status=" + webapps.get(2).getStatus(), webapps.get(2).getStatus().equals("running"));

        assertTrue("sessions=" + webapps.get(0).getActiveSessions(), webapps.get(0).getActiveSessions().equals(0));
        assertTrue("sessions=" + webapps.get(1).getActiveSessions(), webapps.get(1).getActiveSessions().equals(0));
        assertTrue("sessions=" + webapps.get(2).getActiveSessions(), webapps.get(2).getActiveSessions().equals(3));
    }

    @Test
    public void testStart () throws Exception
    {
        assertTrue(tomcatManagerService.startWebapp(tomcatManagerService.listWebapps().get(4)));
    }

    @Test
    public void testStop () throws Exception
    {
        assertTrue(tomcatManagerService.stopWebapp(tomcatManagerService.listWebapps().get(3)));
    }

    @Test
    public void testUndeploy () throws Exception
    {
        assertTrue(tomcatManagerService.undeployWebapp(tomcatManagerService.listWebapps().get(3)));
    }

}
