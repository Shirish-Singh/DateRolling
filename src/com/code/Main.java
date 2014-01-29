package com.code;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.qpid.transport.TransportException;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.korwe.thecore.api.CoreConfig;
import com.thirdparty.Simulate;
import com.util.DateRollingUtil;

/**
 * Main class that is an execution point for the simulator.
 * 
 * @author Shirish
 * 
 */
public class Main {

	static {
		console("----------------------------------------------------------------------------------------",
				true);
		console("                    SIMULATOR Version 2.0 - Making QA's Life Simpler ;)				 ",
				true);
		console("----------------------------------------------------------------------------------------",
				true);
	}

	private final static String DATE_FORMAT = "yyyy-MM-dd";
	private final static String METHOD_NAME = "simulateByDateRollingClientApp";
	private final static String METHOD_NAME_2 = "simulateTriggerByDate";
	private static DateTime startDate = null;
	private static DateTime endDate = null;
	private final static String FILE_NAME = "\\coreconfig.xml";
	private final static String PROPERTIES_FILE_NAME = "\\daterolling.properties";
	private static String workingDir = System.getProperty("user.dir");
	private final static long DEFAULT_DELAY = 8;
	private final static long BUFFER_MINUTES = 4;

	final String CURRENT_SYSTEM_DATE = "CURRENT_SYSTEM_DATE";
	final String RESULT = "RESULT";
	final String ERROR_RESPONSE = "ERROR_RESPONSE";
	final String UNIX_COMMAND = "UNIX_COMMAND";
	final String DESCRIPTION = "DESCRIPTION";

	final String ARREARS_TIME = "arrears_time_";
	final String SUBMISSION_JOB_INTERVAL = "submission_job_interval";
	final String RECEIPTING_JOB_INTERVAL = "receipting_job_interval";
	final String BUFFER_MIN = "buffer_min";

	private static BufferedReader bufferRead = new BufferedReader(
			new InputStreamReader(System.in));
	private static DateRollingUtil dateRollingUtil = new DateRollingUtil();

	//Currently there are three or more calls to server , this can be done in one call in future if required right now not needed.
	public static void main(String args[]) throws IOException,
			InterruptedException {
		boolean flag = true;

		Main main = new Main();
		dateRollingUtil.checkFilePresent(workingDir + FILE_NAME);
		dateRollingUtil.validateIp(workingDir + FILE_NAME);
		main.initialize();
		main.printCoreConfigDetails();
		while (flag) {
			main.checkConnection();
			main.callingOperation(main);
			flag = main.runAgain();
		}
		shutDown();
	}

	private void callingOperation(Main main) {
		
		try{
		CoreConfigDTO coreConfigDTO = dateRollingUtil.fetchDetails(workingDir
				+ FILE_NAME);
		// Get Current System Date
		main.fetchSystemDateAndSetStartDate();
		// Ask for End Date and which job to run and Trigger Date Rolling
		main.triggerDateRolling();
		// Set end date on system
		main.setNewSystemDate(endDate);
		// Ask and Set time for running job
		main.askForJob();
		// Done
		console("\nMake sure to stop and start flexifin service on server:"
				+ coreConfigDTO.getIp());
		console("\nPlease Wait for "
				+ dateRollingUtil.getPropertiesFromResource().getProperty(
						BUFFER_MIN) + " mins after starting the service");
		}catch(Throwable throwable){
			console("\n Please start the system again , system will now exit...");
			System.exit(1);
		}
	}

	private void askForJob() throws IOException {
		try {
			Properties properties = dateRollingUtil.getPropertiesFromResource();
			console("\n--------------------------------------------------------");
			console("\n Job menu");
			console("\n0. No Job");
			console("\n1. Arrears Job (Runs at "
					+ properties.getProperty(ARREARS_TIME) + " )");
			console("\n2. Submission Job (Runs at "
					+ properties.getProperty(SUBMISSION_JOB_INTERVAL)
					+ "th min of an hour)");
			console("\n3. Receipting Job (Runs at "
					+ properties.getProperty(RECEIPTING_JOB_INTERVAL)
					+ "th min of an hour)");
			console("\nPlease Choose ...");
			int choice = Integer.parseInt(bufferRead.readLine());
			console("\n--------------------------------------------------------");
			switch (choice) {
			case 1:
				setArrearsTime(properties.getProperty(ARREARS_TIME),
						properties.getProperty(BUFFER_MIN));
				break;
			case 2:
				setTime(properties.getProperty(SUBMISSION_JOB_INTERVAL),
						properties.getProperty(BUFFER_MIN));
				break;
			case 3:
				setTime(properties.getProperty(RECEIPTING_JOB_INTERVAL),
						properties.getProperty(BUFFER_MIN));
				break;
			case 0: console("\n Exiting System now...");
					System.exit(0);
			default:
				console("\nInvalid Choice...");
				break;
			}
		} catch (NumberFormatException exception) {
			console("\nInvalid Choice (Enter Valid Number)");
			throw exception;
		} catch (IOException e) {
			console("Exception occured" + e.getMessage());
			e.printStackTrace();
			throw e;
		}

	}

