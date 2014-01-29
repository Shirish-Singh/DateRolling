package com.thirdparty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.korwe.thecore.api.CoreMessageHandler;
import com.korwe.thecore.api.CoreSender;
import com.korwe.thecore.api.CoreSubscriber;
import com.korwe.thecore.api.MessageQueue;
import com.korwe.thecore.messages.CoreMessage;
import com.korwe.thecore.messages.CoreMessage.MessageType;
import com.korwe.thecore.messages.CoreResponse;
import com.korwe.thecore.messages.DataResponse;
import com.korwe.thecore.messages.InitiateSessionRequest;
import com.korwe.thecore.messages.KillSessionRequest;
import com.korwe.thecore.messages.ServiceRequest;
import com.korwe.thecore.messages.ServiceResponse;
import com.thoughtworks.xstream.XStream;

/**
 * 
 */
public class Transport implements CoreMessageHandler {

	protected static long timeout;
	protected Map<String, CoreMessage> messageCache = new HashMap<String, CoreMessage>();
	protected Map<String, DataResponse> dataCache = Collections
			.synchronizedMap(new HashMap<String, DataResponse>());
	protected Map<String, CountDownLatch> latches = new HashMap<String, CountDownLatch>();
	protected XStream xStream = new XStream();

	protected Properties commonProperties;

	public Transport() {
		this.timeout = 600000;
	}

	public long getTimeout() {
		return timeout;
	}

	public Properties getProperties() {
		if (commonProperties != null) {
			return commonProperties;
		} else {
			return null;
		}
	}

	public boolean initSession(String sessionId) {
		CoreSender sender = new CoreSender(MessageQueue.ClientToCore);
		CoreSubscriber subscriber = new CoreSubscriber(
				MessageQueue.CoreToClient, sessionId);
		subscriber.connect(this);

		InitiateSessionRequest req = new InitiateSessionRequest(sessionId);
		CountDownLatch latch = new CountDownLatch(1);
		latches.put(req.getGuid(), latch);
		sender.sendMessage(req);
		sender.close();
		try {
			boolean responseReceived = latch.await(timeout,
					TimeUnit.MILLISECONDS);
			latches.remove(req.getGuid());
			if (responseReceived) {
				CoreMessage msg = messageCache.remove(req.getGuid());
				if (null != msg
						&& MessageType.InitiateSessionResponse == msg
								.getMessageType()) {
					return ((CoreResponse) msg).isSuccessful();
				}
			}
			return false;
		} catch (InterruptedException ie) {
			System.out.println("Await interrupted");
			return false;
		} finally {
		//	System.out.println("Closing subscriber");
			subscriber.close();
		}
	}

	public boolean makeRequest(ServiceRequest req) {
		// addAuthToken(req);
		CoreSender sender = new CoreSender(MessageQueue.ClientToCore);
		CoreSubscriber subscriber = new CoreSubscriber(
				MessageQueue.CoreToClient, req.getSessionId());
		subscriber.connect(this);
		CountDownLatch latch = new CountDownLatch(1);
		latches.put(req.getGuid(), latch);
		sender.sendMessage(req);
		sender.close();
		try {
			//System.out.println(" Waiting for response:"+ req.getFunction() + " guid:" + req.getGuid());
			boolean responseReceived = latch.await(timeout,	TimeUnit.MILLISECONDS);
			latches.remove(req.getGuid());
			if (responseReceived) {
				CoreMessage msg = messageCache.remove(req.getGuid());
				if (null != msg
						&& MessageType.ServiceResponse == msg.getMessageType()) {
				//	System.out.println("Received response:"+ req.getFunction() + " = " + msg.getMessageType() + " guid:" + msg.getGuid());
					ServiceResponse response = (ServiceResponse) msg;
					if (response.isSuccessful()) {
						return true;
					} else {
					//	System.out.println("Returning error reponse:"+ req.getFunction()+ " = "	+ msg.getMessageType()+ " guid:" + msg.getGuid());
						handleErrorResponse(req, response);
					}
				}
			}
		//	System.out.println("TIMEOUT (" + timeout + ") waiting for response:" + req.getFunction() + " guid:" + req.getGuid());
			throw new RuntimeException("SYSTEM_RESPONSE_TIMEOUT");
		} catch (InterruptedException ie) {
			System.out.println(" Interrupted waiting for response :"+ req.getFunction() + "  guid:"+ req.getGuid());
			throw new RuntimeException("SYSTEM_UNEXPECTED");
		} finally {
			//System.out.println("Closing subscriber");
			subscriber.close();
		}
	}


