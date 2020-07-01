package io.openems.edge.controller.api.meteocontrol;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import io.openems.common.exceptions.OpenemsException;

public class XmlUtils {

	private Document xmlDoc = null;
	private Config config;
	private Element datapoints;
	private Element datapoint;
	private Element inverter;
	private Element meter;
	private Element battery;

	public XmlUtils(Config c) throws ParserConfigurationException {
		this.config = c;
		this.createXMLBody();

	}

	private void createXMLBody() throws ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		this.xmlDoc = docBuilder.newDocument();

		Element mii = this.xmlDoc.createElement("mii");
		mii.setAttribute("version", "2.0");
		mii.setAttribute("targetNamespace", "http://api.sspcdn.com/mii");
		mii.setAttribute("xmlns", "http://api.sspcdn.com/mii");
		this.xmlDoc.appendChild(mii);

		Element datalogger = this.xmlDoc.createElement("datalogger");
		mii.appendChild(datalogger);

		Element configuration = this.xmlDoc.createElement("configuration");
		configuration.setAttribute("xmlns", "http://api.sspcdn.com/mii/datalogger/configuration");
		datalogger.appendChild(configuration);

		Element uuid = this.xmlDoc.createElement("uuid");
		configuration.appendChild(uuid);

		Element vendor = this.xmlDoc.createElement("vendor");
		vendor.setTextContent("KACO new energy GmbH");
		uuid.appendChild(vendor);

		Element serial = this.xmlDoc.createElement("serial");
		serial.setTextContent(this.config.serial());
		uuid.appendChild(serial);

		Element devices = this.xmlDoc.createElement("devices");
		configuration.appendChild(devices);

		Element inverter = this.xmlDoc.createElement("device");
		inverter.setAttribute("type", "inverter");
		inverter.setAttribute("id", "inverter-1");
		devices.appendChild(inverter);

		Element inverterUid = this.xmlDoc.createElement("uid");
		inverterUid.setTextContent(this.config.pvInverter());
		inverter.appendChild(inverterUid);

		Element meter = this.xmlDoc.createElement("device");
		meter.setAttribute("type", "meter");
		meter.setAttribute("id", "meter-1");
		devices.appendChild(meter);

		Element meterUid = this.xmlDoc.createElement("uid");
		meterUid.setTextContent(this.config.meter());
		meter.appendChild(meterUid);

		Element battery = this.xmlDoc.createElement("device");
		battery.setAttribute("type", "battery");
		battery.setAttribute("id", "battery-1");
		devices.appendChild(battery);

		Element batteryUid = this.xmlDoc.createElement("uid");
		batteryUid.setTextContent(this.config.essId());
		battery.appendChild(batteryUid);

		Element datapoints = this.xmlDoc.createElement("datapoints");
		datapoints.setAttribute("xmlns", "http://api.sspcdn.com/mii/datalogger/datapoints");
		datalogger.appendChild(datapoints);

		this.datapoints = datapoints;

	}

	private Element appendMeasurement(String name, String value) {

		Element mv = this.xmlDoc.createElement("mv");
		mv.setAttribute("t", name);
		mv.setAttribute("v", value);

		return mv;

	}

	public void addDataPoint(String timestamp) {

		this.datapoint = this.xmlDoc.createElement("datapoint");
		this.datapoint.setAttribute("interval", Integer.toString(this.config.mInterval()));
		this.datapoint.setAttribute("timestamp", timestamp);

		this.datapoints.appendChild(datapoint);
		
		this.battery = this.xmlDoc.createElement("device");
		this.battery.setAttribute("id", "battery-1");
		this.datapoint.appendChild(this.battery);
		
		this.inverter = this.xmlDoc.createElement("device");
		this.inverter.setAttribute("id", "inverter-1");
		this.datapoint.appendChild(this.inverter);
		
		this.meter = this.xmlDoc.createElement("device");
		this.meter.setAttribute("id", "meter-1");
		this.datapoint.appendChild(this.meter);
	}

	public void addBatteryData(String name, String value) {

		this.battery.appendChild(this.appendMeasurement(name, value));

	}

	public void addInverterData(String name, String value) {

		this.inverter.appendChild(this.appendMeasurement(name, value));

	}

	public void addMeterData(String name, String value) {

		this.meter.appendChild(this.appendMeasurement(name, value));

	}

	public String formatData() throws OpenemsException {

		// Transform Document to XML String
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = tf.newTransformer();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new OpenemsException(e.getMessageAndLocation());
		}
		StringWriter writer = new StringWriter();

		try {
			transformer.transform(new DOMSource(this.xmlDoc), new StreamResult(writer));
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			throw new OpenemsException(e.getMessageAndLocation());
		}

		return writer.getBuffer().toString();
	}

}
