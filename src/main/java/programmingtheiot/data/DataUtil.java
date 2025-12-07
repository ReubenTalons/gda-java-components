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
 * Shell representation of class for student implementation.
 *
 */
public class DataUtil
{
	// static
	
	private static final DataUtil _Instance = new DataUtil();
	
	/**
	 * Returns the Singleton instance of this class.
	 * 
	 * @return ConfigUtil
	 */
	public static final DataUtil getInstance()
	{
		return _Instance;
	}
	
	
	// private var's
	
	
	// constructors
	
	/**
	 * Default (private).
	 * 
	 */
	private DataUtil()
	{
		super();
	}
	
	
	// public methods
	
	public String actuatorDataToJson(ActuatorData actuatorData)
	{
		return null;
	}
	
	/**
	 * Converts ActuatorData to simplified JSON format with only time and value.
	 * This is useful for cloud services that require minimal payload format.
	 * 
	 * @param actuatorData The ActuatorData instance to convert
	 * @return String JSON representation with time and value, or null if data is null
	 */
	public String actuatorDataToTimeAndValueJson(ActuatorData actuatorData)
	{
		String jsonData = null;
		
		if (actuatorData != null) {
			Gson gson = new Gson();
			TimeAndValuePayloadData tvData = new TimeAndValuePayloadData(actuatorData);
			jsonData = gson.toJson(tvData);
		}
	
		return jsonData;
	}
	
	public String sensorDataToJson(SensorData sensorData)
	{
		return null;
	}
	
	/**
	 * Converts SensorData to simplified JSON format with only time and value.
	 * This is useful for cloud services that require minimal payload format.
	 * 
	 * @param sensorData The SensorData instance to convert
	 * @return String JSON representation with time and value, or null if data is null
	 */
	public String sensorDataToTimeAndValueJson(SensorData sensorData)
	{
		String jsonData = null;
		
		if (sensorData != null) {
			Gson gson = new Gson();
			TimeAndValuePayloadData tvData = new TimeAndValuePayloadData(sensorData);
			jsonData = gson.toJson(tvData);
		}
	
		return jsonData;
	}
	
	public String systemPerformanceDataToJson(SystemPerformanceData sysPerfData)
	{
		return null;
	}
	
	public String systemStateDataToJson(SystemStateData sysStateData)
	{
		return null;
	}
	
	public ActuatorData jsonToActuatorData(String jsonData)
	{
		return null;
	}
	
	public SensorData jsonToSensorData(String jsonData)
	{
		return null;
	}
	
	public SystemPerformanceData jsonToSystemPerformanceData(String jsonData)
	{
		return null;
	}
	
	public SystemStateData jsonToSystemStateData(String jsonData)
	{
		return null;
	}
	
}