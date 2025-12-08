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

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

/**
 * Cloud client connector implementation for connecting to cloud services via MQTT.
 * 
 */
public class CloudClientConnector implements ICloudClient, IConnectionListener
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(CloudClientConnector.class.getName());
	
	// params
	
	private String topicPrefix = "";
	private MqttClientConnector mqttClient = null;
	private IDataMessageListener dataMsgListener = null;
	
	private int qosLevel = 1;
	
	// constructors
	
	/**
	 * Default constructor.
	 * 
	 */
	public CloudClientConnector()
	{
		super();
		
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		this.topicPrefix =
			configUtil.getProperty(ConfigConst.CLOUD_GATEWAY_SERVICE, ConfigConst.BASE_TOPIC_KEY);
		
		// Depending on the cloud service, the topic names may or may not begin with a "/", so this code
		// should be updated according to the cloud service provider's topic naming conventions
		if (topicPrefix == null) {
			topicPrefix = "/";
		} else {
			if (! topicPrefix.endsWith("/")) {
				topicPrefix += "/";
			}
		}
		
		_Logger.info("Cloud client connector initialized with topic prefix: " + this.topicPrefix);
	}
	
	// public methods
	
	@Override
	public boolean connectClient()
	{
		if (this.mqttClient == null) {
			this.mqttClient = new MqttClientConnector(ConfigConst.CLOUD_GATEWAY_SERVICE);
			this.mqttClient.setConnectionListener(this);
		}
		
		return this.mqttClient.connectClient();
	}
	
	@Override
	public boolean disconnectClient()
	{
		if (this.mqttClient != null && this.mqttClient.isConnected()) {
			return this.mqttClient.disconnectClient();
		}
		
		return false;
	}
	
	@Override
	public boolean sendEdgeDataToCloud(ResourceNameEnum resource, SensorData data)
	{
		if (resource != null && data != null) {
			String payload = DataUtil.getInstance().sensorDataToTimeAndValueJson(data);
			
			return publishMessageToCloud(resource, data.getName(), payload);
		}
		
		return false;
	}
	
	@Override
	public boolean sendEdgeDataToCloud(ResourceNameEnum resource, SystemPerformanceData data)
	{
		if (resource != null && data != null) {
			// send the CPU utilization reading as a SensorData representation
			SensorData cpuData = new SensorData();
			cpuData.updateData(data);
			cpuData.setName(ConfigConst.CPU_UTIL_NAME);
			cpuData.setValue(data.getCpuUtilization());
			
			boolean cpuDataSuccess = sendEdgeDataToCloud(resource, cpuData);
			
			if (! cpuDataSuccess) {
				_Logger.warning("Failed to send CPU utilization data to cloud service.");
			}
			
			// send the memory utilization reading as a SensorData representation
			SensorData memData = new SensorData();
			memData.updateData(data);
			memData.setName(ConfigConst.MEM_UTIL_NAME);
			memData.setValue(data.getMemoryUtilization());
			
			boolean memDataSuccess = sendEdgeDataToCloud(resource, memData);
			
			if (! memDataSuccess) {
				_Logger.warning("Failed to send memory utilization data to cloud service.");
			}
			
			return (cpuDataSuccess == memDataSuccess);
		}
		
		return false;
	}
	
	@Override
	public boolean subscribeToCloudEvents(ResourceNameEnum resource)
	{
		boolean success = false;
		
		String topicName = null;
		
		if (this.mqttClient != null && this.mqttClient.isConnected()) {
			topicName = createTopicName(resource);
			
			this.mqttClient.subscribeToTopic(resource, this.qosLevel);
			
			_Logger.info("Subscribed to cloud events for topic: " + topicName);
			
			success = true;
		} else {
			_Logger.warning("Subscription methods only available for MQTT. No MQTT connection to broker. Ignoring. Topic: " + topicName);
		}
		
		return success;
	}
	
	@Override
	public boolean unsubscribeFromCloudEvents(ResourceNameEnum resource)
	{
		boolean success = false;
		
		String topicName = null;
		
		if (this.mqttClient != null && this.mqttClient.isConnected()) {
			topicName = createTopicName(resource);
			
			this.mqttClient.unsubscribeFromTopic(resource);
			
			_Logger.info("Unsubscribed from cloud events for topic: " + topicName);
			
			success = true;
		} else {
			_Logger.warning("Unsubscribe method only available for MQTT. No MQTT connection to broker. Ignoring. Topic: " + topicName);
		}
		
		return success;
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
	
	@Override
	public void onConnect()
	{
		_Logger.info("Handling CSP subscriptions and device topic provisioning...");
		
		LedEnablementMessageListener ledListener = new LedEnablementMessageListener(this.dataMsgListener);
		
		// topic may not exist yet, so create a 'response' actuation event with invalid value -
		// this will create the relevant topic if it doesn't yet exist, which ensures
		// the message listener (if coded correctly) will log a message but ignore the
		// actuation command and NOT pass it onto the IDataMessageListener instance
		ActuatorData ad = new ActuatorData();
		ad.setAsResponse();
		ad.setName(ConfigConst.LED_ACTUATOR_NAME);
		ad.setValue((float) -1.0); // NOTE: this just needs to be an invalid actuation value

		String ledTopic = createTopicName(ledListener.getResource().getDeviceName(), ad.getName());

		String adJson = DataUtil.getInstance().actuatorDataToTimeAndValueJson(ad);

		this.publishMessageToCloud(ledTopic, adJson);
		
		this.mqttClient.subscribeToTopic(ledTopic, this.qosLevel, ledListener);
		
		_Logger.info("Subscribed to LED actuation topic: " + ledTopic);
	}
	
	@Override
	public void onDisconnect()
	{
		_Logger.info("MQTT client disconnected. Nothing else to do.");
	}
	
	// private methods
	
	/**
	 * Creates a topic name using the ResourceNameEnum.
	 * 
	 * @param resource The resource enum to use for topic name generation.
	 * @return String The generated topic name.
	 */
	private String createTopicName(ResourceNameEnum resource)
	{
		return createTopicName(resource.getDeviceName(), resource.getResourceType());
	}
	
	/**
	 * Creates a topic name using the resource and item name.
	 * 
	 * @param resource The resource enum to use.
	 * @param itemName The specific item name.
	 * @return String The generated topic name.
	 */
	private String createTopicName(ResourceNameEnum resource, String itemName)
	{
		return (createTopicName(resource) + "-" + itemName).toLowerCase();
	}
	
	/**
	 * Creates a topic name using the device name and resource type.
	 * 
	 * @param deviceName The device name to use.
	 * @param resourceTypeName The resource type name to use.
	 * @return String The generated topic name.
	 */
	private String createTopicName(String deviceName, String resourceTypeName)
	{
		StringBuilder buf = new StringBuilder();
		
		if (deviceName != null && deviceName.trim().length() > 0) {
			buf.append(topicPrefix).append(deviceName);
		}
		
		if (resourceTypeName != null && resourceTypeName.trim().length() > 0) {
			buf.append('/').append(resourceTypeName);
		}
		
		return buf.toString().toLowerCase();
	}
	
	/**
	 * Publishes a message to the cloud service using the resource and item name.
	 * 
	 * @param resource The resource enum to use.
	 * @param itemName The specific item name (e.g., sensor name).
	 * @param payload The payload to publish.
	 * @return boolean True on success, false otherwise.
	 */
	private boolean publishMessageToCloud(ResourceNameEnum resource, String itemName, String payload)
	{
		String topicName = createTopicName(resource) + "-" + itemName;
		
		return publishMessageToCloud(topicName, payload);
	}
	
	/**
	 * Publishes a message to the cloud service using the topic name and payload.
	 * 
	 * @param topicName The topic name to publish to.
	 * @param payload The payload to publish.
	 * @return boolean True on success, false otherwise.
	 */
	private boolean publishMessageToCloud(String topicName, String payload)
	{
		try {
			_Logger.finest("Publishing payload value(s) to CSP: " + topicName);
			
			this.mqttClient.publishMessage(topicName, payload.getBytes(), this.qosLevel);
			
			return true;
		} catch (Exception e) {
			_Logger.warning("Failed to publish message to CSP: " + topicName);
		}
		
		return false;
	}
	
	// Inner class for LED actuation message listener
	
	private class LedEnablementMessageListener implements IMqttMessageListener
	{
		private IDataMessageListener dataMsgListener = null;
		
		private ResourceNameEnum resource = ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE;
		
		private int    typeID   = ConfigConst.LED_ACTUATOR_TYPE;
		private String itemName = ConfigConst.LED_ACTUATOR_NAME;
		
		LedEnablementMessageListener(IDataMessageListener dataMsgListener)
		{
			this.dataMsgListener = dataMsgListener;
		}
		
		public ResourceNameEnum getResource()
		{
			return this.resource;
		}
		
		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception
		{
			try {
				String jsonData = new String(message.getPayload());
				
				ActuatorData actuatorData =
					DataUtil.getInstance().jsonToActuatorData(jsonData);
				
				// This will have to match the CDA's location ID
				actuatorData.setLocationID(ConfigConst.CONSTRAINED_DEVICE);
				actuatorData.setTypeID(this.typeID);
				actuatorData.setName(this.itemName);
				
				int val = (int) actuatorData.getValue();
				
				switch (val) {
					case ConfigConst.ON_COMMAND:
						_Logger.info("Received LED enablement message [ON].");
						actuatorData.setCommand(ConfigConst.ON_COMMAND);
						break;
						
					case ConfigConst.OFF_COMMAND:
						_Logger.info("Received LED enablement message [OFF].");
						actuatorData.setCommand(ConfigConst.OFF_COMMAND);
						break;
						
					default:
						_Logger.warning("Received invalid LED command value: " + val);
						return;
				}
				
				// Option 1: using JSON
				if (this.dataMsgListener != null) {
					jsonData = DataUtil.getInstance().actuatorDataToJson(actuatorData);
					
					this.dataMsgListener.handleIncomingMessage(
						ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, jsonData);
				}
			} catch (Exception e) {
				_Logger.log(Level.WARNING, "Failed to convert message payload to ActuatorData.", e);
			}
		}
	}
}