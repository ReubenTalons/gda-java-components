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

import programmingtheiot.gda.connection.CloudClientConnector;
import programmingtheiot.gda.connection.CoapClientConnector;
import programmingtheiot.gda.connection.CoapServerGateway;
import programmingtheiot.gda.connection.ICloudClient;
import programmingtheiot.gda.connection.IPersistenceClient;
import programmingtheiot.gda.connection.IPubSubClient;
import programmingtheiot.gda.connection.IRequestResponseClient;
import programmingtheiot.gda.connection.MqttClientConnector;

/**
 * DeviceDataManager implementation for GDA.
 *
 */
public class DeviceDataManager implements IDataMessageListener
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(DeviceDataManager.class.getName());
	
	// private var's
	
	private boolean enableMqttClient = false;
	private boolean enableCoapServer = false;
	private boolean enableCoapClient = false;
	private boolean enableCloudClient = false;
	private boolean enableSmtpClient = false;
	private boolean enablePersistenceClient = false;
	
	private IActuatorDataListener actuatorDataListener = null;
	private MqttClientConnector mqttClient = null;
	private CoapClientConnector coapClient = null;
	private ICloudClient cloudClient = null;  // Changed from IPubSubClient to ICloudClient
	private IPersistenceClient persistenceClient = null;
	private IRequestResponseClient smtpClient = null;
	private CoapServerGateway coapServer = null;
	
	// constructors
	
	public DeviceDataManager()
	{
		super();
		
		initConnections();
	}
	
	public DeviceDataManager(
		boolean enableMqttClient,
		boolean enableCoapClient,
		boolean enableCloudClient,
		boolean enableSmtpClient,
		boolean enablePersistenceClient)
	{
		super();
		
		initConnections();
	}
	
	
	// public methods
	
	@Override
	public boolean handleActuatorCommandResponse(ResourceNameEnum resourceName, ActuatorData data)
	{
		if (data != null) {
			_Logger.info("Handling actuator response: " + data.getName());
			
			// TODO: Add any necessary logic for handling actuator responses
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean handleActuatorCommandRequest(ResourceNameEnum resourceName, ActuatorData data)
	{
		if (data != null) {
			_Logger.log(
				Level.FINE,
				"Actuator request received: {0}. Message: {1}",
				new Object[] {resourceName.getResourceName(), Integer.valueOf((data.getCommand()))});
			
			if (data.hasError()) {
				_Logger.warning("Error flag set for ActuatorData instance.");
			}
			
			// TODO: Implement logic to send actuator command to CDA
			// This would typically forward the command to the CDA via MQTT or CoAP
			
			int qos = ConfigConst.DEFAULT_QOS;
			
			// For now, just log the command
			_Logger.info("Processing actuator command for: " + data.getName());
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean handleIncomingMessage(ResourceNameEnum resourceName, String msg)
	{
		if (resourceName != null && msg != null) {
			try {
				if (resourceName == ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE) {
					_Logger.info("Handling incoming ActuatorData message: " + msg);
					
					// Convert JSON to ActuatorData for validation
					ActuatorData ad = DataUtil.getInstance().jsonToActuatorData(msg);
					
					// Convert back to JSON (validates the data is proper ActuatorData)
					String jsonData = DataUtil.getInstance().actuatorDataToJson(ad);
					
					// Forward to CDA via MQTT
					if (this.mqttClient != null) {
						int qos = ConfigConst.DEFAULT_QOS;
						
						_Logger.fine("Publishing ActuatorData to CDA via MQTT: " + jsonData);
						return this.mqttClient.publishMessage(resourceName, jsonData, qos);
					}
					
					// TODO: Add CoAP support if needed
					// if (this.coapClient != null) {
					//     return this.coapClient.sendPostRequest(...);
					// }
					
				} else {
					_Logger.warning("Failed to parse incoming message. Unknown resource type: " + resourceName);
					return false;
				}
			} catch (Exception e) {
				_Logger.log(Level.WARNING, "Failed to process incoming message for resource: " + resourceName, e);
			}
		} else {
			_Logger.warning("Incoming message has no data. Ignoring for resource: " + resourceName);
		}
		
		return false;
	}
	
	@Override
	public boolean handleSensorMessage(ResourceNameEnum resourceName, SensorData data)
	{
		if (data != null) {
			_Logger.fine("Handling sensor message: " + data.getName());
			
			if (data.hasError()) {
				_Logger.warning("Error flag set for SensorData instance.");
			}
			
			String jsonData = DataUtil.getInstance().sensorDataToJson(data);
			
			_Logger.fine("JSON [SensorData] -> " + jsonData);
			
			int qos = ConfigConst.DEFAULT_QOS;
			
			// Store data if persistence is enabled
			if (this.enablePersistenceClient && this.persistenceClient != null) {
				// this.persistenceClient.storeData(resourceName.getResourceName(), qos, data);
			}
			
			// Send data upstream to cloud
			this.handleUpstreamTransmission(resourceName, data, qos);
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean handleSystemPerformanceMessage(ResourceNameEnum resourceName, SystemPerformanceData data)
	{
		if (data != null) {
			_Logger.info("Handling system performance message: " + data.getName());
			
			if (data.hasError()) {
				_Logger.warning("Error flag set for SystemPerformanceData instance.");
			}
			
			int qos = ConfigConst.DEFAULT_QOS;
			
			// Store data if persistence is enabled
			// NOTE: You may want to persist your SystemPerformanceData here
			
			// Send data upstream to cloud
			this.handleUpstreamTransmission(resourceName, data, qos);
			
			return true;
		}
		
		return false;
	}
	
	public void setActuatorDataListener(String name, IActuatorDataListener listener)
	{
		if (listener != null) {
			// For now, just ignore 'name' - if you need more than one listener,
			// you can use 'name' to create a map of listener instances
			this.actuatorDataListener = listener;
			_Logger.info("Actuator data listener registered: " + name);
		}
	}
	
	public void startManager()
	{
		_Logger.info("Starting DeviceDataManager...");
		
		// Start cloud client FIRST (before MQTT client)
		if (this.enableCloudClient && this.cloudClient != null) {
			if (this.cloudClient.connectClient()) {
				_Logger.info("Successfully connected cloud client to cloud service.");
			} else {
				_Logger.severe("Failed to connect cloud client to cloud service.");
			}
		}
		
		if (this.mqttClient != null) {
			if (this.mqttClient.connectClient()) {
				_Logger.info("Successfully connected MQTT client to broker.");
				
				// add necessary subscriptions
				
				// TODO: read this from the configuration file
				int qos = ConfigConst.DEFAULT_QOS;
				
				// TODO: check the return value for each and take appropriate action
				
				// IMPORTANT NOTE: The 'subscribeToTopic()' method calls shown
				// below will be moved to MqttClientConnector.connectComplete()
				// in Lab Module 10. For now, they can remain here.
				this.mqttClient.subscribeToTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, qos);
				this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE, qos);
				this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, qos);
				this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, qos);
			} else {
				_Logger.severe("Failed to connect MQTT client to broker.");
				
				// TODO: take appropriate action
			}
		}
		
		// NEW: Start CoAP server if enabled
		if (this.enableCoapServer && this.coapServer != null) {
			if (this.coapServer.startServer()) {
				_Logger.info("CoAP server started.");
			} else {
				_Logger.severe("Failed to start CoAP server. Check log file for details.");
			}
		}
		
		_Logger.info("Started DeviceDataManager.");
	}
	
	public void stopManager()
	{
		_Logger.info("Stopping DeviceDataManager...");
		
		if (this.mqttClient != null) {
			// add necessary un-subscribes
			
			// TODO: check the return value for each and take appropriate action
			
			// NOTE: The unsubscribeFromTopic() method calls below should match with
			// the subscribeToTopic() method calls from startManager(). Also, the
			// unsubscribe logic below can be moved to MqttClientConnector's
			// disconnectClient() call PRIOR to actually disconnecting from
			// the MQTT broker.
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE);
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE);
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE);
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE);
			
			if (this.mqttClient.disconnectClient()) {
				_Logger.info("Successfully disconnected MQTT client from broker.");
			} else {
				_Logger.severe("Failed to disconnect MQTT client from broker.");
				
				// TODO: take appropriate action
			}
		}
		
		// Disconnect cloud client
		if (this.enableCloudClient && this.cloudClient != null) {
			// Unsubscribe from any cloud topics if needed
			// this.cloudClient.unsubscribeFromCloudEvents(...);
			
			if (this.cloudClient.disconnectClient()) {
				_Logger.info("Successfully disconnected cloud client from cloud service.");
			} else {
				_Logger.severe("Failed to disconnect cloud client from cloud service.");
			}
		}
		
		// NEW: Stop CoAP server if enabled
		if (this.enableCoapServer && this.coapServer != null) {
			if (this.coapServer.stopServer()) {
				_Logger.info("CoAP server stopped.");
			} else {
				_Logger.severe("Failed to stop CoAP server. Check log file for details.");
			}
		}
		
		_Logger.info("Stopped DeviceDataManager.");
	}
	
	// private methods
	
	/**
	 * Initializes the enabled connections. This will NOT start them, but only create the
	 * instances that will be used in the {@link #startManager() and #stopManager()) methods.
	 * 
	 */
	private void initConnections()
	{
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		this.enableMqttClient =
			configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_MQTT_CLIENT_KEY);
		
		// NOTE: This is new - creating the MQTT client connector instance
		if (this.enableMqttClient) {
			this.mqttClient = new MqttClientConnector();
			
			// NOTE: The next line isn't technically needed until Lab Module 10
			this.mqttClient.setDataMessageListener(this);
			
			_Logger.info("MQTT client enabled and initialized.");
		}
		
		// NEW: Initialize CoAP server if enabled
		this.enableCoapServer =
			configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_COAP_SERVER_KEY);
		
		if (this.enableCoapServer) {
			this.coapServer = new CoapServerGateway(this);
			_Logger.info("CoAP server enabled and initialized.");
		}
		
		// NEW: Initialize CoAP client if enabled
		this.enableCoapClient =
			configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_COAP_CLIENT_KEY);
		
		if (this.enableCoapClient) {
			this.coapClient = new CoapClientConnector();
			this.coapClient.setDataMessageListener(this);
			_Logger.info("CoAP client enabled and initialized.");
		}
		
		// NEW: Initialize cloud client if enabled (Lab Module 11)
		this.enableCloudClient =
			configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_CLOUD_CLIENT_KEY);
		
		if (this.enableCloudClient) {
			this.cloudClient = new CloudClientConnector();
			_Logger.info("Cloud client enabled and initialized.");
		}
		
		if (this.enablePersistenceClient) {
			// TODO: implement this as an optional exercise in Lab Module 5
		}
	}
	
	/**
	 * Handles upstream transmission of sensor data to cloud service.
	 * 
	 * @param resourceName The resource name
	 * @param data The sensor data to send
	 * @param qos The QoS level
	 */
	private void handleUpstreamTransmission(ResourceNameEnum resourceName, SensorData data, int qos)
	{
		_Logger.fine("Sending sensor data to cloud service: " + resourceName);
		
		if (this.cloudClient != null) {
			if (this.cloudClient.sendEdgeDataToCloud(resourceName, data)) {
				_Logger.fine("Sent sensor data upstream to cloud service.");
			} else {
				_Logger.warning("Failed to send sensor data to cloud service.");
			}
		}
	}
	
	/**
	 * Handles upstream transmission of system performance data to cloud service.
	 * 
	 * @param resourceName The resource name
	 * @param data The system performance data to send
	 * @param qos The QoS level
	 */
	private void handleUpstreamTransmission(ResourceNameEnum resourceName, SystemPerformanceData data, int qos)
	{
		_Logger.fine("Sending system performance data to cloud service: " + resourceName);
		
		if (this.cloudClient != null) {
			if (this.cloudClient.sendEdgeDataToCloud(resourceName, data)) {
				_Logger.fine("Sent system performance data upstream to cloud service.");
			} else {
				_Logger.warning("Failed to send system performance data to cloud service.");
			}
		}
	}
	
}