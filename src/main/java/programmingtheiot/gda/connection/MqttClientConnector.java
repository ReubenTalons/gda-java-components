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

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

/**
 * MQTT Client Connector implementation for GDA.
 * 
 */
public class MqttClientConnector implements IPubSubClient, MqttCallbackExtended
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(MqttClientConnector.class.getName());
	
	// params
	
	private boolean useAsyncClient = false;
	
	// NEW: Lab Module 11 additions
	private IConnectionListener connListener = null;
	private boolean useCloudGatewayConfig = false;

	private MqttClient           mqttClient = null;
	private MqttConnectOptions   connOpts = null;
	private MemoryPersistence    persistence = null;
	private IDataMessageListener dataMsgListener = null;

	private String  clientID = null;
	private String  brokerAddr = null;
	private String  host = ConfigConst.DEFAULT_HOST;
	private String  protocol = ConfigConst.DEFAULT_MQTT_PROTOCOL;
	private int     port = ConfigConst.DEFAULT_MQTT_PORT;
	private int     brokerKeepAlive = ConfigConst.DEFAULT_KEEP_ALIVE;
	
	// constructors
	
	/**
	 * Default constructor.
	 * This will use the MQTT Gateway Service configuration.
	 */
	public MqttClientConnector()
	{
		this(false);
	}
	
	/**
	 * Constructor with cloud gateway configuration option.
	 * 
	 * @param useCloudGatewayConfig If true, use Cloud Gateway Service config; 
	 *                               otherwise use MQTT Gateway Service config.
	 */
	public MqttClientConnector(boolean useCloudGatewayConfig)
	{
		this(useCloudGatewayConfig ? ConfigConst.CLOUD_GATEWAY_SERVICE : null);
	}
	
	/**
	 * Constructor with custom configuration section name.
	 * 
	 * @param cloudGatewayConfigSectionName The configuration section name to use.
	 *                                       If null or empty, defaults to MQTT Gateway Service.
	 */
	public MqttClientConnector(String cloudGatewayConfigSectionName)
	{
		super();
		
		if (cloudGatewayConfigSectionName != null && cloudGatewayConfigSectionName.trim().length() > 0) {
			this.useCloudGatewayConfig = true;
			
			initClientParameters(cloudGatewayConfigSectionName);
		} else {
			this.useCloudGatewayConfig = false;
			
			initClientParameters(ConfigConst.MQTT_GATEWAY_SERVICE);
		}
	}
	
	
	// public methods
	
	@Override
	public boolean connectClient()
	{
		try {
			if (this.mqttClient == null) {
				this.mqttClient = new MqttClient(this.brokerAddr, this.clientID, this.persistence);
				this.mqttClient.setCallback(this);
			}
			
			if (! this.mqttClient.isConnected()) {
				_Logger.info("MQTT client connecting to broker: " + this.brokerAddr);
				this.mqttClient.connect(this.connOpts);
				return true;
			} else {
				_Logger.warning("MQTT client already connected to broker: " + this.brokerAddr);
			}
		} catch (MqttException e) {
			_Logger.log(Level.SEVERE, "Failed to connect MQTT client to broker.", e);
		}
		
		return false;
	}
	
	@Override
	public boolean disconnectClient()
	{
		try {
			if (this.mqttClient != null) {
				if (this.mqttClient.isConnected()) {
					_Logger.info("Disconnecting MQTT client from broker: " + this.brokerAddr);
					this.mqttClient.disconnect();
					return true;
				} else {
					_Logger.warning("MQTT client not connected to broker: " + this.brokerAddr);
				}
			}
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to disconnect MQTT client from broker: " + this.brokerAddr, e);
		}
		
		return false;
	}
	
	public boolean isConnected()
	{
		return (this.mqttClient != null && this.mqttClient.isConnected());
	}
	
	/**
	 * Updated to delegate to protected method.
	 */
	@Override
	public boolean publishMessage(ResourceNameEnum topicName, String msg, int qos)
	{
		if (topicName == null) {
			_Logger.warning("Resource is null. Unable to publish message: " + this.brokerAddr);
			return false;
		}
		
		if (msg == null || msg.length() == 0) {
			_Logger.warning("Message is null or empty. Unable to publish message: " + this.brokerAddr);
			return false;
		}
		
		return publishMessage(topicName.getResourceName(), msg.getBytes(), qos);
	}
	
	/**
	 * Updated to delegate to protected method.
	 */
	@Override
	public boolean subscribeToTopic(ResourceNameEnum topicName, int qos)
	{
		if (topicName == null) {
			_Logger.warning("Resource is null. Unable to subscribe to topic: " + this.brokerAddr);
			return false;
		}
		
		return subscribeToTopic(topicName.getResourceName(), qos);
	}
	
	/**
	 * Updated to delegate to protected method.
	 */
	@Override
	public boolean unsubscribeFromTopic(ResourceNameEnum topicName)
	{
		if (topicName == null) {
			_Logger.warning("Resource is null. Unable to unsubscribe from topic: " + this.brokerAddr);
			return false;
		}
		
		return unsubscribeFromTopic(topicName.getResourceName());
	}
	
	/**
	 * Updated implementation for Lab Module 11.
	 */
	@Override
	public boolean setConnectionListener(IConnectionListener listener)
	{
		if (listener != null) {
			_Logger.info("Setting connection listener.");
			this.connListener = listener;
			return true;
		} else {
			_Logger.warning("No connection listener specified. Ignoring.");
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
	
	// callbacks
	
	/**
	 * Updated for Lab Module 11 - handles both local and cloud subscriptions.
	 */
	@Override
	public void connectComplete(boolean reconnect, String serverURI)
	{
		_Logger.info("MQTT connection successful (is reconnect = " + reconnect + "). Broker: " + serverURI);
		
		int qos = 1;
		
		// Option 1: Subscribe to local topics if NOT using cloud gateway config
		if (! this.useCloudGatewayConfig) {
			this.subscribeToTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE, qos);
			this.subscribeToTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, qos);
			this.subscribeToTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, qos);
			
			_Logger.info("Subscribed to local CDA topics.");
		}
		
		// Notify connection listener that connection is complete
		if (this.connListener != null) {
			this.connListener.onConnect();
		}
	}
	
	@Override
	public void connectionLost(Throwable t)
	{
		_Logger.log(Level.WARNING, "Lost connection to MQTT broker: " + this.brokerAddr, t);
	}
	
	@Override
	public void deliveryComplete(IMqttDeliveryToken token)
	{
		// TODO: Logging level may need to be adjusted to see output in log file / console
		_Logger.fine("Delivered MQTT message with ID: " + token.getMessageId());
	}
	
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception
	{
		// TODO: Logging level may need to be adjusted to reduce output in log file / console
		_Logger.info("MQTT message arrived on topic: '" + topic + "'");
	}
	
	// protected methods - NEW for Lab Module 11
	
	/**
	 * Protected publish method that uses String topic names.
	 * Allows package-scoped classes or sub-classes to publish directly.
	 * 
	 * @param topicName The topic name as a String
	 * @param payload The message payload as byte array
	 * @param qos The quality of service level (0, 1, or 2)
	 * @return true if successful, false otherwise
	 */
	protected boolean publishMessage(String topicName, byte[] payload, int qos)
	{
		if (topicName == null) {
			_Logger.warning("Resource is null. Unable to publish message: " + this.brokerAddr);
			return false;
		}
		
		if (payload == null || payload.length == 0) {
			_Logger.warning("Message is null or empty. Unable to publish message: " + this.brokerAddr);
			return false;
		}
		
		if (qos < 0 || qos > 2) {
			_Logger.warning("Invalid QoS. Using default. QoS requested: " + qos);
			qos = ConfigConst.DEFAULT_QOS;
		}
		
		try {
			MqttMessage mqttMsg = new MqttMessage();
			mqttMsg.setQos(qos);
			mqttMsg.setPayload(payload);
			
			this.mqttClient.publish(topicName, mqttMsg);
			
			return true;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to publish message to topic: " + topicName, e);
		}
		
		return false;
	}
	
	/**
	 * Protected subscribe method that uses String topic names.
	 * Allows package-scoped classes or sub-classes to subscribe directly.
	 * 
	 * @param topicName The topic name as a String
	 * @param qos The quality of service level (0, 1, or 2)
	 * @return true if successful, false otherwise
	 */
	protected boolean subscribeToTopic(String topicName, int qos)
	{
		return subscribeToTopic(topicName, qos, null);
	}
	
	/**
	 * Protected subscribe method with custom message listener.
	 * Allows package-scoped classes or sub-classes to subscribe directly
	 * with a custom IMqttMessageListener.
	 * 
	 * @param topicName The topic name as a String
	 * @param qos The quality of service level (0, 1, or 2)
	 * @param listener Custom message listener for this topic (can be null)
	 * @return true if successful, false otherwise
	 */
	protected boolean subscribeToTopic(String topicName, int qos, IMqttMessageListener listener)
	{
		if (topicName == null) {
			_Logger.warning("Resource is null. Unable to subscribe to topic: " + this.brokerAddr);
			return false;
		}
		
		if (qos < 0 || qos > 2) {
			_Logger.warning("Invalid QoS. Using default. QoS requested: " + qos);
			qos = ConfigConst.DEFAULT_QOS;
		}
		
		try {
			if (listener != null) {
				this.mqttClient.subscribe(topicName, qos, listener);
				_Logger.info("Successfully subscribed to topic with listener: " + topicName);
			} else {
				this.mqttClient.subscribe(topicName, qos);
				_Logger.info("Successfully subscribed to topic: " + topicName);
			}
			
			return true;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to subscribe to topic: " + topicName, e);
		}
		
		return false;
	}
	
	/**
	 * Protected unsubscribe method that uses String topic names.
	 * Allows package-scoped classes or sub-classes to unsubscribe directly.
	 * 
	 * @param topicName The topic name as a String
	 * @return true if successful, false otherwise
	 */
	protected boolean unsubscribeFromTopic(String topicName)
	{
		if (topicName == null) {
			_Logger.warning("Resource is null. Unable to unsubscribe from topic: " + this.brokerAddr);
			return false;
		}
		
		try {
			this.mqttClient.unsubscribe(topicName);
			_Logger.info("Successfully unsubscribed from topic: " + topicName);
			return true;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to unsubscribe from topic: " + topicName, e);
		}
		
		return false;
	}
	
	// private methods
	
	/**
	 * Called by the constructor to set the MQTT client parameters to be used for the connection.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
	private void initClientParameters(String configSectionName)
	{
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		// Load basic connection parameters from the specified config section
		this.host =
			configUtil.getProperty(
				configSectionName, ConfigConst.HOST_KEY, ConfigConst.DEFAULT_HOST);
		
		this.port =
			configUtil.getInteger(
				configSectionName, ConfigConst.PORT_KEY, ConfigConst.DEFAULT_MQTT_PORT);
		
		this.brokerKeepAlive =
			configUtil.getInteger(
				configSectionName, ConfigConst.KEEP_ALIVE_KEY, ConfigConst.DEFAULT_KEEP_ALIVE);
		
		this.useAsyncClient =
			configUtil.getBoolean(
				configSectionName, ConfigConst.USE_ASYNC_CLIENT_KEY);
		
		// Generate client ID
		this.clientID = MqttClient.generateClientId();
		
		// Initialize MQTT connection parameters
		this.persistence = new MemoryPersistence();
		this.connOpts = new MqttConnectOptions();
		
		this.connOpts.setKeepAliveInterval(this.brokerKeepAlive);
		this.connOpts.setCleanSession(false);
		this.connOpts.setAutomaticReconnect(true);
		
		// Check if encryption is enabled
		boolean enableEncryption = 
			configUtil.getBoolean(
				configSectionName, ConfigConst.ENABLE_CRYPT_KEY);
		
		if (enableEncryption) {
			this.initSecureConnectionParameters(configSectionName);
		}
		
		// Check if credentials are needed
		boolean enableAuth = 
			configUtil.getBoolean(
				configSectionName, ConfigConst.ENABLE_AUTH_KEY);
		
		if (enableAuth) {
			this.initCredentialConnectionParameters(configSectionName);
		}
		
		// Construct broker address
		this.brokerAddr = this.protocol + "://" + this.host + ":" + this.port;
		
		_Logger.info("Using configuration section: " + configSectionName);
		_Logger.info("MQTT Client ID: " + this.clientID);
		_Logger.info("MQTT Broker: " + this.brokerAddr);
		_Logger.info("MQTT Keep Alive: " + this.brokerKeepAlive);
	}
	
	/**
	 * Called by {@link #initClientParameters(String)} to load credentials.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
	private void initCredentialConnectionParameters(String configSectionName)
	{
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		try {
			// Load credentials from credentials file
			String credFile = 
				configUtil.getProperty(
					configSectionName, ConfigConst.CRED_FILE_KEY);
			
			if (credFile != null) {
				Properties props = configUtil.getCredentials(credFile);
				
				if (props != null) {
					String username = props.getProperty(ConfigConst.USER_NAME_TOKEN_KEY);
					String password = props.getProperty(ConfigConst.USER_AUTH_TOKEN_KEY);
					
					if (username != null && password != null) {
						this.connOpts.setUserName(username);
						this.connOpts.setPassword(password.toCharArray());
						
						_Logger.info("Credentials loaded for user: " + username);
					} else {
						_Logger.warning("Credentials file exists but username/password not found.");
					}
				}
			}
		} catch (Exception e) {
			_Logger.log(Level.WARNING, "Failed to load credentials.", e);
		}
	}
	
	/**
	 * Called by {@link #initClientParameters(String)} to enable encryption.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
	private void initSecureConnectionParameters(String configSectionName)
	{
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		try {
			_Logger.info("Configuring TLS...");
			
			// Update protocol and port for secure connection
			this.protocol = ConfigConst.DEFAULT_MQTT_SECURE_PROTOCOL;
			this.port = 
				configUtil.getInteger(
					configSectionName, ConfigConst.SECURE_PORT_KEY, ConfigConst.DEFAULT_MQTT_SECURE_PORT);
			
			// Load certificate file
			String certFile = 
				configUtil.getProperty(
					configSectionName, ConfigConst.CERT_FILE_KEY);
			
			if (certFile != null) {
				// Set up SSL properties
				Properties sslProps = new Properties();
				sslProps.setProperty("com.ibm.ssl.protocol", "TLSv1.2");
				
				this.connOpts.setSSLProperties(sslProps);
				
				// Note: Additional certificate loading logic would go here
				// This depends on your specific implementation
				
				_Logger.info("TLS enabled with certificate: " + certFile);
			}
			
		} catch (Exception e) {
			_Logger.log(Level.WARNING, "Failed to initialize secure connection.", e);
		}
	}
}