package eu.chargetime.ocpp; /*
							   ChargeTime.eu - Java-OCA-OCPP
							
							   MIT License
							
							   Copyright (C) 2016-2018 Thomas Volden <tv@chargetime.eu>
							
							   Permission is hereby granted, free of charge, to any person obtaining a copy
							   of this software and associated documentation files (the "Software"), to deal
							   in the Software without restriction, including without limitation the rights
							   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
							   copies of the Software, and to permit persons to whom the Software is
							   furnished to do so, subject to the following conditions:
							
							   The above copyright notice and this permission notice shall be included in all
							   copies or substantial portions of the Software.
							
							   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
							   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
							   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
							   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
							   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
							   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
							   SOFTWARE.
							*/

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

public abstract class SOAPSyncHelper {
	private static final Logger logger = LoggerFactory.getLogger(SOAPSyncHelper.class);

	private HashMap<String, CompletableFuture<SOAPMessage>> promises;

	public SOAPSyncHelper() {
		promises = new HashMap<>();
	}

	public static String getHeaderValue(SOAPMessage message, String tagName) {
		String value = null;
		try {
			SOAPHeader header = message.getSOAPPart().getEnvelope().getHeader();
			NodeList elements = header.getElementsByTagNameNS("*", tagName);
			if (elements.getLength() > 0) {
				value = elements.item(0).getChildNodes().item(0).getTextContent();
			}
		} catch (SOAPException e) {
			logger.warn("getHeaderValue() failed", e);
		}
		return value;
	}

	abstract void forwardMessage(SOAPMessage message);

	public CompletableFuture<SOAPMessage> relay(SOAPMessage message) {
		CompletableFuture<SOAPMessage> promise = null;
		String uniqueID = getHeaderValue(message, "MessageID");
		if (uniqueID != null) {
			promise = new CompletableFuture<>();
			promises.put(uniqueID, promise);
		}

		forwardMessage(message);
		return promise;
	}

	abstract void sendRequest(SOAPMessage message) throws NotConnectedException;

	public void send(Object message) throws NotConnectedException {
		SOAPMessage soapMessage = (SOAPMessage) message;

		String relatesTo = getHeaderValue(soapMessage, "RelatesTo");
		if (relatesTo != null && promises.containsKey(relatesTo)) {
			promises.get(relatesTo).complete(soapMessage);
		} else {
			sendRequest(soapMessage);
		}
	}
}