	/**
	 * 
	 * @param property
	 * @param bufferMin
	 */
	private void setArrearsTime(String property, String bufferMin) {
		int hh = fetchHour(property);
		int mm = fetchMin(property);
		int ss = fetchSec(property);

		DateTime dateTime = new DateTime(endDate.getYear(),
				endDate.getMonthOfYear(), endDate.getDayOfMonth(), hh, mm, ss);
		dateTime=dateTime.minusMinutes(Integer.parseInt(bufferMin));
		String time = dateTime.getHourOfDay() + ":"
				+ dateTime.getMinuteOfHour() + ":00";
		console("\nSetting system time: " + time);
		setNewSystemTime(time);
	}

	private int fetchSec(String property) {
		return 00;
	}

	private int fetchMin(String property) {
		String hh = property.substring(3, 5);
		return Integer.parseInt(hh);
	}

	private int fetchHour(String property) {
		String hh = property.substring(0, 2);
		return Integer.parseInt(hh);
	}

	/**
	 * 
	 * @param property
	 * @param bufferMin
	 */
	private void setTime(String property, String bufferMin) {
		int min = Integer.parseInt(property) - Integer.parseInt(bufferMin);
		String hour = get2DigitFormat(startDate.getHourOfDay());
		String time = hour + ":" + min + ":00";
		console("\nSetting system time: " + time);
		setNewSystemTime(time);
	}

	private void triggerDateRolling() throws Exception {

		try {
			String rawEndDate = null;
			BufferedReader bufferRead = new BufferedReader(
					new InputStreamReader(System.in));
			Simulate simulate = new Simulate();
			long start = 0l;
			console("\nPlease enter End Date(yyyy-MM-dd): ");
			rawEndDate = bufferRead.readLine();
			if (showConfigMenu(rawEndDate, bufferRead))
				return;
			endDate = dateRollingUtil.getFormattedDate(rawEndDate, DATE_FORMAT);
			if (endDate.isBefore(startDate)) {
				throw new RuntimeException(
						"End date is before start date, Please try again");
			}
			console("\n------------------------------------------------------\n");
			console("Starting Date Rolling with:\n");
			console(String.format("Start Date = %s", startDate.toString()));
			console(String.format("\nEnd Date = %s", endDate.toString()));
			console("\n------------------------------------------------------\n");
			Map<String, Object> params = createParams(new String[] {
					"startDate", "endDate" },
					new Object[] { startDate, endDate });
			start = System.currentTimeMillis();
			 makeRequestToService(params);
			console(String.format("%s total time: %d", Thread.currentThread()
					.getName(), System.currentTimeMillis() - start));
			console("\n------------------------------------------------------\n");
		} catch (Exception exception) {
			console("------------->Exception Occured<-----------------\n");
			if (exception instanceof TransportException) {
				console(exception.getCause().getMessage() + ":"
						+ exception.getMessage());

			} else {
				console(exception.getMessage());
			}
			
			throw exception;
		}

	}

	private void fetchSystemDateAndSetStartDate() {
		String DateFormat = " yyyy-MM-dd hh:mm:ss ";
		DateTimeFormatter dateTimeFormatter = DateTimeFormat
				.forPattern(DateFormat);
		String CURRENT_SYSTEM_DATE = "CURRENT_SYSTEM_DATE";
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(CURRENT_SYSTEM_DATE, null);
		params = (Map<String, Object>) makeDataRequest(params);
		LocalDateTime dateTime = (LocalDateTime) params
				.get(CURRENT_SYSTEM_DATE);
		console("\nCurrent System Date:"
				+ dateTime.toString(dateTimeFormatter));
		startDate = dateTime.toDateTime();
	}

