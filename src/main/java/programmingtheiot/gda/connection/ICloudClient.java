/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

package programmingtheiot.gda.connection;

import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

/**
 * Interface contract for cloud clients.
 * 
 */
public interface ICloudClient
{
	/**
	 * Connects to the pub/sub broker / server using configuration parameters
	 * specified by the sub-class.
	 * 
	 * @return boolean True on success, False otherwise.
	 */
	public boolean connectClient();

	/**
	 * Disconnects from the pub/sub broker / server if the client is already connected.
	 * If not, this call is ignored, but will return a False.
	 * 
	 * @return boolean True on success, False otherwise.
	 */
	public boolean disconnectClient();

	/**
	 * Attempts to send the given sensor data instance to the remote cloud service.
	 * This will default to the pre-configured QoS level set in the configuration
	 * for the cloud service, and use the underlying implementation to send the data.
	 * 
	 * @param resource The resource enum to use for this send request.
	 * @param data The sensor data instance to publish to the remote cloud service.
	 * @return boolean True on success, False otherwise.
	 */
	public boolean sendEdgeDataToCloud(ResourceNameEnum resource, SensorData data);

	/**
	 * Attempts to send the given system performance data instance to the remote cloud service.
	 * This will default to the pre-configured QoS level set in the configuration
	 * for the cloud service, and use the underlying implementation to send the data.
	 * 
	 * @param resource The resource enum to use for this send request.
	 * @param data The system performance data instance to publish to the remote cloud service.
	 * @return boolean True on success, False otherwise.
	 */
	public boolean sendEdgeDataToCloud(ResourceNameEnum resource, SystemPerformanceData data);

	/**
	 * Attempts to subscribe to events destined for edge consumption that are
	 * sent by the remote cloud service.
	 * 
	 * @param resource The resource enum to use for this subscribe request.
	 * @return boolean True on success, False otherwise.
	 */
	public boolean subscribeToCloudEvents(ResourceNameEnum resource);

	/**
	 * Attempts to unsubscribe from events destined for edge consumption that are
	 * sent by the remote cloud service.
	 * 
	 * @param resource The resource enum to use for this unsubscribe request.
	 * @return boolean True on success, False otherwise.
	 */
	public boolean unsubscribeFromCloudEvents(ResourceNameEnum resource);

	/**
	 * Sets the data message listener reference, assuming listener is non-null.
	 * 
	 * @param listener The data message listener instance to use for passing relevant
	 * messages, such as those received from a subscription event.
	 * @return boolean True on success (if listener is non-null will always be the case), False otherwise.
	 */
	public boolean setDataMessageListener(IDataMessageListener listener); 
}