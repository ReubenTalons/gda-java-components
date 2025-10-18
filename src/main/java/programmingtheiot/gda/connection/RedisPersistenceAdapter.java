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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

/**
 * Redis-based persistence adapter for storing and retrieving IoT data.
 * Uses Jedis client for Redis connectivity.
 */
public class RedisPersistenceAdapter implements IPersistenceClient
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(RedisPersistenceAdapter.class.getName());
	
	// private var's
	
	private String host = ConfigConst.DEFAULT_HOST;
	private int port = ConfigConst.DEFAULT_REDIS_PORT;
	private Jedis jedis = null;
	private boolean useCloudClient = false;
	
	// constructors
	
	/**
	 * Default constructor.
	 * Initializes configuration from config file.
	 */
	public RedisPersistenceAdapter()
	{
		super();
		
		initConfig();
	}
	
	
	// public methods
	
	/**
	 * Connects to the Redis server.
	 * 
	 * @return true if connection successful, false otherwise
	 */
	@Override
	public boolean connectClient()
	{
		try {
			if (this.jedis != null && this.jedis.isConnected()) {
				_Logger.warning("Redis client already connected.");
				return true;
			}
			
			_Logger.info("Connecting to Redis server at " + this.host + ":" + this.port);
			
			this.jedis = new Jedis(this.host, this.port);
			
			// Authenticate with password
			String password = "pass,99word";
			this.jedis.auth(password);
			
			// Test connection with ping
			String response = this.jedis.ping();
			
			if ("PONG".equalsIgnoreCase(response)) {
				_Logger.info("Successfully connected to Redis server.");
				return true;
			} else {
				_Logger.warning("Unexpected response from Redis server: " + response);
				return false;
			}
		} catch (JedisException e) {
			_Logger.log(Level.SEVERE, "Failed to connect to Redis server.", e);
			return false;
		}
	}
	
	/**
	 * Disconnects from the Redis server.
	 * 
	 * @return true if disconnection successful, false otherwise
	 */
	@Override
	public boolean disconnectClient()
	{
		try {
			if (this.jedis == null || !this.jedis.isConnected()) {
				_Logger.warning("Redis client already disconnected.");
				return true;
			}
			
			_Logger.info("Disconnecting from Redis server.");
			
			this.jedis.close();
			this.jedis = null;
			
			_Logger.info("Successfully disconnected from Redis server.");
			return true;
		} catch (JedisException e) {
			_Logger.log(Level.SEVERE, "Failed to disconnect from Redis server.", e);
			return false;
		}
	}
	
	/**
	 * Retrieves ActuatorData from Redis based on topic and date range.
	 * 
	 * @param topic The topic/key to retrieve data from
	 * @param startDate Start date for data retrieval (currently not used)
	 * @param endDate End date for data retrieval (currently not used)
	 * @return Array of ActuatorData instances
	 */
	@Override
	public ActuatorData[] getActuatorData(String topic, Date startDate, Date endDate)
	{
		if (this.jedis == null || !this.jedis.isConnected()) {
			_Logger.warning("Redis client not connected. Cannot retrieve ActuatorData.");
			return null;
		}
		
		try {
			Set<String> keys = this.jedis.keys(topic + "*");
			List<ActuatorData> dataList = new ArrayList<>();
			
			for (String key : keys) {
				String jsonData = this.jedis.get(key);
				if (jsonData != null) {
					ActuatorData data = DataUtil.getInstance().jsonToActuatorData(jsonData);
					if (data != null) {
						dataList.add(data);
					}
				}
			}
			
			_Logger.info("Retrieved " + dataList.size() + " ActuatorData entries for topic: " + topic);
			return dataList.toArray(new ActuatorData[0]);
		} catch (JedisException e) {
			_Logger.log(Level.WARNING, "Failed to retrieve ActuatorData from Redis.", e);
			return null;
		}
	}
	
	/**
	 * Retrieves SensorData from Redis based on topic and date range.
	 * 
	 * @param topic The topic/key to retrieve data from
	 * @param startDate Start date for data retrieval (currently not used)
	 * @param endDate End date for data retrieval (currently not used)
	 * @return Array of SensorData instances
	 */
	@Override
	public SensorData[] getSensorData(String topic, Date startDate, Date endDate)
	{
		if (this.jedis == null || !this.jedis.isConnected()) {
			_Logger.warning("Redis client not connected. Cannot retrieve SensorData.");
			return null;
		}
		
		try {
			Set<String> keys = this.jedis.keys(topic + "*");
			List<SensorData> dataList = new ArrayList<>();
			
			for (String key : keys) {
				String jsonData = this.jedis.get(key);
				if (jsonData != null) {
					SensorData data = DataUtil.getInstance().jsonToSensorData(jsonData);
					if (data != null) {
						dataList.add(data);
					}
				}
			}
			
			_Logger.info("Retrieved " + dataList.size() + " SensorData entries for topic: " + topic);
			return dataList.toArray(new SensorData[0]);
		} catch (JedisException e) {
			_Logger.log(Level.WARNING, "Failed to retrieve SensorData from Redis.", e);
			return null;
		}
	}
	
	/**
	 * Registers a data storage listener (not implemented for Redis adapter).
	 */
	@Override
	public void registerDataStorageListener(Class cType, IPersistenceListener listener, String... topics)
	{
		// Not implemented for Redis adapter
		_Logger.info("registerDataStorageListener() not implemented for Redis adapter.");
	}
	
	/**
	 * Stores ActuatorData to Redis.
	 * 
	 * @param topic The topic/key to store data under
	 * @param qos Quality of service (not used for Redis)
	 * @param data ActuatorData instances to store
	 * @return true if storage successful, false otherwise
	 */
	@Override
	public boolean storeData(String topic, int qos, ActuatorData... data)
	{
		if (this.jedis == null || !this.jedis.isConnected()) {
			_Logger.warning("Redis client not connected. Cannot store ActuatorData.");
			return false;
		}
		
		if (data == null || data.length == 0) {
			_Logger.warning("No ActuatorData to store.");
			return false;
		}
		
		try {
			for (int i = 0; i < data.length; i++) {
				String jsonData = DataUtil.getInstance().actuatorDataToJson(data[i]);
				if (jsonData != null) {
					String key = topic + ":" + System.currentTimeMillis() + ":" + i;
					this.jedis.set(key, jsonData);
					// Set expiration to 24 hours (86400 seconds)
					this.jedis.expire(key, 86400);
				}
			}
			
			_Logger.info("Stored " + data.length + " ActuatorData entries to Redis with topic: " + topic);
			return true;
		} catch (JedisException e) {
			_Logger.log(Level.WARNING, "Failed to store ActuatorData to Redis.", e);
			return false;
		}
	}
	
	/**
	 * Stores SensorData to Redis.
	 * 
	 * @param topic The topic/key to store data under
	 * @param qos Quality of service (not used for Redis)
	 * @param data SensorData instances to store
	 * @return true if storage successful, false otherwise
	 */
	@Override
	public boolean storeData(String topic, int qos, SensorData... data)
	{
		if (this.jedis == null || !this.jedis.isConnected()) {
			_Logger.warning("Redis client not connected. Cannot store SensorData.");
			return false;
		}
		
		if (data == null || data.length == 0) {
			_Logger.warning("No SensorData to store.");
			return false;
		}
		
		try {
			for (int i = 0; i < data.length; i++) {
				String jsonData = DataUtil.getInstance().sensorDataToJson(data[i]);
				if (jsonData != null) {
					String key = topic + ":" + System.currentTimeMillis() + ":" + i;
					this.jedis.set(key, jsonData);
					// Set expiration to 24 hours (86400 seconds)
					this.jedis.expire(key, 86400);
				}
			}
			
			_Logger.info("Stored " + data.length + " SensorData entries to Redis with topic: " + topic);
			return true;
		} catch (JedisException e) {
			_Logger.log(Level.WARNING, "Failed to store SensorData to Redis.", e);
			return false;
		}
	}
	
	/**
	 * Stores SystemPerformanceData to Redis.
	 * 
	 * @param topic The topic/key to store data under
	 * @param qos Quality of service (not used for Redis)
	 * @param data SystemPerformanceData instances to store
	 * @return true if storage successful, false otherwise
	 */
	@Override
	public boolean storeData(String topic, int qos, SystemPerformanceData... data)
	{
		if (this.jedis == null || !this.jedis.isConnected()) {
			_Logger.warning("Redis client not connected. Cannot store SystemPerformanceData.");
			return false;
		}
		
		if (data == null || data.length == 0) {
			_Logger.warning("No SystemPerformanceData to store.");
			return false;
		}
		
		try {
			for (int i = 0; i < data.length; i++) {
				String jsonData = DataUtil.getInstance().systemPerformanceDataToJson(data[i]);
				if (jsonData != null) {
					String key = topic + ":" + System.currentTimeMillis() + ":" + i;
					this.jedis.set(key, jsonData);
					// Set expiration to 24 hours (86400 seconds)
					this.jedis.expire(key, 86400);
				}
			}
			
			_Logger.info("Stored " + data.length + " SystemPerformanceData entries to Redis with topic: " + topic);
			return true;
		} catch (JedisException e) {
			_Logger.log(Level.WARNING, "Failed to store SystemPerformanceData to Redis.", e);
			return false;
		}
	}
	
	
	// private methods
	
	/**
	 * Initializes configuration from the config file.
	 */
	private void initConfig()
	{
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		this.host = configUtil.getProperty(
			ConfigConst.DATA_GATEWAY_SERVICE, ConfigConst.HOST_KEY, ConfigConst.DEFAULT_HOST);
		
		this.port = configUtil.getInteger(
			ConfigConst.DATA_GATEWAY_SERVICE, ConfigConst.PORT_KEY, ConfigConst.DEFAULT_REDIS_PORT);
		
		_Logger.info("Redis persistence adapter configured: host=" + this.host + ", port=" + this.port);
	}
}