	private void setNewSystemDate(DateTime endDate) {
		if (endDate == null) {
			console("\nEnd Date is invalid");
			System.exit(0);
		}
		StringBuilder command = new StringBuilder("date -s ");
		String month = get2DigitFormat(endDate.getMonthOfYear());
		String day = get2DigitFormat(endDate.getDayOfMonth());
		String year = get2DigitFormat(endDate.getYear());
		command.append(month + "/" + day + "/" + year);
		console("\nSetting END DATE (dd-MM-yyyy) :" + day + "-" + month + "-" + year
				+ "\n");
		if (startDate == null) {
			console("No start date set... exiting...");
			System.exit(1);
		}
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(UNIX_COMMAND, command.toString());
		params = (Map<String, Object>) makeDataRequest(params);
		console(params.containsKey(RESULT) ? params.get(RESULT).toString() : "");
		console(params.containsKey(DESCRIPTION) ? (params.get(DESCRIPTION) == null ? ""
				: params.get(DESCRIPTION).toString())
				: "");
		console(params.containsKey(ERROR_RESPONSE) ? params.get(ERROR_RESPONSE)
				.toString() : "");
	}

	/**
	 * 
	 * @param time
	 */
	private void setNewSystemTime(String time) {
		StringBuilder command = new StringBuilder("date -s ").append(time);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(UNIX_COMMAND, command.toString());
		params = (Map<String, Object>) makeDataRequest(params);
		console("\n");
		console(params.containsKey(RESULT) ? params.get(RESULT).toString()
				: "No RESULT Response");
		console(params.containsKey(DESCRIPTION) ? (params.get(DESCRIPTION) == null ? ""
				: params.get(DESCRIPTION).toString())
				: "No DESCRIPTION Response");
		console(params.containsKey(ERROR_RESPONSE) ? params.get(ERROR_RESPONSE)
				.toString() : "");
	}

	private void checkConnection() {
		//

	}

	/**
	 * initialize core config
	 * 
	 * @throws FileNotFoundException
	 */
	private void initialize() {
		try {
			CoreConfig.initialize(new FileInputStream(workingDir + FILE_NAME));
			console("Intializiang Simulator...Done");
			console("\n*It is Recommended that you take a database dump before proceeding...");
		} catch (FileNotFoundException e) {
			console(e.getMessage() + ":" + workingDir + FILE_NAME);
		}
	}

	/**
	 * makeRequestToService
	 * 
	 * @throws IOException
	 */
	private Object makeRequestToService(Map<String, Object> params) {
		try {
			Simulate simulate = new Simulate();
			simulate.connect();
			Object object = simulate.makeServiceRequest(METHOD_NAME_2, params);
			simulate.disconnect();
			return object;
		} catch (Exception exception) {
			console("------------->Exception Occured<-----------------\n");
			if (exception instanceof TransportException) {
				console(exception.getCause().getMessage() + ":"
						+ exception.getMessage());

			} else {
				console(exception.getMessage());
			}
		}
		return null;
	}

	/**
	 * makeRequestToService
	 * 
	 * @throws IOException
	 */
	private Object makeDataRequest(Map<String, Object> params) {
		try {
			Simulate simulate = new Simulate();
			simulate.connect();
			Object object = simulate.makeRequestToFetchSystemDate(METHOD_NAME,
					params);
			simulate.disconnect();
			return object;
		} catch (Exception exception) {
			console("------------->Exception Occured<-----------------\n");
			if (exception instanceof TransportException) {
				console(exception.getCause().getMessage() + ":"
						+ exception.getMessage());

			} else {
				console(exception.getMessage());
			}
		}
		return null;
	}

	// /**
	// * makeRequestToDataService
	// *
	// * @throws IOException
	// */
	// private Object makeRequestToDataService(Map<String,Object> params) {
	// try {
	// Simulate simulate = new Simulate();
	// simulate.connect();
	// Object object=simulate.makeDataServiceRequest(METHOD_NAME, params);
	// simulate.disconnect();
	// return object;
	// } catch (Exception exception) {
	// console("------------->Exception Occured<-----------------\n");
	// if (exception instanceof TransportException) {
	// console(exception.getCause().getMessage() + ":"
	// + exception.getMessage());
	//
	// } else {
	// console(exception.getMessage());
	// }
	// }
	// return null;
	// }

	private boolean showConfigMenu(String data, BufferedReader bufferedReader)
			throws IOException {
		if (data.equals("!@#$%^&*()")) {
			DateTime dateTime = new DateTime();
			// if (dateTime.getHourOfDay() == 3) {
			configMenu();
			return true;
			// }
		}
		return false;
	}

