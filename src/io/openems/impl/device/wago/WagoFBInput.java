package io.openems.impl.device.wago;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.io.InputNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.impl.protocol.modbus.ModbusCoilReadChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.internal.CoilElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.range.ModbusCoilRange;
import io.openems.impl.protocol.modbus.internal.range.ModbusRange;

public class WagoFBInput extends ModbusDeviceNature implements InputNature {

	@ConfigInfo(title = "Ip-Address to download the wago configuration", type = Inet4Address.class)
	public ConfigChannel<Inet4Address> ip = new ConfigChannel<Inet4Address>("ip", this);

	private List<ModbusCoilReadChannel> channel = new ArrayList<>();

	public WagoFBInput(String thingId) throws ConfigException {
		super(thingId);
	}

	@Override
	public ModbusCoilReadChannel[] getInput() {
		return channel.toArray(new ModbusCoilReadChannel[channel.size()]);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		// writeElements = new ArrayList<String>();
		// mainElements = new ArrayList<String>();
		// bitElementMapping = new HashMap<String, String>();
		HashMap<String, List<String>> channels = new HashMap<>();
		String username = "admin";
		String password = "wago";
		int ftpPort = 21;
		URL url;
		Document doc;
		try {
			url = new URL("ftp://" + username + ":" + password + "@" + ip.value().getHostAddress() + ":" + ftpPort
					+ "/etc/EA-config.xml;type=i");
			URLConnection urlc = url.openConnection();
			InputStream is = urlc.getInputStream();
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(is);
			doc.getDocumentElement().normalize();
		} catch (IOException | SAXException | ParserConfigurationException e) {
			throw new ConfigException(e.getMessage());
		} catch (InvalidValueException e) {
			throw new ConfigException("Failed to read ip-address!" + e.getMessage());
		}

		Node wagoNode = doc.getElementsByTagName("WAGO").item(0);
		if (wagoNode != null) {
			HashMap<String, Integer> moduleCounter = new HashMap<String, Integer>();
			Node moduleNode = wagoNode.getFirstChild();
			while (moduleNode != null) {
				if (moduleNode.getNodeType() == Node.ELEMENT_NODE) {
					NamedNodeMap moduleAttrs = moduleNode.getAttributes();
					String article = moduleAttrs.getNamedItem("ARTIKELNR").getNodeValue();
					String moduletype = moduleAttrs.getNamedItem("MODULETYPE").getNodeValue();
					if (!moduleCounter.containsKey(moduletype)) {
						moduleCounter.put(moduletype, 0);
					}
					moduleCounter.replace(moduletype, moduleCounter.get(moduletype) + 1);
					int index = 1;
					Node channelNode = moduleNode.getFirstChild();
					while (channelNode != null) {
						if (channelNode.getNodeType() == Node.ELEMENT_NODE) {
							NamedNodeMap channelAttrs = channelNode.getAttributes();
							String channelType = channelAttrs.getNamedItem("CHANNELTYPE").getNodeValue();
							if (!channels.containsKey(channelType)) {
								channels.put(channelType, new ArrayList<String>());
							}
							String channelName = "";
							switch (channelType) {
							case "DO":
								channelName = "DigitalOutput_" + moduleCounter.get(channelType) + "_" + index;
								break;
							case "DI":
								channelName = "DigitalInput_" + moduleCounter.get(channelType) + "_" + index;
								break;
							default:
								log.debug("ChannelType: " + channelName + " nicht erkannt");
								break;
							}
							channels.get(channelType).add(channelName);
							index++;
						}
						channelNode = channelNode.getNextSibling();
					}
				}
				moduleNode = moduleNode.getNextSibling();
			}
		}
		List<ModbusCoilRange> ranges = new ArrayList<>();
		for (String key : channels.keySet()) {
			switch (key) {
			case "DI": {
				List<CoilElement> elements = new ArrayList<>();
				int count = 0;
				for (String channel : channels.get(key)) {
					if (count % 64 == 0) {
						ranges.add(
								new ModbusCoilRange(512 + count, elements.toArray(new CoilElement[elements.size()])));
						elements.clear();
					}
					ModbusCoilReadChannel ch = new ModbusCoilReadChannel(Integer.toString(count), this);
					this.channel.add(ch);
					elements.add(new CoilElement(512 + count, ch));
					count++;
				}
			}
				break;
			}
		}
		ModbusProtocol protocol = new ModbusProtocol(ranges.toArray(new ModbusRange[ranges.size()]));
		return protocol;
	}

}
