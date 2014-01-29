package com.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.code.CoreConfigDTO;

/**
 * DateRollingUtil
 * @author Shirish
 *
 */
public class DateRollingUtil {
	
	//public final static String PROPERTIES_FILE_NAME="com/util/daterolling.properties";
	public final static String PROPERTIES_FILE_NAME_2="daterolling.properties";
	public final static String IGNORE_IP_PROPERTIES_FILE="com/util/ignoreIP.properties";
	
	 /**
     * getFormattedDate
     * @param rawDate
     * @param dateFormat
     * @return DateTime
     */
	public DateTime getFormattedDate(String rawDate, String dateFormat) {
		try {
			DateTimeFormatter formatter = DateTimeFormat.forPattern(dateFormat)
					.withZone(DateTimeZone.UTC);
			formatter.parseDateTime(rawDate);
			return formatter.parseDateTime(rawDate);
			}catch (Exception ex) {
			System.out.println("ERROR: Could not read date:" + rawDate);
			throw new RuntimeException("Invalid Date");
			}
		}
	
	/**
	 * checkFilePresent
	 * @param fullFilePath
	 */
	 public void checkFilePresent(String fullFilePath){
		File file=new File(fullFilePath);
		if(!file.exists()){
			System.out.println("Note:Please verify if coreconfig file is present at: \""+fullFilePath+"\"");
			throw new RuntimeException("Invalid File Path");
		}
	}

	/**
	 * fetchDetails
	 * @param coreConfigFilePath
	 * @return CoreConfigDTO
	 */
	public CoreConfigDTO fetchDetails(String coreConfigFilePath) {
		CoreConfigDTO configDTO=new CoreConfigDTO();
		checkFilePresent(coreConfigFilePath);
		try {
		  Properties prop = new Properties();
         FileInputStream fis = new FileInputStream(coreConfigFilePath);
	    prop.loadFromXML(fis);
	    configDTO.setIp(prop.getProperty("amqp_server"));
	    configDTO.setPort(prop.getProperty("amqp_port"));
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return configDTO;
	}
	
	/**
	 * 
	 * @param coreConfigFilePath
	 */
	public void validateIp(String coreConfigFilePath){
		Properties properties=getPropertiesFromResource(IGNORE_IP_PROPERTIES_FILE);
		if(properties.containsValue(fetchDetails(coreConfigFilePath).getIp())){
			System.out.println("Please change ip address in coreConfig.xml("+coreConfigFilePath+")");
			System.out.println("Server IP provided in coreConfig.xml("+coreConfigFilePath+") is not allowed to be triggered for Date Rolling.");
			System.exit(1);
		}
		
	}
	
	/**
	 * 
	 * @param fileName
	 * @return
	 */
	public Properties getPropertiesFromResource(String path){
		try {
			InputStream fis = DateRollingUtil.class.getClassLoader().getResourceAsStream(path);
			Properties properties = new Properties();
			properties.load(fis);
			fis.close();
			return properties;
		} catch (IOException e) {
			System.out.println("Something went wrong with properties file:"+e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		throw new RuntimeException("Invalid Properties File:");
	}
	
	
	/**
	 * 
	 * @param fileName
	 * @return
	 */
	public Properties getPropertiesFromResource(){
		try {
			File file = new File(PROPERTIES_FILE_NAME_2);
			if(file!=null && !file.exists()){
				System.out.println("File doesnt exist"+PROPERTIES_FILE_NAME_2+" at:"+file.getAbsolutePath());
				System.exit(1);
			}
			FileInputStream fis=new FileInputStream(PROPERTIES_FILE_NAME_2);
			Properties properties=new Properties();
			properties.load(fis);
			fis.close();
			return properties;
		} catch (IOException e) {
			System.out.println("Something went wrong with properties file:"+e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		throw new RuntimeException("Invalid Properties File:"+PROPERTIES_FILE_NAME_2);
	}
	
	/**
	 * 
	 * @param key
	 * @param value
	 * @return
	 * @throws IOException
	 */
	public boolean storeProperties(String key, String value) throws IOException {
		try {
			File file = new File(PROPERTIES_FILE_NAME_2);
			if(file!=null && !file.exists()){
				System.out.println("File doesnt exist"+PROPERTIES_FILE_NAME_2+" at:"+file.getAbsolutePath());
				System.exit(1);
			}
			//InputStream fis = DateRollingUtil.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME);
			FileInputStream fis=new FileInputStream(PROPERTIES_FILE_NAME_2);
			Properties p = new Properties();
			p.load(fis);
			p.setProperty(key, value);
			FileOutputStream fos = new FileOutputStream(file.getAbsolutePath());
			p.store(fos, "Config Settings");
			fos.close();
			return true;
		} catch (Exception e) {
			System.out.println("error adding to file.");
			System.out.println(e + " \n *Application will close");
			System.exit(1);
			return false;
		}
	}
}
