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

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;

/**
 * Observable resource handler for ActuatorData commands.
 * Allows CDA to observe actuator command updates from GDA.
 */
public class GetActuatorCommandResourceHandler extends CoapResource
	implements IActuatorDataListener
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(GetActuatorCommandResourceHandler.class.getName());
	
	// params
	
	private ActuatorData actuatorData = null;
	
	// constructors
	
	/**
	 * Constructor.
	 * 
	 * @param resourceName The name of the resource.
	 */
	public GetActuatorCommandResourceHandler(String resourceName)
	{
		super(resourceName);
		
		// Initialize with default ActuatorData
		this.actuatorData = new ActuatorData();
		
		// Set the resource to be observable
		super.setObservable(true);
		
		_Logger.info("GetActuatorCommandResourceHandler created as observable resource: " + resourceName);
	}
	
	// public methods
	
	/**
	 * Callback method for actuator data updates from DeviceDataManager.
	 * Notifies all observing clients when data changes.
	 * 
	 * @param data The updated actuator data
	 * @return true if update successful, false otherwise
	 */
	@Override
	public boolean onActuatorDataUpdate(ActuatorData data)
	{
		if (data != null && this.actuatorData != null) {
			this.actuatorData.updateData(data);
			
			// Notify all connected clients
			super.changed();
			
			_Logger.fine("Actuator data updated for URI: " + super.getURI() + 
				": Data value = " + this.actuatorData.getValue());
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Handles GET requests for actuator command data.
	 * Returns current actuator data as JSON.
	 */
	@Override
	public void handleGET(CoapExchange context)
	{
		_Logger.info("GET request received for ActuatorCommand resource: " + super.getName());
		
		// Validate context
		if (context == null) {
			_Logger.warning("CoapExchange context is null. Ignoring GET request.");
			return;
		}
		
		// Accept the request
		context.accept();
		
		// Convert the locally stored ActuatorData to JSON
		String jsonData = DataUtil.getInstance().actuatorDataToJson(this.actuatorData);
		
		_Logger.fine("Returning ActuatorData JSON: " + jsonData);
		
		// Send response with JSON data
		context.respond(ResponseCode.CONTENT, jsonData);
	}
	
	/**
	 * Handles PUT requests (basic implementation).
	 */
	@Override
	public void handlePUT(CoapExchange context)
	{
		_Logger.info("PUT request received for ActuatorCommand resource.");
		context.accept();
		context.respond(ResponseCode.CHANGED, "ActuatorCommand resource updated");
	}
	
	/**
	 * Handles POST requests (basic implementation).
	 */
	@Override
	public void handlePOST(CoapExchange context)
	{
		_Logger.info("POST request received for ActuatorCommand resource.");
		context.accept();
		context.respond(ResponseCode.CREATED, "ActuatorCommand resource created");
	}
	
	/**
	 * Handles DELETE requests (basic implementation).
	 */
	@Override
	public void handleDELETE(CoapExchange context)
	{
		_Logger.info("DELETE request received for ActuatorCommand resource.");
		context.accept();
		context.respond(ResponseCode.DELETED, "ActuatorCommand resource deleted");
	}
}
