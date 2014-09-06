package com.unicorn.rest.server;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Date;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.unicorn.rest.commons.ServiceConstants;
import com.unicorn.rest.server.injector.RepositoryBinder;

public class GrizzlyServerOrchestrator {

    private static final String SERVER_PROPERTIES_FILE = "server.properties";

    protected static final String HTTPS_BASE_URL_PROPERTY = "HTTPS_BASE_URL";
    protected static final String HTTPS_PORT_PROPERTY = "HTTPS_PORT";

    protected static final String HTTPS_CERTIFICATE_PROPERTIES_FILE_PROPERTY = "HTTPS_CERTIFICATE_PROPERTIES_FILE";
    protected static final String SERVER_KEYSTORE_FILE_PROPERTY = "SERVER_KEYSTORE_FILE";
    protected static final String SERVER_KEYSTORE_PASSWORD_PROPERTY = "SERVER_KEYSTORE_PASSWORD";
    protected static final String SERVER_TRUSTORE_FILE_PROPERTY = "SERVER_TRUSTORE_FILE";
    protected static final String SERVER_TRUSTORE_PASSWORD_PROPERTY = "SERVER_TRUSTORE_PASSWORD";

    private static volatile boolean terminate = false;

    public static void main(String[] args) throws IOException {

        HttpServer grizzlyWebServer = null;
        try {
            printWithTimestamp(" [INFO] Starting Grizzly Server...");

            try {
                ResourceConfig resourceConfig = createResourceConfig(new RepositoryBinder());
                grizzlyWebServer = startGrizzlyWebServer(SERVER_PROPERTIES_FILE, resourceConfig);
                printWithTimestamp(" [INFO] Grizzly Server Started");

            } catch(IllegalArgumentException iae) {
                printWithTimestamp(String.format(" [ERROR] %s", iae.getMessage()));
                return;
            } catch (RuntimeException re) {
                printWithTimestamp(String.format(" [ERROR] %s: %s", re.getMessage(), re.getCause()));
                return;
            }

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    terminate = true;
                }
            }, "GrizzlyServerOrchestrator-ShutdownHook"));

            while(!terminate){}

        } finally {
            printWithTimestamp(" [INFO] Stopping Grizzly Server...");
            shutdownGrizzlyWebServer(grizzlyWebServer);
        }
    }
    
    /**
     * 
     * @param serverPropertyFile @Nullable
     * @param resourceConfig @Nonnull
     * @return @Nonnull
     */
    protected static HttpServer startGrizzlyWebServer(@Nullable String serverPropertyFile, @Nonnull ResourceConfig resourceConfig) {
        HttpServer grizzlyWebServer = createGrizzlyWebServer(serverPropertyFile, resourceConfig);
        startGrizzlyWebServer(grizzlyWebServer);
        return grizzlyWebServer;
    }

    /**
     * @param grizzlyWebServer @Nullable
     */
    public static void shutdownGrizzlyWebServer(@Nullable HttpServer grizzlyWebServer) {
        if (grizzlyWebServer != null && grizzlyWebServer.isStarted()) {            
            GrizzlyFuture<HttpServer> future = grizzlyWebServer.shutdown();
            while (!future.isDone()) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignore){}
            }            
        }      
    }

    private static void startGrizzlyWebServer(@Nonnull HttpServer grizzlyWebServer) {
        try {
            // Start the server.
            grizzlyWebServer.start();
        } catch (IOException ioe) {
            grizzlyWebServer.shutdownNow();
            throw new RuntimeException("Grizzly Server failed while attempting to start" , ioe);
        }
    }
    
    /**
     * @return ResourceConfig @Nonnull
     */
    protected static ResourceConfig createResourceConfig(AbstractBinder... abstractBinders) {
        /*
         * create a resource config that scans for JAX-RS resources and providers under ebServiceConstants.ROOT_PACKAGE
         * Note: All the API and filter should under this ROOT_PACKAGE. Otherwise, we will get 404 Not Found and filters will not get triggered.
         */
        ResourceConfig resourceConfig = new ResourceConfig().packages(ServiceConstants.ROOT_PACKAGE).setApplicationName(ServiceConstants.APPLICATION_NAME);
        for (AbstractBinder abstractBinder : abstractBinders) {
            resourceConfig.register(abstractBinder);
        }
        return resourceConfig;
    }


    /**
     * @param serverPropertiesParser @Nonnull
     * @param baseURIProperty @Nonnull
     * @param portProperty @Nonnull
     * @return NULL if given properties does not exist; 
     * @throws IllegalArgumentException if port is not an integer
     */
    protected static URI buildGrizzlyServerURI(@Nonnull PropertiesParser serverPropertiesParser, @Nonnull String baseURIProperty, @Nonnull String portProperty) {
        try {
            String baseURI = serverPropertiesParser.getProperty(baseURIProperty);
            String port = serverPropertiesParser.getProperty(portProperty);

            if (!StringUtils.isBlank(baseURI) && !StringUtils.isBlank(port)) {
                return UriBuilder.fromUri(baseURI).port(Integer.parseInt(port)).build();
            } else {
                return null;
            }
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(String.format("Grizzly Server failed while attempting to parse port property %s: %s", portProperty, nfe));
        }
    }

    /**
     * @param httpsCertificatePropertyFile @Nullable
     * @return SSLEngineConfigurator @Nonnull
     * @throws IllegalArgumentException if failed to find httpsCertificatePropertyFile
     *                                  if failed to find server key store file
     *                                  if failed to load server key store file or trust store file 
     * @throws RuntimeException if failed to load httpsCertificatePropertyFile 
     */
    private static SSLEngineConfigurator buildSSLEngineConfigurator(@Nullable String httpsCertificatePropertyFile) {
        if (StringUtils.isBlank(httpsCertificatePropertyFile)) {
            throw new IllegalArgumentException("Grizzly Server failed while attempting to get https certificate property");
        }

        try {
            PropertiesParser certificatePropertiesParser = new PropertiesParser(httpsCertificatePropertyFile);

            boolean clientAuth = false;
            // Grizzly SSL configuration
            SSLContextConfigurator sslContext = new SSLContextConfigurator();

            String keyStoreServerFile = certificatePropertiesParser.getProperty(SERVER_KEYSTORE_FILE_PROPERTY);
            String keyStoreServerPassword = certificatePropertiesParser.getProperty(SERVER_KEYSTORE_PASSWORD_PROPERTY);
            if (StringUtils.isBlank(keyStoreServerFile) || StringUtils.isBlank(keyStoreServerPassword)) {
                throw new IllegalArgumentException("Grizzly Server failed while attempting to get server keystore file");
            }

            if (!new File(keyStoreServerFile).exists()) {
                throw new IllegalArgumentException( String.format("Grizzly Server failed while attempting to load server keystore file %s", keyStoreServerFile));
            }

            sslContext.setKeyStoreFile(keyStoreServerFile); 
            sslContext.setKeyStorePass(keyStoreServerPassword);
            // contains client certificate
            String trustStoreFile = certificatePropertiesParser.getProperty(SERVER_TRUSTORE_FILE_PROPERTY);
            String trustStorePassword = certificatePropertiesParser.getProperty(SERVER_TRUSTORE_PASSWORD_PROPERTY);

            if (!StringUtils.isBlank(trustStoreFile) && !StringUtils.isBlank(trustStorePassword)) {
                if (!new File(trustStoreFile).exists()) {
                    throw new IllegalArgumentException( String.format("Grizzly Server failed while attempting to load server truststore file %s", trustStoreFile));
                }

                sslContext.setTrustStoreFile(trustStoreFile);
                sslContext.setTrustStorePass(trustStorePassword);
                clientAuth = true;
            }

            return new SSLEngineConfigurator(sslContext, false, clientAuth, clientAuth);
        } catch (IOException ioe) {
            throw new RuntimeException( String.format("Grizzly Server failed while attempting to load %s", httpsCertificatePropertyFile), ioe);
        }
    }

    /**
     * This method is protected for unit test.
     * @param serverPropertyFile @Nullable
     * @param resourceConfig @Nonnull
     * @return HttpServer @Nonnull
     * @throws IllegalArgumentException if failed to get serverPropertyFile 
     *                                  if failed to load URI and port from serverPropertyFile
     * @throws RuntimeException if failed to load serverPropertyFile 
     */
    protected static HttpServer createGrizzlyWebServer(@Nullable String serverPropertyFile, @Nonnull ResourceConfig resourceConfig) {
        if (StringUtils.isBlank(serverPropertyFile)) {
            throw new IllegalArgumentException("Grizzly Server failed while attempting to get server property");
        }

        try {
            PropertiesParser serverPropertiesParser = new PropertiesParser(serverPropertyFile);

            URI httpsURI = buildGrizzlyServerURI(serverPropertiesParser, HTTPS_BASE_URL_PROPERTY, HTTPS_PORT_PROPERTY);
            if (httpsURI == null) {
                throw new IllegalArgumentException("Grizzly Server failed while attempting to load URI and port: No URI and port provided");
            }

            HttpServer grizzlyWebServer= new HttpServer();
            
            ServerConfiguration serverConfiguration = grizzlyWebServer.getServerConfiguration();
            GrizzlyHttpContainer grizzlyHttpHandler = ContainerFactory.createContainer(GrizzlyHttpContainer.class, resourceConfig);
            serverConfiguration.setPassTraceRequest(true);
            NetworkListener httpsListener = new NetworkListener("GRIZZLY-HTTPS", httpsURI.getHost(), httpsURI.getPort());
            httpsListener.setSecure(true);
            httpsListener.setSSLEngineConfig(buildSSLEngineConfigurator(serverPropertiesParser.getProperty(HTTPS_CERTIFICATE_PROPERTIES_FILE_PROPERTY)));
            grizzlyWebServer.addListener(httpsListener);
            serverConfiguration.addHttpHandler(grizzlyHttpHandler, httpsURI.getPath());

            return grizzlyWebServer;
        } catch (IOException ioe) {
            throw new RuntimeException( String.format("Grizzly Server failed while attempting to load %s", serverPropertyFile), ioe);
        }
    }

    private static void printWithTimestamp(String message) {
        System.out.println(new Date().toString() + message); 
    }

}
