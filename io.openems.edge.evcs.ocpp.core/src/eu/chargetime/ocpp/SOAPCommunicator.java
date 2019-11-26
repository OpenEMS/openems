package eu.chargetime.ocpp;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/*
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
   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
   SOFTWARE.    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,

*/
import eu.chargetime.ocpp.model.CallErrorMessage;
import eu.chargetime.ocpp.model.CallMessage;
import eu.chargetime.ocpp.model.CallResultMessage;
import eu.chargetime.ocpp.model.Message;
import eu.chargetime.ocpp.model.SOAPHostInfo;

public class SOAPCommunicator extends Communicator {
	private static final Logger logger = LoggerFactory.getLogger(SOAPCommunicator.class);

	private static final String HEADER_ACTION = "Action";
	private static final String HEADER_MESSAGEID = "MessageID";
	private static final String HEADER_RELATESTO = "RelatesTo";
	private static final String HEADER_FROM = "From";
	private static final String HEADER_REPLYTO = "ReplyTo";
	private static final String HEADER_REPLYTO_ADDRESS = "Address";
	private static final String HEADER_TO = "To";
	private static final String HEADER_CHARGEBOXIDENTITY = "chargeBoxIdentity";

	private final SOAPHostInfo hostInfo;
	private String toUrl;

	public SOAPCommunicator(SOAPHostInfo hostInfo, Radio radio) {
		super(radio);
		this.hostInfo = hostInfo;
	}

	@Override
	public <T> T unpackPayload(Object payload, Class<T> type) {
		T output = null;
		try {
			Document input = (Document) payload;
			setNamespace(input, "urn://Ocpp/Cs/2015/10/");
			Unmarshaller unmarshaller = JAXBContext.newInstance(type).createUnmarshaller();
			JAXBElement<T> jaxbElement = unmarshaller.unmarshal(input, type);
			output = jaxbElement.getValue();
		} catch (JAXBException e) {
			logger.warn("unpackPayload() failed", e);
		}
		return output;
	}

	@Override
	public Object packPayload(Object payload) {
		Document document = null;
		try {
			Marshaller marshaller = JAXBContext.newInstance(payload.getClass()).createMarshaller();
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			document = factory.newDocumentBuilder().newDocument();
			marshaller.marshal(payload, document);
			setNamespace(document, hostInfo.getNamespace());
		} catch (JAXBException | ParserConfigurationException e) {
			logger.warn("packPayload() failed", e);
		}
		return document;
	}

	private void setNamespace(Document document, String namespace) {
		Element orgElement = document.getDocumentElement();
		Element newElement = document.createElementNS(namespace, orgElement.getNodeName());

		NodeList childNodes = orgElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			appendChildNS(document, newElement, childNodes.item(i), namespace);
		}

