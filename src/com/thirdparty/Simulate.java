package com.thirdparty;

import java.util.Map;
import java.util.UUID;

import com.korwe.thecore.messages.ServiceRequest;
import com.thoughtworks.xstream.XStream;

/**
 * Simulate class that triggers the simulation
 * @author Shirish
 *
 */
public class Simulate {

	  private boolean connected;
	 // protected Map<String, CountDownLatch> latches = new HashMap<String, CountDownLatch>();
	  private Transport transport=new Transport(); 
	  private String sessionId;
	  protected XStream xStream = new XStream();
	  
	  public Simulate(){
		  sessionId= UUID.randomUUID().toString();  
	  }
	
	    private void connectIfNecessary() {
	        if (!connected) connect();
	    }
	    
	    public boolean connect() {
	        connected = transport.initSession(sessionId);
	        return connected;
	    }
	    
	    public boolean disconnect(){
	    	return transport.closeSession(sessionId);
	    }
	    
	  public Object makeServiceRequest(String functionName, Map<String, Object> params) {
	        connectIfNecessary();
	        return makeServiceRequest(functionName,sessionId, params);
	    }

	  public Object makeDataServiceRequest(String functionName, Map<String, Object> params) {
	        connectIfNecessary();
	        return makeDataServiceRequest(functionName,sessionId, params);
	    }
	  
	  public Object makeRequestToFetchSystemDate(String functionName, Map<String, Object> params) {
	        connectIfNecessary();
	        try{
	            ServiceRequest req = new ServiceRequest(sessionId, functionName);
	            req.setChoreography("AccountingService");
	            req.setParameter("params", xStream.toXML(params));
	            return (Map)xStream.fromXML(transport.makeDataRequest(req).getData());
		  }catch(Throwable throwable){
		  throw new RuntimeException("Failure: Simulate ByTrigger",throwable);
	    }
	  }
	  
	  public Object makeServiceRequest(String functionName, String sessionId, Map<String, Object> params) {
		  try{
	            ServiceRequest req = new ServiceRequest(sessionId, functionName);
	            req.setChoreography("AccountingService");
	            req.setParameter("startDate", xStream.toXML(params.get("startDate")));
	            req.setParameter("endDate", xStream.toXML(params.get("endDate")));
                return transport.makeRequest(req);
		  }catch(Throwable throwable){
		  throw new RuntimeException("Failure: Simulate ByTrigger",throwable);
	    }
	  }
	  
	  
	  public Object makeDataServiceRequest(String functionName, String sessionId, Map<String, Object> params) {
		  try{
	            ServiceRequest req = new ServiceRequest(sessionId, functionName);
	            req.setChoreography("AccountingService");
	            req.setParameter("startDate", xStream.toXML(params.get("startDate")));
	            req.setParameter("endDate", xStream.toXML(params.get("endDate")));
	            return (Map)xStream.fromXML(transport.makeDataRequest(req).getData());
		  }catch(Throwable throwable){
		  throw new RuntimeException("Failure: Simulate ByTrigger",throwable);
	    }
	  }
}
