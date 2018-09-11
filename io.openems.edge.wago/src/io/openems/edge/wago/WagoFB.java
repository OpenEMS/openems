//package io.openems.edge.wago;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URL;
//import java.net.URLConnection;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import javax.xml.parsers.ParserConfigurationException;
//
//import org.osgi.service.cm.ConfigurationAdmin;
//import org.osgi.service.component.ComponentContext;
//import org.osgi.service.component.annotations.Activate;
//import org.osgi.service.component.annotations.Component;
//import org.osgi.service.component.annotations.ConfigurationPolicy;
//import org.osgi.service.component.annotations.Deactivate;
//import org.osgi.service.component.annotations.Reference;
//import org.osgi.service.metatype.annotations.Designate;
//import org.slf4j.LoggerFactory;
//import org.w3c.dom.Document;
//import org.w3c.dom.NamedNodeMap;
//import org.w3c.dom.Node;
//import org.xml.sax.SAXException;
//
//import io.openems.edge.common.component.AbstractOpenemsComponent;
//import io.openems.edge.common.component.OpenemsComponent;
//
//@Designate(ocd = Config.class, factory = true)
//@Component( //
//		name = "WAGO", //
//		immediate = true, //
//		configurationPolicy = ConfigurationPolicy.REQUIRE //
////		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
//)
//public class WagoFB extends AbstractOpenemsComponent implements OpenemsComponent {
//
//	@Reference
//	protected ConfigurationAdmin cm;
//
//	private String modbusBridgeId;
//
//	private static HashMap<String, HashMap<String, List<String>>> configCache = new HashMap<>();
//
//	@Activate
//	void activate(ComponentContext context, Config config) {
//		super.activate(context, config.service_pid(), config.id(), config.enabled());
//		this.modbusBridgeId = config.modbus_id();
//		getConfig(config);
//		System.out.println(getConfig(config));
//	}
//
//	public static HashMap<String, List<String>> getConfig(Config config) {
//		HashMap<String, List<String>> channels = new HashMap<>();
//		Document doc = null;
//		URL url;
//		try {
//			url = new URL("ftp://" + config.username() + ":" + config.password() + "@" + config.ip() + ":"
//					+ config.ftpPort() + "/etc/ea-config.xml;type=i");
//			URLConnection urlc = url.openConnection();
//			InputStream is = urlc.getInputStream();
//			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
//			doc = dBuilder.parse(is);
//			doc.getDocumentElement().normalize();
//		} catch (IOException | ParserConfigurationException | SAXException e) {
//			e.printStackTrace();
//		}
//		Node wagoNode = doc.getElementsByTagName("WAGO").item(0);
//		if (wagoNode != null) {
//			HashMap<String, Integer> moduleCounter = new HashMap<String, Integer>();
//			Node moduleNode = wagoNode.getFirstChild();
//			while (moduleNode != null) {
//				if (moduleNode.getNodeType() == Node.ELEMENT_NODE) {
//					NamedNodeMap moduleAttrs = moduleNode.getAttributes();
//					String moduletype = moduleAttrs.getNamedItem("MODULETYPE").getNodeValue();
//					if (!moduleCounter.containsKey(moduletype)) {
//						moduleCounter.put(moduletype, 0);
//					}
//					moduleCounter.replace(moduletype, moduleCounter.get(moduletype) + 1);
//					int index = 1;
//					Node channelNode = moduleNode.getFirstChild();
//					while (channelNode != null) {
//						if (channelNode.getNodeType() == Node.ELEMENT_NODE) {
//							NamedNodeMap channelAttrs = channelNode.getAttributes();
//							String channelType = channelAttrs.getNamedItem("CHANNELTYPE").getNodeValue();
//							if (!channels.containsKey(channelType)) {
//								channels.put(channelType, new ArrayList<String>());
//							}
//							String channelName = "";
//							switch (channelType) {
//							case "DO":
//								channelName = "DigitalOutput_" + moduleCounter.get(channelType) + "_" + index;
//								break;
//							case "DI":
//								channelName = "DigitalInput_" + moduleCounter.get(channelType) + "_" + index;
//								break;
//							default:
//								LoggerFactory.getLogger(WagoFB.class)
//										.debug("ChannelType: " + channelName + " nicht erkannt");
//								break;
//							}
//							channels.get(channelType).add(channelName);
//							index++;
//						}
//						channelNode = channelNode.getNextSibling();
//					}
//				}
//				moduleNode = moduleNode.getNextSibling();
//			}
//		}
//		configCache.put(config.ip(), channels);
//		return channels;
//	}
//
//	@Deactivate
//	protected void deactivate() {
//		super.deactivate();
//	}
//
//	public String getModbusBridgeId() {
//		return modbusBridgeId;
//	}
//}