		document.replaceChild(newElement, orgElement);
	}

	private void appendChildNS(Document doc, Node destination, Node child, String namespace) {
		Node newChild;
		if (child.getNodeType() == Node.ELEMENT_NODE) {
			newChild = doc.createElementNS(namespace, child.getNodeName());

			NodeList childNodes = child.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); i++) {
				appendChildNS(doc, newChild, childNodes.item(i), namespace);
			}
		} else {
			newChild = child;
		}

		destination.appendChild(newChild);
	}

	@Override
	protected Object makeCallResult(String uniqueId, String action, Object payload) {
		return createMessage(uniqueId, String.format("%sResponse", action), (Document) payload, true);
	}

	@Override
	protected Object makeCall(String uniqueId, String action, Object payload) {
		return createMessage(uniqueId, action, (Document) payload, false);
	}

	private QName blameSomeone(String errorCode) {
		QName result = SOAPConstants.SOAP_RECEIVER_FAULT;
		if ("SecurityError".equals(errorCode) || "IdentityMismatch".equals(errorCode)
				|| "ProtocolError".equals(errorCode)) {
			return SOAPConstants.SOAP_SENDER_FAULT;
		}

		return result;
	}

	@Override
	protected Object makeCallError(String uniqueId, String action, String errorCode, String errorDescription) {
		SOAPMessage message = null;
		try {
			MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
			message = messageFactory.createMessage();
			message.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");
			createMessageHeader(uniqueId, String.format("%sResponse", action), true, message);

			SOAPFault soapFault = message.getSOAPBody().addFault();
			soapFault.setFaultCode(blameSomeone(errorCode));
			soapFault.setFaultString(errorDescription);

			soapFault.appendFaultSubcode(new QName(hostInfo.getNamespace(), errorCode));

		} catch (SOAPException e) {
			logger.warn("makeCallError() failed", e);
		}
		return message;
	}

	private Object createMessage(String uniqueId, String action, Document payload, boolean isResponse) {
		SOAPMessage message = null;

		try {
			MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);

			message = messageFactory.createMessage();
			message.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");

			createMessageHeader(uniqueId, action, isResponse, message);

			if (isResponse) {
				setNamespace(payload,
						hostInfo.isClient() ? SOAPHostInfo.NAMESPACE_CHARGEBOX : SOAPHostInfo.NAMESPACE_CENTRALSYSTEM);
			}

			message.getSOAPBody().addDocument(payload);
		} catch (Exception e) {
			logger.warn("createMessage() failed", e);
		}

		return message;
	}

	private void createMessageHeader(String uniqueId, String action, boolean isResponse, SOAPMessage message)
			throws SOAPException {
		SOAPFactory soapFactory = SOAPFactory.newInstance();
		SOAPHeader soapHeader = message.getSOAPHeader();

		String prefix = "wsa";
		String namespace = "http://schemas.xmlsoap.org/ws/2004/08/addressing";

		// Set chargeBoxIdentity
		SOAPHeaderElement chargeBoxIdentityHeader = soapHeader
				.addHeaderElement(soapFactory.createName(HEADER_CHARGEBOXIDENTITY, "cs", hostInfo.getNamespace()));
		chargeBoxIdentityHeader.setMustUnderstand(true);
		chargeBoxIdentityHeader.setValue(hostInfo.getChargeBoxIdentity());

		// Set Action
		SOAPHeaderElement actionHeader = soapHeader
				.addHeaderElement(soapFactory.createName(HEADER_ACTION, prefix, namespace));
		actionHeader.setMustUnderstand(true);
		actionHeader.setValue(String.format("/%s", action));

		// Set MessageID
		SOAPHeaderElement messageIdHeader = soapHeader
				.addHeaderElement(soapFactory.createName(HEADER_MESSAGEID, prefix, namespace));
		messageIdHeader.setMustUnderstand(true);
		messageIdHeader.setValue(uniqueId);

		// Set RelatesTo
		if (isResponse) {
			SOAPHeaderElement relatesToHeader = soapHeader
					.addHeaderElement(soapFactory.createName(HEADER_RELATESTO, prefix, namespace));
			relatesToHeader.setValue(uniqueId);
		}

		// Set From
		SOAPHeaderElement fromHeader = soapHeader
				.addHeaderElement(soapFactory.createName(HEADER_FROM, prefix, namespace));
		fromHeader.setValue(hostInfo.getFromUrl());

		// Set ReplyTo
		SOAPHeaderElement replyToHeader = soapHeader
				.addHeaderElement(soapFactory.createName(HEADER_REPLYTO, prefix, namespace));
		replyToHeader.setMustUnderstand(true);
		SOAPElement addressElement = replyToHeader
				.addChildElement(soapFactory.createName(HEADER_REPLYTO_ADDRESS, prefix, namespace));
		addressElement.setValue("http://www.w3.org/2005/08/addressing/anonymous");

		// Set To
		SOAPHeaderElement toHeader = soapHeader.addHeaderElement(soapFactory.createName(HEADER_TO, prefix, namespace));
		toHeader.setMustUnderstand(true);
		toHeader.setValue(toUrl);
	}

	@Override
	protected Message parse(Object message) {
		Message output = null;
		SOAPParser soapParser = new SOAPParser((SOAPMessage) message);

		if (soapParser.isAddressedToMe()) {
			output = soapParser.parseMessage();
		}

		return output;
	}

	private class SOAPParser {

		private SOAPHeader soapHeader;
		private SOAPMessage soapMessage;

		public SOAPParser(SOAPMessage message) {
			try {
				soapMessage = message;
				soapHeader = message.getSOAPPart().getEnvelope().getHeader();
			} catch (SOAPException e) {
				logger.error("SOAPParser() failed", e);
			}
		}

		public Message parseMessage() {
			Message output = null;
			try {

				String relatesTo = getElementValue(HEADER_RELATESTO);
				String action = getElementValue(HEADER_ACTION);

				if (relatesTo != null && !relatesTo.isEmpty() && action != null && action.endsWith("Response")) {
					if (soapMessage.getSOAPBody().hasFault()) {
						output = parseError();
					} else {
						output = parseResult();
					}
				} else {
					output = parseCall();
				}

				if (action != null && !action.isEmpty()) {
					output.setAction(action.substring(1));
				}

				if (!soapMessage.getSOAPBody().hasFault()) {
					output.setPayload(soapMessage.getSOAPBody().extractContentAsDocument());
				}

			} catch (SOAPException e) {
				logger.warn("parseMessage() failed", e);
			}
			return output;
		}

		public boolean isAddressedToMe() {
			String to = getElementValue(HEADER_TO);
			String cbIdentity = getElementValue(HEADER_CHARGEBOXIDENTITY);
			return hostInfo.getFromUrl().equals(to) && hostInfo.getChargeBoxIdentity().equals(cbIdentity);
		}

		private CallErrorMessage parseError() {
			CallErrorMessage message = new CallErrorMessage();

			String id = getElementValue(HEADER_RELATESTO);
			message.setId(id);

			try {
				SOAPFault fault = soapMessage.getSOAPBody().getFault();

				if (fault.getFaultSubcodes().hasNext()) {
					message.setErrorCode(((QName) fault.getFaultSubcodes().next()).getLocalPart());
				}
				if (fault.getFaultReasonTexts().hasNext()) {
					message.setErrorDescription(fault.getFaultReasonTexts().next().toString());
				}

			} catch (SOAPException e) {
				logger.error("Parse error", e);
			}

			return message;
		}

		private CallResultMessage parseResult() {
			CallResultMessage message = new CallResultMessage();

			String id = getElementValue(HEADER_RELATESTO);
			message.setId(id);

			return message;
		}

		private CallMessage parseCall() {
			CallMessage message = new CallMessage();

			String id = getElementValue(HEADER_MESSAGEID);
			message.setId(id);

			return message;
		}

		private String getElementValue(String tagName) {
			String value = null;
			NodeList elements = soapHeader.getElementsByTagNameNS("*", tagName);

			if (elements.getLength() > 0) {
				value = elements.item(0).getChildNodes().item(0).getNodeValue();
			}

			return value;
		}
	}

	public String getToUrl() {
		return toUrl;
	}

	public void setToUrl(String toUrl) {
		this.toUrl = toUrl;
	}
}
