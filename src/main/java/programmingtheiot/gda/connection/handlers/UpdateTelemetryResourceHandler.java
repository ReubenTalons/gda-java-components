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

import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.DataUtil;

/**
 * Resource handler for SensorData (Telemetry) updates via CoAP PUT requests.
 */
public class UpdateTelemetryResourceHandler extends CoapResource {
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(UpdateTelemetryResourceHandler.class.getName());
	
	// params
	
	private IDataMessageListener dataMsgListener = null;
	
	// constructors
	
	/**
	 * Constructor.
	 * 
	 * @param resourceName The name of the resource.
	 */
	public UpdateTelemetryResourceHandler(String resourceName) {
		super(resourceName);
	}
	
	// public methods
	
	/**
	 * Sets the data message listener for callbacks.
	 * 
	 * @param listener The data message listener
	 */
	public void setDataMessageListener(IDataMessageListener listener) {
		if (listener != null) {
			this.dataMsgListener = listener;
		}
	}
	
	@Override
	public void handleDELETE(CoapExchange context) {
		_Logger.info("DELETE request received for SensorData resource.");
		context.accept();
		context.respond(ResponseCode.DELETED, "SensorData resource deleted");
	}
	
	@Override
	public void handleGET(CoapExchange context) {
		_Logger.info("GET request received for SensorData resource.");
		context.accept();
		context.respond(ResponseCode.CONTENT, "SensorData resource - GET not fully implemented");
	}
	
	@Override
	public void handlePOST(CoapExchange context) {
		_Logger.info("POST request received for SensorData resource.");
		context.accept();
		context.respond(ResponseCode.CREATED, "SensorData resource created/updated");
	}
	
	@Override
	public void handlePUT(CoapExchange context) {
		ResponseCode code = ResponseCode.NOT_ACCEPTABLE;
		
		context.accept();
		
		if (this.dataMsgListener != null) {
			try {
				String jsonData = new String(context.getRequestPayload());
				
				SensorData sensorData =
					DataUtil.getInstance().jsonToSensorData(jsonData);
				
				this.dataMsgListener.handleSensorMessage(
					ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sensorData);
				
				code = ResponseCode.CHANGED;
				
				_Logger.info("SensorData PUT request processed successfully.");
			} catch (Exception e) {
				_Logger.warning(
					"Failed to handle PUT request. Message: " + e.getMessage());
				
				code = ResponseCode.BAD_REQUEST;
			}
		} else {
			_Logger.info(
				"No callback listener for request. Ignoring PUT.");
			
			code = ResponseCode.CONTINUE;
		}
		
		String msg = "Update telemetry data request handled: " + super.getName();
		
		context.respond(code, msg);
	}
}
