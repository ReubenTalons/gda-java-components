/**
 * 
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 */ 
package programmingtheiot.integration.connection;

import static org.junit.Assert.*;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.gda.connection.RedisPersistenceAdapter;

/**
 * Integration tests for RedisPersistenceAdapter.
 * Tests Redis connectivity and data persistence operations.
 */
public class PersistenceClientAdapterTest
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(PersistenceClientAdapterTest.class.getName());
	
	
	// member var's
	
	private RedisPersistenceAdapter rpa = null;
	
	
	// test setup methods
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		_Logger.info("Testing RedisPersistenceAdapter class...");
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
		_Logger.info("Test complete.");
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception
	{
		this.rpa = new RedisPersistenceAdapter();
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception
	{
		if (this.rpa != null) {
			this.rpa.disconnectClient();
		}
	}
	
	// test methods
	
	/**
	 * Test method for connecting to Redis.
	 */
	@Test
	public void testConnectClient()
	{
		_Logger.info("Testing connectClient()...");
		
		boolean success = this.rpa.connectClient();
		
		assertTrue("Connection should succeed", success);
		
		_Logger.info("Connect test passed.");
	}
	
	/**
	 * Test method for disconnecting from Redis.
	 */
	@Test
	public void testDisconnectClient()
	{
		_Logger.info("Testing disconnectClient()...");
		
		// Connect first
		this.rpa.connectClient();
		
		// Then disconnect
		boolean success = this.rpa.disconnectClient();
		
		assertTrue("Disconnection should succeed", success);
		
		_Logger.info("Disconnect test passed.");
	}
	
	/**
	 * Test method for storing and retrieving ActuatorData.
	 */
	@Test
	public void testStoreAndGetActuatorData()
	{
		_Logger.info("Testing store and get ActuatorData...");
		
		// Connect to Redis
		this.rpa.connectClient();
		
		// Create test data
		ActuatorData ad = new ActuatorData();
		ad.setName("TestActuator");
		ad.setValue(25.5f);
		ad.setCommand(1);
		
		String topic = ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE.getResourceName();
		
		// Store data
		boolean storeSuccess = this.rpa.storeData(topic, 0, ad);
		assertTrue("Store should succeed", storeSuccess);
		
		// Small delay to ensure data is written
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// ignore
		}
		
		// Retrieve data
		ActuatorData[] results = this.rpa.getActuatorData(topic, null, null);
		
		assertNotNull("Retrieved data should not be null", results);
		assertTrue("Should retrieve at least one entry", results.length > 0);
		
		_Logger.info("Store and retrieve ActuatorData test passed. Retrieved " + results.length + " entries.");
	}
	
	/**
	 * Test method for storing and retrieving SensorData.
	 */
	@Test
	public void testStoreAndGetSensorData()
	{
		_Logger.info("Testing store and get SensorData...");
		
		// Connect to Redis
		this.rpa.connectClient();
		
		// Create test data
		SensorData sd = new SensorData();
		sd.setName("TestSensor");
		sd.setValue(22.5f);
		
		String topic = ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE.getResourceName();
		
		// Store data
		boolean storeSuccess = this.rpa.storeData(topic, 0, sd);
		assertTrue("Store should succeed", storeSuccess);
		
		// Small delay to ensure data is written
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// ignore
		}
		
		// Retrieve data
		SensorData[] results = this.rpa.getSensorData(topic, null, null);
		
		assertNotNull("Retrieved data should not be null", results);
		assertTrue("Should retrieve at least one entry", results.length > 0);
		
		_Logger.info("Store and retrieve SensorData test passed. Retrieved " + results.length + " entries.");
	}
	
	/**
	 * Test method for storing SystemPerformanceData.
	 */
	@Test
	public void testStoreSystemPerformanceData()
	{
		_Logger.info("Testing store SystemPerformanceData...");
		
		// Connect to Redis
		this.rpa.connectClient();
		
		// Create test data
		SystemPerformanceData spd = new SystemPerformanceData();
		spd.setName("TestSystemPerf");
		spd.setCpuUtilization(45.5f);
		spd.setMemoryUtilization(60.2f);
		
		String topic = ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE.getResourceName();
		
		// Store data
		boolean storeSuccess = this.rpa.storeData(topic, 0, spd);
		assertTrue("Store should succeed", storeSuccess);
		
		_Logger.info("Store SystemPerformanceData test passed.");
	}
	
	/**
	 * Test method for storing multiple data items.
	 */
	@Test
	public void testStoreMultipleData()
	{
		_Logger.info("Testing store multiple data items...");
		
		// Connect to Redis
		this.rpa.connectClient();
		
		// Create multiple test data items
		SensorData sd1 = new SensorData();
		sd1.setName("Sensor1");
		sd1.setValue(20.0f);
		
		SensorData sd2 = new SensorData();
		sd2.setName("Sensor2");
		sd2.setValue(25.0f);
		
		SensorData sd3 = new SensorData();
		sd3.setName("Sensor3");
		sd3.setValue(30.0f);
		
		String topic = ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE.getResourceName();
		
		// Store multiple data items
		boolean storeSuccess = this.rpa.storeData(topic, 0, sd1, sd2, sd3);
		assertTrue("Store multiple should succeed", storeSuccess);
		
		_Logger.info("Store multiple data test passed.");
	}
	
}