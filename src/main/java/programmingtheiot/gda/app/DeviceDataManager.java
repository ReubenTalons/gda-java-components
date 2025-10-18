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
package programmingtheiot.gda.app;

import java.util.logging.Level;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.data.SystemStateData;

import programmingtheiot.gda.connection.CloudClientConnector;
import programmingtheiot.gda.connection.CoapServerGateway;
import programmingtheiot.gda.connection.IPersistenceClient;
import programmingtheiot.gda.connection.IPubSubClient;
import programmingtheiot.gda.connection.IRequestResponseClient;
import programmingtheiot.gda.connection.MqttClientConnector;
import programmingtheiot.gda.connection.RedisPersistenceAdapter;
import programmingtheiot.gda.connection.SmtpClientConnector;
import programmingtheiot.gda.system.SystemPerformanceManager;

/**
 * Implementation of DeviceDataManager for the Gateway Device Application (GDA).
 * This class manages all data processing and communication connections.
 */
public class DeviceDataManager implements IDataMessageListener
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(DeviceDataManager.class.getName());
	
	// private var's
	
	private boolean enableMqttClient = true;
	private boolean enableCoapServer = false;
	private boolean enableCloudClient = false;
	private boolean enablePersistenceClient = false;
	private boolean enableSystemPerf = false;
	
	private IPubSubClient mqttClient = null;
	private IPubSubClient cloudClient = null;
	private IPersistenceClient persistenceClient = null;
	private RedisPersistenceAdapter redisClient = null;
	private CoapServerGateway coapServer = null;
	private SystemPerformanceManager sysPerfMgr = null;
	
	// constructors
	
	/**
	 * Default constructor.
	 * Initializes configuration and connection instances.
	 */
	public DeviceDataManager()
	{
		super();
		
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		this.enableMqttClient =
			configUtil.getBoolean(
				ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_MQTT_CLIENT_KEY);
		
		this.enableCoapServer =
			configUtil.getBoolean(
				ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_COAP_SERVER_KEY);
		
		this.enableCloudClient =
			configUtil.getBoolean(
				ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_CLOUD_CLIENT_KEY);
		
		this.enablePersistenceClient =
			configUtil.getBoolean(
				ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_PERSISTENCE_CLIENT_KEY);
		
		initManager();
	}
	
	
	// public methods
	
	@Override
	public boolean handleActuatorCommandResponse(ResourceNameEnum resourceName, ActuatorData data)
	{
		if (data != null) {
			_Logger.info("Handling actuator response: " + data.getName());
			
			// Store to Redis if persistence client is enabled
			if (this.redisClient != null && this.enablePersistenceClient) {
				String topic = resourceName.getResourceName();
				this.redisClient.storeData(topic, 0, data);
				_Logger.info("Stored ActuatorData to Redis: " + data.getName());
			}
			
			// this next call is optional for now
			//this.handleIncomingDataAnalysis(resourceName, data);
			
			if (data.hasError()) {
				_Logger.warning("Error flag set for ActuatorData instance.");
			}
			
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean handleActuatorCommandRequest(ResourceNameEnum resourceName, ActuatorData data)
	{
		if (data != null) {
			_Logger.info("Handling actuator command request: " + data.getName());
			
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean handleIncomingMessage(ResourceNameEnum resourceName, String msg)
	{
		if (msg != null) {
			_Logger.info("Handling incoming generic message: " + msg);
			
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean handleSensorMessage(ResourceNameEnum resourceName, SensorData data)
	{
		if (data != null) {
			_Logger.info("Handling sensor message: " + data.getName());
			
			// Store to Redis if persistence client is enabled
			if (this.redisClient != null && this.enablePersistenceClient) {
				String topic = resourceName.getResourceName();
				this.redisClient.storeData(topic, 0, data);
				_Logger.info("Stored SensorData to Redis: " + data.getName());
			}
			
			if (data.hasError()) {
				_Logger.warning("Error flag set for SensorData instance.");
			}
			
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean handleSystemPerformanceMessage(ResourceNameEnum resourceName, SystemPerformanceData data)
	{
		if (data != null) {
			_Logger.info("Handling system performance message: " + data.getName());
			
			// Store to Redis if persistence client is enabled
			if (this.redisClient != null && this.enablePersistenceClient) {
				String topic = resourceName.getResourceName();
				this.redisClient.storeData(topic, 0, data);
				_Logger.info("Stored SystemPerformanceData to Redis: " + data.getName());
			}
			
			if (data.hasError()) {
				_Logger.warning("Error flag set for SystemPerformanceData instance.");
			}
			
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public void setActuatorDataListener(String name, IActuatorDataListener listener)
	{
		// Not implemented yet - will be used in future labs
		_Logger.info("setActuatorDataListener() called with name: " + name);
	}
	
	/**
	 * Starts the DeviceDataManager and all enabled connections.
	 */
	public void startManager()
	{
		_Logger.info("Starting DeviceDataManager...");
		
		if (this.sysPerfMgr != null) {
			this.sysPerfMgr.startManager();
		}
		
		if (this.redisClient != null && this.enablePersistenceClient) {
			this.redisClient.connectClient();
		}
		
		_Logger.info("DeviceDataManager started.");
	}
	
	/**
	 * Stops the DeviceDataManager and all enabled connections.
	 */
	public void stopManager()
	{
		_Logger.info("Stopping DeviceDataManager...");
		
		if (this.sysPerfMgr != null) {
			this.sysPerfMgr.stopManager();
		}
		
		if (this.redisClient != null && this.enablePersistenceClient) {
			this.redisClient.disconnectClient();
		}
		
		_Logger.info("DeviceDataManager stopped.");
	}
	
	
	// private methods
	
	/**
	 * Initializes the enabled connections. This will NOT start them, but only create the
	 * instances that will be used in the startManager() and stopManager() methods.
	 */
	private void initManager()
	{
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		this.enableSystemPerf =
			configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_SYSTEM_PERF_KEY);
		
		if (this.enableSystemPerf) {
			this.sysPerfMgr = new SystemPerformanceManager();
			this.sysPerfMgr.setDataMessageListener(this);
		}
		
		if (this.enableMqttClient) {
			// TODO: implement this in Lab Module 7
		}
		
		if (this.enableCoapServer) {
			// TODO: implement this in Lab Module 8
		}
		
		if (this.enableCloudClient) {
			// TODO: implement this in Lab Module 10
		}
		
		if (this.enablePersistenceClient) {
			this.redisClient = new RedisPersistenceAdapter();
			_Logger.info("Redis persistence client created.");
		}
	}
	
	/**
	 * Handles incoming data analysis for ActuatorData.
	 * 
	 * @param resourceName The resource name.
	 * @param data The ActuatorData instance.
	 */
	private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, ActuatorData data)
	{
		_Logger.log(Level.FINE, "Analyzing incoming actuator data: " + data.getName());
		
		// TODO: implement analysis logic in future lab modules
	}
	
	/**
	 * Handles incoming data analysis for SystemStateData.
	 * 
	 * @param resourceName The resource name.
	 * @param data The SystemStateData instance.
	 */
	private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, SystemStateData data)
	{
		_Logger.log(Level.FINE, "Analyzing incoming system state data: " + data.getName());
		
		// TODO: implement analysis logic in future lab modules
	}
	
	/**
	 * Handles upstream transmission of data to cloud or other services.
	 * 
	 * @param resourceName The resource name.
	 * @param jsonData The JSON data to transmit.
	 * @param qos The quality of service level.
	 * @return true if successful, false otherwise.
	 */
	private boolean handleUpstreamTransmission(ResourceNameEnum resourceName, String jsonData, int qos)
	{
		_Logger.log(Level.FINE, "Handling upstream transmission for resource: " + resourceName);
		
		// TODO: implement transmission logic in future lab modules
		
		return false;
	}
	
}