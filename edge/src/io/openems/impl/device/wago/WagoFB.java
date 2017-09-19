/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.impl.device.wago;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.modbus.ModbusDevice;

/*
 * Example config:
 *
 * <pre>
 * {
 *   "class": "io.openems.impl.protocol.modbus.ModbusTcp",
 *   "ip": "172.16.86.1",
 *   "devices": [
 *     {
 *       "class": "io.openems.impl.device.wago.WagoFB",
 *       "modbusUnitId": 1,
 *       "output": {
 *         "id": "output0",
 *         "ip": "172.16.86.1"
 *       },
 *       "input": {
 *         "id": "input0",
 *         "ip": "172.16.86.1"
 *       }
 *     }
 *   ]
 * }
 * </pre>
 */

@ThingInfo(title = "WAGO I/O")
public class WagoFB extends ModbusDevice {

	/*
	 * Constructors
	 */
	public WagoFB(Bridge parent) throws OpenemsException {
		super(parent);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Output", description = "Sets the output nature.", type = WagoFBOutput.class)
	public final ConfigChannel<WagoFBOutput> output = new ConfigChannel<>("output", this);

	@ChannelInfo(title = "Input", description = "Sets the input nature.", type = WagoFBInput.class)
	public final ConfigChannel<WagoFBInput> input = new ConfigChannel<>("input", this);

	/*
	 * Fields
	 */
	private static HashMap<Inet4Address, HashMap<String, List<String>>> configCache = new HashMap<>();

	/*
	 * Methods
	 */
	@Override
	protected Set<DeviceNature> getDeviceNatures() {
		Set<DeviceNature> natures = new HashSet<>();
		if (output.valueOptional().isPresent()) {
			natures.add(output.valueOptional().get());
		}
		if (input.valueOptional().isPresent()) {
			natures.add(input.valueOptional().get());
		}
		return natures;
	}

	public static HashMap<String, List<String>> getConfig(Inet4Address ip) throws ConfigException {
		if (configCache.containsKey(ip)) {
			return configCache.get(ip);
		} else {
			HashMap<String, List<String>> channels = new HashMap<>();
			String username = "admin";
			String password = "wago";
			int ftpPort = 21;
			URL url;
			Document doc;
			try {
				url = new URL("ftp://" + username + ":" + password + "@" + ip.getHostAddress() + ":" + ftpPort
						+ "/etc/EA-config.xml;type=i");
				URLConnection urlc = url.openConnection();
				InputStream is = urlc.getInputStream();
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				doc = dBuilder.parse(is);
				doc.getDocumentElement().normalize();
			} catch (IOException | SAXException | ParserConfigurationException e) {
				throw new ConfigException(e.getMessage());
			}

			Node wagoNode = doc.getElementsByTagName("WAGO").item(0);
			if (wagoNode != null) {
				HashMap<String, Integer> moduleCounter = new HashMap<String, Integer>();
				Node moduleNode = wagoNode.getFirstChild();
				while (moduleNode != null) {
					if (moduleNode.getNodeType() == Node.ELEMENT_NODE) {
						NamedNodeMap moduleAttrs = moduleNode.getAttributes();
						// String article = moduleAttrs.getNamedItem("ARTIKELNR").getNodeValue();
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
									LoggerFactory.getLogger(WagoFB.class)
											.debug("ChannelType: " + channelName + " nicht erkannt");
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
			configCache.put(ip, channels);
			return channels;
		}
	}

}
