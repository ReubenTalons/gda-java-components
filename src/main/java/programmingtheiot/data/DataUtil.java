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
package programmingtheiot.data;

import java.nio.file.FileSystems;

import com.google.gson.Gson;

/**
 * Utility class for converting data objects to and from JSON.
 *
 */
public class DataUtil
{
	// static
	
	private static final DataUtil _Instance = new DataUtil();
	
	/**
	 * Returns the Singleton instance of this class.
	 * 
	 * @return DataUtil
	 */
	public static final DataUtil getInstance()
	{
		return _Instance;
	}
	
	
	// private var's
	
	private Gson gson = null;
	
	
	// constructors
	
	/**
	 * Default (private).
	 * 
	 */
	private DataUtil()
	{
		super();
		
		this.gson = new Gson();
	}
	
	
	// public methods
	
	/**
	 * Converts ActuatorData to JSON string.
	 * 
	 * @param actuatorData The ActuatorData instance to convert.
	 * @return String The JSON representation, or null if actuatorData is null.
	 */
	public String actuatorDataToJson(ActuatorData actuatorData)
	{
		String jsonData = null;
		
		if (actuatorData != null) {
			jsonData = this.gson.toJson(actuatorData);
		}
		
		return jsonData;
	}
	
	/**
	 * Converts ActuatorData to simplified time and value JSON format.
	 * 
	 * @param actuatorData The ActuatorData instance to convert.
	 * @return String The JSON representation with time and value only, or null if actuatorData is null.
	 */
	public String actuatorDataToTimeAndValueJson(ActuatorData actuatorData)
	{
		String jsonData = null;
		
		if (actuatorData != null) {
			TimeAndValuePayloadData tvData = new TimeAndValuePayloadData(actuatorData);
			jsonData = this.gson.toJson(tvData);
		}
		
		return jsonData;
	}
	
	/**
	 * Converts SensorData to JSON string.
	 * 
	 * @param sensorData The SensorData instance to convert.
	 * @return String The JSON representation, or null if sensorData is null.
	 */
	public String sensorDataToJson(SensorData sensorData)
	{
		String jsonData = null;
		
		if (sensorData != null) {
			jsonData = this.gson.toJson(sensorData);
		}
		
		return jsonData;
	}
	
	/**
	 * Converts SensorData to simplified time and value JSON format.
	 * 
	 * @param sensorData The SensorData instance to convert.
	 * @return String The JSON representation with time and value only, or null if sensorData is null.
	 */
	public String sensorDataToTimeAndValueJson(SensorData sensorData)
	{
		String jsonData = null;
		
		if (sensorData != null) {
			TimeAndValuePayloadData tvData = new TimeAndValuePayloadData(sensorData);
			jsonData = this.gson.toJson(tvData);
		}
		
		return jsonData;
	}
	
	/**
	 * Converts SystemPerformanceData to JSON string.
	 * 
	 * @param sysPerfData The SystemPerformanceData instance to convert.
	 * @return String The JSON representation, or null if sysPerfData is null.
	 */
	public String systemPerformanceDataToJson(SystemPerformanceData sysPerfData)
	{
		String jsonData = null;
		
		if (sysPerfData != null) {
			jsonData = this.gson.toJson(sysPerfData);
		}
		
		return jsonData;
	}
	
	/**
	 * Converts SystemStateData to JSON string.
	 * 
	 * @param sysStateData The SystemStateData instance to convert.
	 * @return String The JSON representation, or null if sysStateData is null.
	 */
	public String systemStateDataToJson(SystemStateData sysStateData)
	{
		String jsonData = null;
		
		if (sysStateData != null) {
			jsonData = this.gson.toJson(sysStateData);
		}
		
		return jsonData;
	}
	
	/**
	 * Converts JSON string to ActuatorData.
	 * 
	 * @param jsonData The JSON string to convert.
	 * @return ActuatorData The ActuatorData instance, or null if jsonData is null or invalid.
	 */
	public ActuatorData jsonToActuatorData(String jsonData)
	{
		ActuatorData data = null;
		
		if (jsonData != null && jsonData.trim().length() > 0) {
			data = this.gson.fromJson(jsonData, ActuatorData.class);
		}
		
		return data;
	}
	
	/**
	 * Converts JSON string to SensorData.
	 * 
	 * @param jsonData The JSON string to convert.
	 * @return SensorData The SensorData instance, or null if jsonData is null or invalid.
	 */
	public SensorData jsonToSensorData(String jsonData)
	{
		SensorData data = null;
		
		if (jsonData != null && jsonData.trim().length() > 0) {
			data = this.gson.fromJson(jsonData, SensorData.class);
		}
		
		return data;
	}
	
	/**
	 * Converts JSON string to SystemPerformanceData.
	 * 
	 * @param jsonData The JSON string to convert.
	 * @return SystemPerformanceData The SystemPerformanceData instance, or null if jsonData is null or invalid.
	 */
	public SystemPerformanceData jsonToSystemPerformanceData(String jsonData)
	{
		SystemPerformanceData data = null;
		
		if (jsonData != null && jsonData.trim().length() > 0) {
			data = this.gson.fromJson(jsonData, SystemPerformanceData.class);
		}
		
		return data;
	}
	
	/**
	 * Converts JSON string to SystemStateData.
	 * 
	 * @param jsonData The JSON string to convert.
	 * @return SystemStateData The SystemStateData instance, or null if jsonData is null or invalid.
	 */
	public SystemStateData jsonToSystemStateData(String jsonData)
	{
		SystemStateData data = null;
		
		if (jsonData != null && jsonData.trim().length() > 0) {
			data = this.gson.fromJson(jsonData, SystemStateData.class);
		}
		
		return data;
	}
	
}