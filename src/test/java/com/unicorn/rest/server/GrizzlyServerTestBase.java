package com.unicorn.rest.server;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.TracingConfig;
import org.junit.AfterClass;

import com.unicorn.rest.server.GrizzlyServerOrchestrator;
import com.unicorn.rest.server.PropertiesParser;
import com.unicorn.rest.utils.JSONObjectMapperImpl;

public class GrizzlyServerTestBase {

    private static final String CLIENT_TRUSTORE_FILE_PROPERTY = "CLIENT_TRUSTORE_FILE";
    private static final String CLIENT_TRUSTORE_PASSWORD_PROPERTY = "CLIENT_TRUSTORE_PASSWORD";
    public static final String DEFAULT_HTTPS_SERVER_PROPERTIES_FILE = "test-https-server.properties";
    
    protected static HttpServer grizzlyWebServer;
    protected static Client  client;
    protected static URI uri;
    
    protected static void setUpHttpsWebServer(AbstractBinder... abstractBinders) throws Exception {
        PropertiesParser serverPropertiesParser = new PropertiesParser(DEFAULT_HTTPS_SERVER_PROPERTIES_FILE);
        uri = GrizzlyServerOrchestrator.buildGrizzlyServerURI(serverPropertiesParser, 
                GrizzlyServerOrchestrator.HTTPS_BASE_URL_PROPERTY, GrizzlyServerOrchestrator.HTTPS_PORT_PROPERTY);
        
        grizzlyWebServer = GrizzlyServerOrchestrator.startGrizzlyWebServer(DEFAULT_HTTPS_SERVER_PROPERTIES_FILE, GrizzlyServerOrchestrator.createResourceConfig(abstractBinders));
        client = getHttpsClient ();
    }
    
    protected static ResourceConfig addTracingSupport(ResourceConfig resourceConfig) {
       
        resourceConfig.register(new LoggingFilter(java.util.logging.Logger.getLogger(LoggingFilter.class.getName()), true));
        resourceConfig.property(ServerProperties.TRACING, TracingConfig.ALL.name());
        return resourceConfig;
    }
    
    protected static ResourceConfig addBinder(ResourceConfig resourceConfig, AbstractBinder... abstractBinders) {
        for (AbstractBinder abstractBinder : abstractBinders) {
            resourceConfig.register(abstractBinder);
        }
        return resourceConfig;
    }
    
    private static Client getHttpsClient () throws IOException {
        PropertiesParser serverPropertiesParser = new PropertiesParser(DEFAULT_HTTPS_SERVER_PROPERTIES_FILE);
        ClientConfig clientConfig = new ClientConfig().connectorProvider(new GrizzlyConnectorProvider());
        clientConfig.register(JSONObjectMapperImpl.class);
        PropertiesParser certificatePropertiesParser = new PropertiesParser(
                serverPropertiesParser.getProperty(GrizzlyServerOrchestrator.HTTPS_CERTIFICATE_PROPERTIES_FILE_PROPERTY));
        
        SslConfigurator sslConfigurator = SslConfigurator.newInstance()
                .trustStoreFile(certificatePropertiesParser.getProperty(CLIENT_TRUSTORE_FILE_PROPERTY))
                .trustStorePassword(certificatePropertiesParser.getProperty(CLIENT_TRUSTORE_PASSWORD_PROPERTY));
        Client client = ClientBuilder.newBuilder().withConfig(clientConfig)
                .sslContext(sslConfigurator.createSSLContext()).build();
        return client;
    }
    
    @AfterClass
    public static void tearDownWebServer() throws Exception {
        GrizzlyServerOrchestrator.shutdownGrizzlyWebServer(grizzlyWebServer);
    }
}