	private void handleErrorResponse(ServiceRequest request,
			ServiceResponse response) {
		String[] errorVars = Iterables.toArray(
				Splitter.on('|').split(response.getErrorMessage()),
				String.class);
		if (response.getErrorCode() != null) {
			String[] splitErrorCode = response.getErrorCode().split("\\.");
			if (splitErrorCode.length < 2) {
				throw new RuntimeException("ErrorCode.SYSTEM_UNEXPECTED");
			}

			if (splitErrorCode[0].equals("service")) {
				throw new RuntimeException("response.getErrorCode(), errorVars");
			} else if (splitErrorCode[0].equals("validation")) {
				throw new RuntimeException("response.getErrorCode(), errorVars");
			} else if (splitErrorCode[0].equals("business")) {
				throw new RuntimeException("response.getErrorCode(), errorVars");
			} else if (splitErrorCode[0].equals("system")) {
				throw new RuntimeException(
						"response.getErrorCode(), request.getChoreography()");
			} else {
				throw new RuntimeException("ErrorCode.SYSTEM_UNEXPECTED");
			}
		} else {
			throw new RuntimeException("ErrorCode.SYSTEM_UNEXPECTED");
		}
	}

	public DataResponse makeDataRequest(ServiceRequest req) {
		CoreSender sender = new CoreSender(MessageQueue.ClientToCore);
		CoreSubscriber subscriber = new CoreSubscriber(
				MessageQueue.CoreToClient, req.getSessionId());
		subscriber.connect(this);
		CoreSubscriber dataSubscriber = new CoreSubscriber(MessageQueue.Data,
				req.getSessionId());
		dataSubscriber.connect(this);
		CountDownLatch latch = new CountDownLatch(2);
		latches.put(req.getGuid(), latch);
		sender.sendMessage(req);
		sender.close();
		try {
			//System.out.println("Waiting for reponse:"+ req.getFunction() + " guid:" + req.getGuid());
			boolean responseReceived = latch.await(timeout,
					TimeUnit.MILLISECONDS);
			latches.remove(req.getGuid());
			if (responseReceived) {
				CoreMessage msg = messageCache.remove(req.getGuid());
				if (null != msg
						&& MessageType.ServiceResponse == msg.getMessageType()) {
				//	System.out.println("Received response:"+ req.getFunction() + " = " + msg.getMessageType() + " guid:" + msg.getGuid());
					ServiceResponse response = (ServiceResponse) msg;
					if (response.isSuccessful() && response.hasData()) {
						return dataCache.remove(req.getGuid());
					} else {
						if (!response.hasData()
								&& response.getErrorCode() == null) {
							//System.out.println("Missing Data in reponse:"+ req.getFunction()+ " = "+ msg.getMessageType()+ " guid: " + msg.getGuid());
							throw new RuntimeException(
									"ErrorCode.SYSTEM_UNEXPECTED");
						} else {
							//System.out.println("Returning error reponse:"+ req.getFunction()+ " = "	+ msg.getMessageType()+ " guid:" + msg.getGuid());
							handleErrorResponse(req, response);
						}

					}
				}
			}
		//	System.out.println("TIMEOUT (" + timeout + ") waiting for response:" + req.getFunction() + " guid:" + req.getGuid());
			throw new RuntimeException("ErrorCode.SYSTEM_RESPONSE_TIMEOUT");
		} catch (InterruptedException ie) {
			System.out.println(" Interrupted waiting for response :"+ req.getFunction() + "  guid:"+ req.getGuid());
			throw new RuntimeException("ErrorCode.SYSTEM_UNEXPECTED");
		} finally {
			//System.out.println("Closing subscriber");
			subscriber.close();
			//System.out.println("Closing data subscriber");
			dataSubscriber.close();
		}
	}

	@Override
	public void handleMessage(CoreMessage message) {
		//System.out.println("Message received " + message);
		if (MessageType.DataResponse == message.getMessageType()) {
		//	System.out.println("Saving data response");
			dataCache.put(message.getGuid(), (DataResponse) message);
		} else {
			messageCache.put(message.getGuid(), message);
		//	System.out.println("Saved message " + messageCache);
		}
		CountDownLatch latch = latches.get(message.getGuid());
		if (latch != null)
			latch.countDown();
	}

	public boolean closeSession(String sessionId) {
		CoreSender sender = new CoreSender(MessageQueue.ClientToCore);
		CoreSubscriber subscriber = new CoreSubscriber(
				MessageQueue.CoreToClient, sessionId);
		subscriber.connect(this);
		KillSessionRequest req = new KillSessionRequest(sessionId);
		CountDownLatch latch = new CountDownLatch(1);
		latches.put(req.getGuid(), latch);
		sender.sendMessage(req);
		sender.close();
		try {
			boolean responseReceived = latch.await(timeout,
					TimeUnit.MILLISECONDS);
			latches.remove(req.getGuid());
			if (responseReceived) {
				CoreMessage msg = messageCache.remove(req.getGuid());
				if (null != msg
						&& MessageType.KillSessionResponse == msg
								.getMessageType()) {
					return ((CoreResponse) msg).isSuccessful();
				}
			}
			return false;
		} catch (InterruptedException ie) {
			System.out.println("Await interrupted");
			return false;
		} finally {
		//	System.out.println("Closing subscriber");
			subscriber.close();
		}
	}
}
