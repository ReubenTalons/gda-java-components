/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 */
package programmingtheiot.gda.connection.handlers;

import java.util.logging.Logger;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;

import programmingtheiot.common.IDataMessageListener;

/**
 * Observer handler for SensorData updates from CoAP server.
 */
public class SensorDataObserverHandler implements CoapHandler
{
	private static final Logger _Logger =
		Logger.getLogger(SensorDataObserverHandler.class.getName());
	
	private IDataMessageListener dataMsgListener = null;
	
	/**
	 * Default constructor.
	 */
	public SensorDataObserverHandler()
	{
		super();
	}
	
	/**
	 * Set the data message listener.
	 * 
	 * @param listener The data message listener
	 */
	public void setDataMessageListener(IDataMessageListener listener)
	{
		this.dataMsgListener = listener;
	}
	
	@Override
	public void onError()
	{
		_Logger.warning("Handling CoAP error for SensorData observation...");
	}
	
	@Override
	public void onLoad(CoapResponse response)
	{
		if (response != null) {
			_Logger.info("Received CoAP response (payload should be SensorData in JSON): " + response.getResponseText());
			
			// TODO: Parse SensorData and send to listener in future activities
			if (this.dataMsgListener != null) {
				// TODO: Convert JSON to SensorData and call listener
			}
		} else {
			_Logger.warning("SensorData observation response is null.");
		}
	}
}
