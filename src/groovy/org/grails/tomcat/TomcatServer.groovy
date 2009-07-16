package org.grails.tomcat

import grails.web.container.*
import grails.util.BuildSettingsHolder
import org.apache.catalina.*
import org.apache.catalina.startup.*
import org.apache.catalina.core.StandardContext
import java.beans.PropertyChangeListener;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.catalina.connector.Connector
import org.apache.catalina.*

class TomcatServer implements EmbeddableServer {

	Tomcat tomcat
	StandardContext context
	
    protected String keystore
    protected File keystoreFile
    protected String keyPassword
	protected buildSettings
	
	TomcatServer(String basedir, String webXml, String contextPath, ClassLoader classLoader) {
		tomcat = new Tomcat()
        this.buildSettings = BuildSettingsHolder.getSettings()		

		tomcat.basedir = buildSettings.projectWorkDir
		context = tomcat.addWebapp(contextPath, basedir)
		// we handle reloading manually
		context.reloadable = false
		context.setAliases("/WEB-INF=${buildSettings.projectWorkDir}/resources")
				
		TomcatLoader loader = new TomcatLoader(classLoader)
		
		loader.container = context
		context.loader = loader
		
		initialize()
	}
	
	TomcatServer(String warPath, String contextPath) {
		tomcat = new Tomcat()
        this.buildSettings = BuildSettingsHolder.getSettings()
		
		tomcat.basedir = buildSettings.projectWorkDir		
		context = tomcat.addWebapp(contextPath, warPath)		

		def rootLoader = getClass().classLoader.rootLoader
		def classLoader = new URLClassLoader([buildSettings.classesDir.toURL()] as URL[], rootLoader)
		TomcatLoader loader = new TomcatLoader(classLoader)
		loader.container = context
		context.loader = loader
		
		initialize()
	}
	
    protected initialize() {


        keystore = "${buildSettings.grailsWorkDir}/ssl/keystore"
        keystoreFile = new File("${keystore}")
        keyPassword = "123456"

        System.setProperty('org.mortbay.xml.XmlParser.NotValidating', 'true')
    }
	
    /**
     * Starts the container on the default port
     */
    void start() {
		start("localhost", 8080)
	}

    /**
     * Starts the container on the given port
     * @param port The port number
     */
    void start(int port) {
		start("localhost", port)
	}

    /**
     * Starts the container on the given port
     * @param host The host to start on
     * @param port The port number
     */
    void start(String host, int port) {
		tomcat.hostname = host
		tomcat.port = port
		tomcat.start()
	}

    /**
     * Starts a secure container running over HTTPS
     */
    void startSecure() {
		startSecure(8443)
	}

    /**
     * Starts a secure container running over HTTPS for the given port
     * @param port The port
     */
    void startSecure(int port) {
		startSecure("localhost", 8080, port)
	}
	
    /**
     * Starts a secure container running over HTTPS for the given port and host.
     * @param host The server host
     * @param httpPort The port for HTTP traffic.
     * @param httpsPort The port for HTTPS traffic.
     */
    void startSecure(String host, int httpPort, int httpsPort) {		
		tomcat.hostname = host
		tomcat.port = httpPort
		def sslConnector = new Connector()
		sslConnector.scheme = "https"
		sslConnector.secure = true
		sslConnector.port = httpsPort
		sslConnector.setProperty("SSLEnabled","true")
		sslConnector.setAttribute("keystore", keystore)
		sslConnector.setAttribute("keystorePass", keyPassword)
		tomcat.service.addConnector sslConnector
		tomcat.start()
	}
	
    /**
     * Creates the necessary SSL certificate for running in HTTPS mode
     */
    protected createSSLCertificate() {
        println 'Creating SSL Certificate...'
        if (!keystoreFile.parentFile.exists() &&
                !keystoreFile.parentFile.mkdir()) {
            def msg = "Unable to create keystore folder: " + keystoreFile.parentFile.canonicalPath
            throw new RuntimeException(msg)
        }
        String[] keytoolArgs = ["-genkey", "-alias", "localhost", "-dname",
                "CN=localhost,OU=Test,O=Test,C=US", "-keyalg", "RSA",
                "-validity", "365", "-storepass", "key", "-keystore",
                "${keystore}", "-storepass", "${keyPassword}",
                "-keypass", "${keyPassword}"]
        KeyTool.main(keytoolArgs)
        println 'Created SSL Certificate.'
    }	


    /**
     * Stops the container
     */
    void stop() {
		tomcat.stop()
	}

    /**
     * Typically combines the stop() and start() methods in order to restart the container
     */
    void restart() {
		stop()
		start()
	}

}


class TomcatLoader implements Loader, Lifecycle {

    private static Log log = LogFactory.getLog(TomcatLoader.class.getName())
    
    ClassLoader classLoader
    Container container
    boolean delegate
    boolean reloadable

	TomcatLoader(ClassLoader classLoader) {
		this.classLoader = classLoader
	}

    void addPropertyChangeListener(PropertyChangeListener listener) {}

    void addRepository(String repository) {
          log.warn "Call to addRepository($repository) was ignored."
    }

    void backgroundProcess() {}

    String[] findRepositories() {
          log.warn "Call to findRepositories() returned null." 
    }

    String getInfo() { "MyLoader/1.0" }

    boolean modified() { false }

    void removePropertyChangeListener(PropertyChangeListener listener) {}

    void addLifecycleListener(LifecycleListener listener) {
        log.warn "Call to addLifecycleListener($listener) was ignored."
    }

    LifecycleListener[] findLifecycleListeners() {
        log.warn "Call to findLifecycleListeners() returned null."
    }

    void removeLifecycleListener(LifecycleListener listener) {
        log.warn "Call to removeLifecycleListener(${listener}) was ignored."	
    }

    void start()  {}

    void stop()  {
        classLoader = null
    }

}