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
 * Cloud client connector implementation that delegates MQTT communication
 * to MqttClientConnector for cloud service integration.
 * Implements IConnectionListener to handle connection events.
 * 
 */
public class CloudClientConnector implements ICloudClient, IConnectionListener
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(CloudClientConnector.class.getName());
	
	// private var's
	
	private String topicPrefix = "";
	private MqttClientConnector mqttClient = null;
	private IDataMessageListener dataMsgListener = null;
	
	// Set to either 0 or 1, depending on which is preferred for your implementation
	private int qosLevel = 1;
	
	// constructors
	
	/**
	 * Default constructor.
	 * Initializes the topic prefix from configuration.
	 */
	public CloudClientConnector()
	{
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
		
		_Logger.info("Topic prefix set to: " + this.topicPrefix);
	}
	
	// public methods
	
	/**
	 * Connects to the cloud service via MQTT.
	 * Sets this instance as the connection listener.
	 */
	@Override
	public boolean connectClient()
	{
		if (this.mqttClient == null) {
			// Use the Cloud Gateway Service configuration
			this.mqttClient = new MqttClientConnector(ConfigConst.CLOUD_GATEWAY_SERVICE);
			
			// Set this CloudClientConnector as the connection listener
			this.mqttClient.setConnectionListener(this);
		}
		
		// NOTE: If MqttClientConnector is using the async client, we won't have a complete
		// connection to the cloud-hosted MQTT broker until MqttClientConnector's
		// connectComplete() callback is invoked.
		return this.mqttClient.connectClient();
	}
	
	/**
	 * Disconnects from the cloud service.
	 */
	@Override
	public boolean disconnectClient()
	{
		if (this.mqttClient != null && this.mqttClient.isConnected()) {
			return this.mqttClient.disconnectClient();
		}
		
		return false;
	}
	
	/**
	 * Sends sensor data to the cloud service.
	 * 
	 * @param resource The resource enum identifying the data type
	 * @param data The sensor data to send
	 * @return true if successful, false otherwise
	 */
	@Override
	public boolean sendEdgeDataToCloud(ResourceNameEnum resource, SensorData data)
	{
		if (resource != null && data != null) {
			// NOTE: Using TimeAndValuePayloadData format for Ubidots compatibility
			String payload = DataUtil.getInstance().sensorDataToTimeAndValueJson(data);
			
			return publishMessageToCloud(resource, data.getName(), payload);
		}
		
		return false;
	}
	
	/**
	 * Sends system performance data to the cloud service.
	 * Splits the data into separate CPU and memory readings.
	 * 
	 * @param resource The resource enum identifying the data type
	 * @param data The system performance data to send
	 * @return true if both CPU and memory data sent successfully, false otherwise
	 */
	@Override
	public boolean sendEdgeDataToCloud(ResourceNameEnum resource, SystemPerformanceData data)
	{
		if (resource != null && data != null) {
			// Send CPU utilization as a SensorData representation
			SensorData cpuData = new SensorData();
			cpuData.updateData(data);
			cpuData.setName(ConfigConst.CPU_UTIL_NAME);
			cpuData.setValue(data.getCpuUtilization());
			
			boolean cpuDataSuccess = sendEdgeDataToCloud(resource, cpuData);
			
			if (! cpuDataSuccess) {
				_Logger.warning("Failed to send CPU utilization data to cloud service.");
			}
			
			// Send memory utilization as a SensorData representation
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
	
	/**
	 * Subscribes to cloud events for the given resource.
	 * 
	 * @param resource The resource to subscribe to
	 * @return true if successful, false otherwise
	 */
	@Override
	public boolean subscribeToCloudEvents(ResourceNameEnum resource)
	{
		boolean success = false;
		
		String topicName = null;
		
		if (this.mqttClient != null && this.mqttClient.isConnected()) {
			topicName = createTopicName(resource);
			
			// NOTE: This is a generic subscribe call - if you use this approach,
			// you will need to update this.mqttClient.messageReceived() to
			//   (1) identify the message source (e.g., CDA or Cloud), 
			//   (2) determine the message type (e.g., actuator command), and
			//   (3) convert the payload into a data container (e.g., ActuatorData)
			this.mqttClient.subscribeToTopic(topicName, this.qosLevel);
			
			success = true;
		} else {
			_Logger.warning("Subscription methods only available for MQTT. No MQTT connection to broker. Ignoring. Topic: " + topicName);
		}
		
		return success;
	}
	
	/**
	 * Unsubscribes from cloud events for the given resource.
	 * 
	 * @param resource The resource to unsubscribe from
	 * @return true if successful, false otherwise
	 */
	@Override
	public boolean unsubscribeFromCloudEvents(ResourceNameEnum resource)
	{
		boolean success = false;
		
		String topicName = null;
		
		if (this.mqttClient != null && this.mqttClient.isConnected()) {
			topicName = createTopicName(resource);
			
			this.mqttClient.unsubscribeFromTopic(topicName);
			
			success = true;
		} else {
			_Logger.warning("Unsubscribe method only available for MQTT. No MQTT connection to broker. Ignoring. Topic: " + topicName);
		}
		
		return success;
	}
	
	/**
	 * Sets the data message listener for handling incoming cloud messages.
	 * 
	 * @param listener The data message listener
	 * @return true if successful, false otherwise
	 */
	@Override
	public boolean setDataMessageListener(IDataMessageListener listener)
	{
		if (listener != null) {
			this.dataMsgListener = listener;
			return true;
		}
		
		return false;
	}
	
	// IConnectionListener implementation
	
	/**
	 * Called when MQTT connection to cloud service is complete.
	 * Sets up subscriptions to LED actuation events from the cloud.
	 */
	@Override
	public void onConnect()
	{
		_Logger.info("Handling cloud service subscriptions and device topic provisioning...");
		
		// Create LED actuation listener
		LedEnablementMessageListener ledListener = new LedEnablementMessageListener(this.dataMsgListener);
		
		// Create LED topic - topic may not exist yet, so create a 'response' actuation event with invalid value
		// This will create the relevant topic if it doesn't yet exist
		ActuatorData ad = new ActuatorData();
		ad.setAsResponse();
		ad.setName(ConfigConst.LED_ACTUATOR_NAME);
		ad.setValue((float) -1.0); // Invalid actuation value to ensure it's ignored
		
		String ledTopic = createTopicName(ledListener.getResource().getDeviceName(), ad.getName());
		
		_Logger.info("Creating LED topic (if needed): " + ledTopic);
		
		// Publish initial message to create topic
		String adJson = DataUtil.getInstance().actuatorDataToTimeAndValueJson(ad);
		this.publishMessageToCloud(ledTopic, adJson);
		
		// Subscribe to LED actuation events from cloud
		this.mqttClient.subscribeToTopic(ledTopic, this.qosLevel, ledListener);
		
		_Logger.info("Subscribed to LED actuation topic: " + ledTopic);
	}
	
	/**
	 * Called when MQTT connection to cloud service is disconnected.
	 */
	@Override
	public void onDisconnect()
	{
		_Logger.info("Cloud service MQTT client disconnected. Nothing else to do.");
	}
	
	// private methods
	
	/**
	 * Creates a topic name from a ResourceNameEnum.
	 * 
	 * @param resource The resource enum
	 * @return The formatted topic name
	 */
	private String createTopicName(ResourceNameEnum resource)
	{
		return createTopicName(resource.getDeviceName(), resource.getResourceType());
	}
	
	/**
	 * Creates a topic name from a ResourceNameEnum and item name.
	 * 
	 * @param resource The resource enum
	 * @param itemName The item name
	 * @return The formatted topic name
	 */
	private String createTopicName(ResourceNameEnum resource, String itemName)
	{
		return (createTopicName(resource) + "-" + itemName).toLowerCase();
	}
	
	/**
	 * Creates a topic name from device name and resource type.
	 * 
	 * @param deviceName The device name
	 * @param resourceTypeName The resource type name
	 * @return The formatted topic name
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
	 * Publishes a message to the cloud with resource and item name.
	 * 
	 * @param resource The resource enum
	 * @param itemName The specific item name (e.g., sensor name)
	 * @param payload The message payload
	 * @return true if successful, false otherwise
	 */
	private boolean publishMessageToCloud(ResourceNameEnum resource, String itemName, String payload)
	{
		String topicName = createTopicName(resource) + "-" + itemName;
		
		return publishMessageToCloud(topicName, payload);
	}
	
	/**
	 * Publishes a message to the cloud service.
	 * 
	 * @param topicName The topic to publish to
	 * @param payload The message payload
	 * @return true if successful, false otherwise
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
	
	// Inner class: LED Enablement Message Listener
	
	/**
	 * Inner class to handle LED actuation events from the cloud service.
	 * Processes incoming MQTT messages and converts them to ActuatorData commands.
	 */
	private class LedEnablementMessageListener implements IMqttMessageListener
	{
		private IDataMessageListener dataMsgListener = null;
		
		private ResourceNameEnum resource = ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE;
		
		private int    typeID   = ConfigConst.LED_ACTUATOR_TYPE;
		private String itemName = ConfigConst.LED_ACTUATOR_NAME;
		
		/**
		 * Constructor.
		 * 
		 * @param dataMsgListener The data message listener to forward actuator commands to
		 */
		LedEnablementMessageListener(IDataMessageListener dataMsgListener)
		{
			this.dataMsgListener = dataMsgListener;
		}
		
		/**
		 * Gets the resource name for this listener.
		 * 
		 * @return The resource enum
		 */
		public ResourceNameEnum getResource()
		{
			return this.resource;
		}
		
		/**
		 * Callback invoked when an LED actuation message arrives from the cloud.
		 * Parses the message and forwards it to the CDA via DeviceDataManager.
		 */
		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception
		{
			try {
				String jsonData = new String(message.getPayload());
				
				_Logger.info("LED actuation message received from cloud: " + jsonData);
				
				// Convert JSON to ActuatorData
				ActuatorData actuatorData = DataUtil.getInstance().jsonToActuatorData(jsonData);
				
				// Set required fields for CDA processing
				actuatorData.setLocationID(ConfigConst.CONSTRAINED_DEVICE);
				actuatorData.setTypeID(this.typeID);
				actuatorData.setName(this.itemName);
				
				int val = (int) actuatorData.getValue();
				
				// Process the actuation command
				switch (val) {
					case ConfigConst.ON_COMMAND:
						_Logger.info("Received LED enablement message [ON].");
						actuatorData.setCommand(ConfigConst.ON_COMMAND);
						actuatorData.setStateData("LED switching ON");
						break;
						
					case ConfigConst.OFF_COMMAND:
						_Logger.info("Received LED enablement message [OFF].");
						actuatorData.setCommand(ConfigConst.OFF_COMMAND);
						actuatorData.setStateData("LED switching OFF");
						break;
						
					default:
						_Logger.warning("Invalid LED command value: " + val + ". Ignoring.");
						return;
				}
				
				// Forward ActuatorData to DeviceDataManager via handleIncomingMessage
				if (this.dataMsgListener != null) {
					// Convert ActuatorData back to JSON for validation and forwarding
					jsonData = DataUtil.getInstance().actuatorDataToJson(actuatorData);
					
					_Logger.info("Forwarding LED actuation command to CDA via DeviceDataManager.");
					
					// Send to DeviceDataManager which will forward to CDA
					this.dataMsgListener.handleIncomingMessage(
						ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, jsonData);
				} else {
					_Logger.warning("No data message listener set. Cannot forward LED actuation command.");
				}
			} catch (Exception e) {
				_Logger.log(Level.WARNING, "Failed to process LED actuation message from cloud.", e);
			}
		}
	}
}