package programmingtheiot.gda.app;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import programmingtheiot.common.ConfigConst;

/**
 * Main GDA application.
 * 
 */
public class GatewayDeviceApp
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(GatewayDeviceApp.class.getName());
	
	public static final long DEFAULT_TEST_RUNTIME = 60000L;
	
	// private var's
	
	private String configFile = ConfigConst.DEFAULT_CONFIG_FILE_NAME;
	private DeviceDataManager dataMgr = null;
	
	// constructors
	
	/**
	 * Default.
	 * 
	 */
	public GatewayDeviceApp()
	{
		super();
		
		_Logger.info("Initializing GDA...");
	}
	
	/**
	 * Constructor with arguments.
	 * 
	 * @param args Command line arguments
	 */
	public GatewayDeviceApp(String[] args)
	{
		super();
		
		_Logger.info("Initializing GDA...");
		
		Map<String, String> argMap = parseArgs(args);
		if (argMap.containsKey(ConfigConst.CONFIG_FILE_KEY)) {
			System.setProperty(ConfigConst.CONFIG_FILE_KEY, argMap.get(ConfigConst.CONFIG_FILE_KEY));
		}
	}
	
	
	// static
	
	/**
	 * Main application entry point.
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		GatewayDeviceApp gwApp = new GatewayDeviceApp(args);
		
		gwApp.startApp();
		
		try {
			Thread.sleep(65000L);
		} catch (InterruptedException e) {
			// ignore
		}
		
		gwApp.stopApp(0);
	}
	
	/**
	 * Parse any arguments passed in on app startup.
	 * <p>
	 * This method should be written to check if any valid command line args are provided,
	 * including the name of the config file. Once parsed, call {@link #initConfig(String)}
	 * with the name of the config file, or null if the default should be used.
	 * <p>
	 * If any command line args conflict with the config file, the config file
	 * in-memory content should be overridden with the command line argument(s).
	 * 
	 * @param args The non-null and non-empty args array.
	 */
	private static Map<String, String> parseArgs(String[] args)
	{
		// store command line values in a map
		Map<String, String> argMap = new HashMap<String, String>();
		
		if (args != null && args.length > 0)  {
			// create the parser and options - only need one for now ("c" for config file)
			CommandLineParser parser = new DefaultParser();
			Options options = new Options();
			options.addOption("c", true, "The relative or absolute path of the config file.");
			try {
				CommandLine cmdLineArgs = parser.parse(options, args);
				if (cmdLineArgs.hasOption("c")) {
					argMap.put(ConfigConst.CONFIG_FILE_KEY, cmdLineArgs.getOptionValue("c"));
				} else {
					_Logger.info("No custom config file specified. Using default.");
				}
			} catch (ParseException e) {
				_Logger.warning("Failed to parse command line args. Ignoring - using defaults.");
			}
		}
		return argMap;
	}
	
	
	// public methods
	
	/**
	 * Initializes and starts the application.
	 * 
	 */
	public void startApp()
	{
		_Logger.info("Starting GDA...");
		
		try {
			this.dataMgr = new DeviceDataManager();
			
			if (this.dataMgr != null) {
				this.dataMgr.startManager();
			}
			
			_Logger.info("GDA started successfully.");
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to start GDA. Exiting.", e);
			
			stopApp(-1);
		}
	}
	
	/**
	 * Stops the application.
	 * 
	 * @param code The exit code to pass to {@link System.exit()}
	 */
	public void stopApp(int code)
	{
		_Logger.info("Stopping GDA...");
		
		try {
			if (this.dataMgr != null) {
				this.dataMgr.stopManager();
			}
			
			_Logger.log(Level.INFO, "GDA stopped successfully with exit code {0}.", code);
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to cleanly stop GDA. Exiting.", e);
		}
		
		System.exit(code);
	}
	
	
	// private methods
	
}