/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * You may find it more helpful to your design to adjust the
 * functionality, constants and interfaces (if there are any)
 * provided within in order to meet the needs of your specific
 * Programming the Internet of Things project.
 */
package programmingtheiot.gda.connection;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.WebLink;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.UdpConfig;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.gda.connection.handlers.SensorDataObserverHandler;
import programmingtheiot.gda.connection.handlers.SystemPerformanceDataObserverHandler;

/**
 * CoAP client connector implementation using Eclipse Californium library.
 * Provides connectivity to CoAP servers for IoT device communication.
 */
public class CoapClientConnector implements IRequestResponseClient
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(CoapClientConnector.class.getName());
	
	// params
	
	private String protocol;
	private String host;
	private int port;
	private String serverAddr;
	private CoapClient clientConn;
	private IDataMessageListener dataMsgListener;
	private Map<String, CoapObserveRelation> observeRelations;
	
	// constructors
	
	/**
	 * Default constructor.
	 * All config data will be loaded from the config file.
	 */
	public CoapClientConnector()
	{
		ConfigUtil config = ConfigUtil.getInstance();
		
		this.host = config.getProperty(
			ConfigConst.COAP_GATEWAY_SERVICE, 
			ConfigConst.HOST_KEY, 
			ConfigConst.DEFAULT_HOST);
		
		if (config.getBoolean(ConfigConst.COAP_GATEWAY_SERVICE, ConfigConst.ENABLE_CRYPT_KEY)) {
			this.protocol = ConfigConst.DEFAULT_COAP_SECURE_PROTOCOL;
			this.port = config.getInteger(
				ConfigConst.COAP_GATEWAY_SERVICE, 
				ConfigConst.SECURE_PORT_KEY, 
				ConfigConst.DEFAULT_COAP_SECURE_PORT);
		} else {
			this.protocol = ConfigConst.DEFAULT_COAP_PROTOCOL;
			this.port = config.getInteger(
				ConfigConst.COAP_GATEWAY_SERVICE, 
				ConfigConst.PORT_KEY, 
				ConfigConst.DEFAULT_COAP_PORT);
		}
		
		// NOTE: URL does not have a protocol handler for "coap",
		// so we need to construct the URL manually
		this.serverAddr = this.protocol + "://" + this.host + ":" + this.port;
		
		this.observeRelations = new HashMap<>();
		
		initClient();
		
		_Logger.info("Using URL for server conn: " + this.serverAddr);
	}
	
	/**
	 * Constructor with parameters.
	 * 
	 * @param host The CoAP server host
	 * @param isSecure Whether to use secure CoAP (CoAPS)
	 * @param enableConfirmedMsgs Whether to use confirmable messages
	 */
	public CoapClientConnector(String host, boolean isSecure, boolean enableConfirmedMsgs)
	{
		// TODO: Implement parameterized constructor if needed
		this();
	}
	
	// public methods
	
	@Override
	public boolean sendDiscoveryRequest(int timeout)
	{
		_Logger.info("Discovering remote resources...");
		
		try {
			// Use Californium's built-in discovery API
			Set<WebLink> wlSet = this.clientConn.discover();
			
			if (wlSet != null) {
				for (WebLink wl : wlSet) {
					_Logger.info(" --> URI: " + wl.getURI() + ". Attributes: " + wl.getAttributes());
				}
				
				return true;
			} else {
				_Logger.warning("No resources discovered. Response is null.");
				return false;
			}
		} catch (Exception e) {
			_Logger.log(Level.WARNING, "Failed to discover resources.", e);
			return false;
		}
	}
	
	@Override
	public boolean sendDeleteRequest(ResourceNameEnum resource, String name, boolean enableCON, int timeout)
	{
		_Logger.info("Issuing DELETE request to CoAP server...");
		
		CoapResponse response = null;
		
		try {
			// Set message type (CON or NON)
			if (enableCON) {
				this.clientConn.useCONs();
			} else {
				this.clientConn.useNONs();
			}
			
			// Build resource path
			String resourcePath = resource.getResourceName();
			if (name != null && name.length() > 0) {
				resourcePath += "/" + name;
			}
			
			// Set URI and send DELETE request
			this.clientConn.setURI(this.serverAddr + "/" + resourcePath);
			response = this.clientConn.delete();
			
			if (response != null) {
				_Logger.info("Handling DELETE. Response: " + response.isSuccess() + " - " + response.getOptions() + 
					" - " + response.getCode() + " - " + response.getResponseText());
				
				// Pass to data message listener if available
				if (this.dataMsgListener != null) {
					// TODO: Parse response and send to listener in future activities
				}
				
				return true;
			} else {
				_Logger.warning("Handling DELETE. No response received.");
			}
		} catch (Exception e) {
			_Logger.log(Level.WARNING, "Failed to execute DELETE request.", e);
		}
		
		return false;
	}
	
	@Override
	public boolean sendGetRequest(ResourceNameEnum resource, String name, boolean enableCON, int timeout)
	{
		_Logger.info("Issuing GET request to CoAP server...");
		
		CoapResponse response = null;
		
		try {
			// Set message type (CON or NON)
			if (enableCON) {
				this.clientConn.useCONs();
			} else {
				this.clientConn.useNONs();
			}
			
			// Build resource path
			String resourcePath = resource.getResourceName();
			if (name != null && name.length() > 0) {
				resourcePath += "/" + name;
			}
			
			// Set URI and send GET request
			this.clientConn.setURI(this.serverAddr + "/" + resourcePath);
			response = this.clientConn.get();
			
			if (response != null) {
				_Logger.info("Handling GET. Response: " + response.isSuccess() + " - " + response.getOptions() + 
					" - " + response.getCode() + " - " + response.getResponseText());
				
				// Pass to data message listener if available
				if (this.dataMsgListener != null) {
					// TODO: Parse response and send to listener in future activities
				}
				
				return true;
			} else {
				_Logger.warning("Handling GET. No response received.");
			}
		} catch (Exception e) {
			_Logger.log(Level.WARNING, "Failed to execute GET request.", e);
		}
		
		return false;
	}
	
	@Override
	public boolean sendPostRequest(ResourceNameEnum resource, String name, boolean enableCON, String payload, int timeout)
	{
		_Logger.info("Issuing POST request to CoAP server...");
		
		CoapResponse response = null;
		
		try {
			// Set message type (CON or NON)
			if (enableCON) {
				this.clientConn.useCONs();
			} else {
				this.clientConn.useNONs();
			}
			
			// Build resource path
			String resourcePath = resource.getResourceName();
			if (name != null && name.length() > 0) {
				resourcePath += "/" + name;
			}
			
			// Set URI and send POST request with payload
			this.clientConn.setURI(this.serverAddr + "/" + resourcePath);
			response = this.clientConn.post(payload, MediaTypeRegistry.TEXT_PLAIN);
			
			if (response != null) {
				_Logger.info("Handling POST. Response: " + response.isSuccess() + " - " + response.getOptions() + 
					" - " + response.getCode() + " - " + response.getResponseText());
				
				// Pass to data message listener if available
				if (this.dataMsgListener != null) {
					// TODO: Parse response and send to listener in future activities
				}
				
				return true;
			} else {
				_Logger.warning("Handling POST. No response received.");
			}
		} catch (Exception e) {
			_Logger.log(Level.WARNING, "Failed to execute POST request.", e);
		}
		
		return false;
	}
	
	@Override
	public boolean sendPutRequest(ResourceNameEnum resource, String name, boolean enableCON, String payload, int timeout)
	{
		_Logger.info("Issuing PUT request to CoAP server...");
		
		CoapResponse response = null;
		
		try {
			// Set message type (CON or NON)
			if (enableCON) {
				this.clientConn.useCONs();
			} else {
				this.clientConn.useNONs();
			}
			
			// Build resource path
			String resourcePath = resource.getResourceName();
			if (name != null && name.length() > 0) {
				resourcePath += "/" + name;
			}
			
			// Set URI and send PUT request with payload
			this.clientConn.setURI(this.serverAddr + "/" + resourcePath);
			response = this.clientConn.put(payload, MediaTypeRegistry.TEXT_PLAIN);
			
			if (response != null) {
				_Logger.info("Handling PUT. Response: " + response.isSuccess() + " - " + response.getOptions() + 
					" - " + response.getCode() + " - " + response.getResponseText());
				
				// Pass to data message listener if available
				if (this.dataMsgListener != null) {
					// TODO: Parse response and send to listener in future activities
				}
				
				return true;
			} else {
				_Logger.warning("Handling PUT. No response received.");
			}
		} catch (Exception e) {
			_Logger.log(Level.WARNING, "Failed to execute PUT request.", e);
		}
		
		return false;
	}
	
	@Override
	public boolean setDataMessageListener(IDataMessageListener listener)
	{
		if (listener != null) {
			this.dataMsgListener = listener;
			return true;
		}
		
		return false;
	}
	
	public void clearEndpointPath()
	{
		// Optional: Clear any stored endpoint path
	}
	
	public void setEndpointPath(ResourceNameEnum resource)
	{
		// Optional: Set endpoint path for resource
	}
	
	@Override
	public boolean startObserver(ResourceNameEnum resource, String name, int ttl)
	{
		String uriPath = createUriPath(resource, name);
		
		_Logger.info("Observing resource [START]: " + uriPath);
		
		this.clientConn.setURI(uriPath);
		
		// Determine which handler to use based on resource type
		CoapHandler handler = null;
		
		if (resource == ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE) {
			SensorDataObserverHandler sensorHandler = new SensorDataObserverHandler();
			sensorHandler.setDataMessageListener(this.dataMsgListener);
			handler = sensorHandler;
		} else if (resource == ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE) {
			SystemPerformanceDataObserverHandler sysPerfHandler = new SystemPerformanceDataObserverHandler();
			sysPerfHandler.setDataMessageListener(this.dataMsgListener);
			handler = sysPerfHandler;
		} else {
			_Logger.warning("No specific handler for resource type. Using default observation.");
			return false;
		}
		
		// Start observation
		CoapObserveRelation cor = this.clientConn.observe(handler);
		
		// Store the observation relation for later cancellation
		this.observeRelations.put(uriPath, cor);
		
		return (cor != null && !cor.isCanceled());
	}
	
	@Override
	public boolean stopObserver(ResourceNameEnum resource, String name, int timeout)
	{
		String uriPath = createUriPath(resource, name);
		
		_Logger.info("Observing resource [STOP]: " + uriPath);
		
		if (this.observeRelations.containsKey(uriPath)) {
			CoapObserveRelation cor = this.observeRelations.get(uriPath);
			
			if (cor != null) {
				cor.proactiveCancel();
				this.observeRelations.remove(uriPath);
				_Logger.info("Cancelled observation for: " + uriPath);
				return true;
			}
		}
		
		_Logger.warning("No active observation found for: " + uriPath);
		return false;
	}
	
	// private methods
	
	/**
	 * Initialize the CoAP client connection.
	 * Creates a CoapClient instance pointing to the configured server.
	 * FIXED: Removed the Configuration.getStandard() check that was causing 
	 * "Configuration contains no definitions!" error.
	 */
	private void initClient()
	{
		try {
			// Create the CoAP configuration with proper definitions
			// NOTE 1: Always create a fresh configuration to avoid file loading issues
			// NOTE 2: These settings are for UDP only
			Configuration config = new Configuration(
				org.eclipse.californium.core.config.CoapConfig.DEFINITIONS, 
				UdpConfig.DEFINITIONS
			);
			
			// Set this as the standard configuration
			Configuration.setStandard(config);
			_Logger.info("Initialized Californium configuration with standard definitions");
			
			// Create the CoAP client
			this.clientConn = new CoapClient(this.serverAddr);
			
			_Logger.info("Created client connection to server / resource: " + this.serverAddr);
			
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to connect to server: " + this.serverAddr, e);
		}
	}
	
	/**
	 * Create URI path from resource and name.
	 * 
	 * @param resource The resource enum
	 * @param name Optional resource name
	 * @return The complete URI path
	 */
	private String createUriPath(ResourceNameEnum resource, String name)
	{
		String resourcePath = resource.getResourceName();
		
		if (name != null && name.length() > 0) {
			resourcePath += "/" + name;
		}
		
		return this.serverAddr + "/" + resourcePath;
	}
	
	/**
	 * Get the server URI.
	 * 
	 * @return The server address
	 */
	private String getServerUri()
	{
		return this.serverAddr;
	}
}