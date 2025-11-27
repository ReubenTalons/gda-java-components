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
package programmingtheiot.gda.connection.handlers;

import java.util.logging.Logger;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.OptionSet;

import programmingtheiot.common.IDataMessageListener;

/**
 * Generic CoAP resource handler implementation.
 * Processes responses from CoAP requests (GET, PUT, POST, DELETE).
 */
public class GenericCoapResponseHandler implements CoapHandler
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(GenericCoapResponseHandler.class.getName());
	
	// params
	
	private IDataMessageListener dataMsgListener = null;
	
	// constructors
	
	/**
	 * Default constructor.
	 */
	public GenericCoapResponseHandler()
	{
		this((IDataMessageListener) null);
	}
	
	/**
	 * Constructor with data message listener.
	 * 
	 * @param listener The data message listener for callbacks
	 */
	public GenericCoapResponseHandler(IDataMessageListener listener)
	{
		super();
		
		this.dataMsgListener = listener;
		
		_Logger.fine("Response handler created. IDataMessageListener is " + (listener != null ? "set" : "not set"));
	}
	
	// public methods
	
	/**
	 * Callback invoked when a CoAP response is received successfully.
	 * 
	 * @param response The CoAP response object
	 */
	@Override
	public void onLoad(CoapResponse response)
	{
		if (response != null) {
			OptionSet options = response.getOptions();
			
			_Logger.info("Processing CoAP response. Options: " + options);
			_Logger.info("Processing CoAP response. MID: " + response.advanced().getMID());
			_Logger.info("Processing CoAP response. Token: " + response.advanced().getTokenString());
			_Logger.info("Processing CoAP response. Code: " + response.getCode());
			
			// TODO: parse payload and notify listener
			_Logger.info(" --> Payload: " + response.getResponseText());
			
			if (this.dataMsgListener != null) {
				// TODO: send listener the response
			}
		} else {
			_Logger.warning("No CoAP response to process. Response is null.");
		}
	}
	
	/**
	 * Callback invoked when an error occurs processing the CoAP response.
	 */
	@Override
	public void onError()
	{
		_Logger.warning("Error processing CoAP response. Ignoring.");
	}
}