	private void configMenu() throws IOException {

		console("-------------------------------------------------------");
		console("\nConfig Menu\n");
		console("-------------------------------------------------------");
		boolean tryAgain = false;
		do {
			try {
				console("\n0. Exit");
				console("\n1. List Properties");
				console("\n2. Add Property key value");
				console("\n3. Update Property key\n");
				int choice = Integer.parseInt(bufferRead.readLine());
				switch (choice) {
				case 1:
					listProperties();
					console("\n");
					break;
				case 2:
					addProperty();
					break;
				case 3:
					updateProperty();
					break;
				case 0:
					System.exit(0);
					break;
				default:
					console("\nInvalid Choice...Try Again\n");
					break;
				}
			} catch (NumberFormatException exception) {
				console("\nInvalid Choice (Enter Valid Number)\n");
			}
		} while (tryAgain == false);
	}

	private void updateProperty() throws IOException {
		console("\nPlease input key to be updated:\n");
		String property = bufferRead.readLine();
		Properties properties = dateRollingUtil.getPropertiesFromResource();
		if (properties.containsKey(property)) {
			console("\nPlease input value:\n");
			String value = bufferRead.readLine();
			properties.setProperty(property, value);
			dateRollingUtil.storeProperties(property, value);
		} else {
			console("Property Key: " + property + " not found in properties file\n");
		}

	}

	private void addProperty() throws IOException {

		console("\nPlease input key to be added:\n");
		String key = bufferRead.readLine();
		Properties properties = dateRollingUtil.getPropertiesFromResource();
		if (!properties.containsKey(key)) {
			console("\nPlease input value:\n");
			String value = bufferRead.readLine();
			dateRollingUtil.storeProperties(key, value);
		} else {
			console("Property Key: " + key
					+ " allready present in properties file\n");
		}
	}

	private void listProperties() {
		Properties properties = dateRollingUtil.getPropertiesFromResource();
		for (Map.Entry<Object, Object> entry : properties.entrySet()) {
			console("\n" + entry.getKey() + "=" + entry.getValue() + "");
		}
	}

	private void printCoreConfigDetails() {
		CoreConfigDTO coreConfigDTO = dateRollingUtil.fetchDetails(workingDir
				+ FILE_NAME);
		console("\nSimulating Date Rolling on ip: \"" + coreConfigDTO.getIp()
				+ "\" and on port: \"" + coreConfigDTO.getPort() + "\"\n");
	}

	/**
	 * runAgain
	 * 
	 * @return
	 */
	private boolean runAgain() {
		console("\n********************************");
		console("\nDo you want to do it again: y/n");
		console("\n********************************\n");
		try {
			if (!bufferRead.readLine().equalsIgnoreCase("y")) {
				return false;
			}
		} catch (IOException e) {
			console(e.getMessage());
		}
		return true;
	}

	/**
	 * shutDown
	 */
	private static void shutDown() {
		console("Simulator Shutting down...", DEFAULT_DELAY, false);
		console("Thank You!!.", 20, false);
		System.exit(0);
	}

	/************************************************************************************** Class Utilities ***********************************************************/

	/**
	 * createParams
	 * 
	 * @param paramNames
	 * @param paramValues
	 * @return
	 */
	private static Map<String, Object> createParams(String[] paramNames,
			Object[] paramValues) {
		Map<String, Object> params = new HashMap<String, Object>(
				paramNames.length);
		for (int i = 0, paramCount = paramNames.length; i < paramCount; i++) {
			params.put(paramNames[i], paramValues[i]);
		}
		return params;
	}

	/**
	 * console
	 * 
	 * @param message
	 */
	private static void console(String message) {
		console(message, DEFAULT_DELAY, false);
	}

	/**
	 * console
	 * 
	 * @param message
	 * @param newLine
	 */
	private static void console(String message, boolean newLine) {
		console(message, DEFAULT_DELAY, newLine);
	}

	/**
	 * console
	 * 
	 * @param message
	 * @param delay
	 * @param newLine
	 */
	private static void console(String message, long delay, boolean newLine) {
		char[] chars = message.toCharArray();
		int i = 0;
		while (message.length() != i) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				System.out.println("Interrupt Exception:" + e.getMessage());
			}
			System.out.print(chars[i]);
			i++;
		}
		if (newLine) {
			System.out.println("\n");
		}
	}

	/**
	 * get2DigitFormat
	 * 
	 * @param digit
	 * @return
	 */
	private String get2DigitFormat(int digit) {
		if (digit >= 0 && digit < 10) {
			return "0" + digit;
		}
		return Integer.toString(digit);
	}

}
