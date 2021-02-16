package io.openems.edge.wago;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.io.api.AnalogInput;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;

@Designate(ocd = Config.class, factory = true)
@Component(name = "IO.WAGO", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Wago extends AbstractOpenemsModbusComponent implements DigitalOutput, DigitalInput, AnalogInput, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(Wago.class);

	private static final int UNIT_ID = 1;

	@Reference
	protected ConfigurationAdmin cm;

	private InetAddress ipAddress = null;
	private ModbusProtocol protocol = null;
	private double multiplicationFactor = 1;
	private CopyOnWriteArrayList<FieldbusDigitalModule> digitalModules = new CopyOnWriteArrayList<FieldbusDigitalModule>();
	private CopyOnWriteArrayList<FieldbusAnalogModule> analogModules = new CopyOnWriteArrayList<FieldbusAnalogModule>();
	

	public enum ThisChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ThisChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public Wago() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				DigitalInput.ChannelId.values(), //
				AnalogInput.ChannelId.values(), //
				ThisChannelId.values() //
		);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbusTcp modbus) {
		super.setModbus(modbus);
		this.ipAddress = modbus.getIpAddress();
	}

	private final ScheduledExecutorService configExecutor = Executors.newSingleThreadScheduledExecutor();
	private ScheduledFuture<?> configFuture = null;

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.multiplicationFactor = config.multiplicationFactor();
		if (super.activate(context, config.id(), config.alias(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}

		/*
		 * Async Create Channels dynamically from ea-config.xml file
		 */
		this.configFuture = configExecutor.schedule(() -> {
			try {
				Document doc = Wago.downloadConfigXml(this.ipAddress, config.username(), config.password());
				this.digitalModules.addAll(this.parseXmlForDigital(doc));
				this.analogModules.addAll(this.parseXmlForAnalog(doc));
				this.createProtocolFromModules(this.digitalModules, this.analogModules);
			} catch (SAXException | IOException | ParserConfigurationException | OpenemsException e) {
				e.printStackTrace();
			}
		}, 2, TimeUnit.SECONDS);
	}

	/**
	 * Downloads the config xml file from WAGO fieldbus coupler. Tries old
	 * ('ea-config.xml') and new ('io_config.xml') filenames.
	 * 
	 * @param ip       the IP address
	 * @param username the login username
	 * @param password the login password
	 * @return the XML document
	 * @throws SAXException                 on error
	 * @throws IOException                  on error
	 * @throws ParserConfigurationException on error
	 */
	private static Document downloadConfigXml(InetAddress ip, String username, String password)
			throws SAXException, IOException, ParserConfigurationException {
		try {
			return downloadConfigXml(ip, "ea-config.xml", username, password);
		} catch (FileNotFoundException e) {
			return downloadConfigXml(ip, "io_config.xml", username, password);
		}
	}

	private static Document downloadConfigXml(InetAddress ip, String filename, String username, String password)
			throws SAXException, IOException, ParserConfigurationException {
		URL url = new URL(String.format("http://%s/etc/%s", ip.getHostAddress(), filename));
		String authStr = String.format("%s:%s", username, password);
		byte[] bytesEncoded = Base64.getEncoder().encode(authStr.getBytes());
		String authEncoded = new String(bytesEncoded);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestProperty("Authorization", String.format("Basic %s", authEncoded));
		connection.setRequestProperty("Content-Type", "text/xml");
		connection.setRequestMethod("GET");
		connection.connect();
		InputStream is = connection.getInputStream();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(is);
		doc.getDocumentElement().normalize();
		return doc;
	}

	/**
	 * Parses the config xml file
	 * 
	 * @param doc the XML document
	 * @return a list of FieldbusModules
	 */
	private List<FieldbusDigitalModule> parseXmlForDigital(Document doc) {
		FieldbusModuleFactory factory = new FieldbusModuleFactory();
		List<FieldbusDigitalModule> result = new ArrayList<>();
		Element wagoElement = doc.getDocumentElement();
		int inputOffset = 0;
		int outputOffset = 512;

		// parse all "Module" XML elements
		NodeList moduleNodes = wagoElement.getElementsByTagName("Module");
		for (int i = 0; i < moduleNodes.getLength(); i++) {
			Node moduleNode = moduleNodes.item(i);
			// get "Module" node attributes
			NamedNodeMap moduleAttrs = moduleNode.getAttributes();
			String moduleArtikelnr = moduleAttrs.getNamedItem("ARTIKELNR").getNodeValue();
			String moduleType = moduleAttrs.getNamedItem("MODULETYPE").getNodeValue();
			if (moduleNode.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element moduleElement = (Element) moduleNode;
			// parse all "Kanal" XML elements inside the "Module" element
			NodeList kanalNodes = moduleElement.getElementsByTagName("Kanal");
			FieldbusModuleKanal[] kanals = new FieldbusModuleKanal[kanalNodes.getLength()];
			for (int j = 0; j < kanalNodes.getLength(); j++) {
				Node kanalNode = kanalNodes.item(j);
				if (kanalNode.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				NamedNodeMap kanalAttrs = kanalNode.getAttributes();
				String channelName = kanalAttrs.getNamedItem("CHANNELNAME").getNodeValue();
				String channelType = kanalAttrs.getNamedItem("CHANNELTYPE").getNodeValue();
				kanals[j] = new FieldbusModuleKanal(channelName, channelType);
			}
			// Create FieldbusModule instance using factory method
			if(moduleArtikelnr.equals("750-4xx") || moduleArtikelnr.equals("750-5xx")) {
				FieldbusDigitalModule module = factory.ofDigital(this, moduleArtikelnr, moduleType, kanals, inputOffset, outputOffset);
				inputOffset += module.getInputCoils();
				outputOffset += module.getOutputCoils();
				result.add(module);
			}
		}
		return result;
	}
	
	private List<FieldbusAnalogModule> parseXmlForAnalog(Document doc) {
		FieldbusModuleFactory factory = new FieldbusModuleFactory();
		List<FieldbusAnalogModule> result = new ArrayList<>();
		Element wagoElement = doc.getDocumentElement();
		int inputOffset = 0;
		int outputOffset = 512;

		// parse all "Module" XML elements
		NodeList moduleNodes = wagoElement.getElementsByTagName("Module");
		for (int i = 0; i < moduleNodes.getLength(); i++) {
			Node moduleNode = moduleNodes.item(i);
			// get "Module" node attributes
			NamedNodeMap moduleAttrs = moduleNode.getAttributes();
			String moduleArtikelnr = moduleAttrs.getNamedItem("ARTIKELNR").getNodeValue();
			String moduleType = moduleAttrs.getNamedItem("MODULETYPE").getNodeValue();
			if (moduleNode.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element moduleElement = (Element) moduleNode;
			// parse all "Kanal" XML elements inside the "Module" element
			NodeList kanalNodes = moduleElement.getElementsByTagName("Kanal");
			FieldbusModuleKanal[] kanals = new FieldbusModuleKanal[kanalNodes.getLength()];
			for (int j = 0; j < kanalNodes.getLength(); j++) {
				Node kanalNode = kanalNodes.item(j);
				if (kanalNode.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				NamedNodeMap kanalAttrs = kanalNode.getAttributes();
				String channelName = kanalAttrs.getNamedItem("CHANNELNAME").getNodeValue();
				String channelType = kanalAttrs.getNamedItem("CHANNELTYPE").getNodeValue();
				kanals[j] = new FieldbusModuleKanal(channelName, channelType);
			}
			// Create FieldbusModule instance using factory method
			if(moduleArtikelnr.equals("750-496/000-000") || moduleArtikelnr.equals("750-497/000-000")) {
				FieldbusAnalogModule module = factory.ofAnalog(this, moduleArtikelnr, moduleType, kanals, inputOffset, outputOffset);
				inputOffset += module.getInputCoils();
				outputOffset += module.getOutputCoils();
				result.add(module);
			}
		}
		return result;
	}

	/**parent
	 * Takes a list of FieldbusModules and adds Modbus tasks to the protocol.
	 * 
	 * @param modules lit of {@link FieldbusDigitalModule}s
	 * @throws OpenemsException
	 */
	private void createProtocolFromModules(List<FieldbusDigitalModule> digitalModules, List<FieldbusAnalogModule> analogModules) throws OpenemsException {
		List<AbstractModbusElement<?>> readElements0Digital = new ArrayList<>();
		List<AbstractModbusElement<?>> readElements0Analog = new ArrayList<>();
		List<AbstractModbusElement<?>> readElements512Digital = new ArrayList<>();
		List<AbstractModbusElement<?>> readElements512Analog = new ArrayList<>();
		for (FieldbusDigitalModule module : digitalModules) {
			for (AbstractModbusElement<?> element : module.getInputElements()) {
				if (element.getStartAddress() > 511) {
					readElements512Digital.add(element);
				} else {
					readElements0Digital.add(element);
				}
			}
			for (AbstractModbusElement<?> element : module.getOutputElements()) {
				FC5WriteCoilTask writeCoilTask = new FC5WriteCoilTask(element.getStartAddress(), element);
				this.protocol.addTask(writeCoilTask);
			}
		}
		
		for (FieldbusAnalogModule module : analogModules) {
			for (AbstractModbusElement<?> element : module.getInputElements()) {
				if (element.getStartAddress() > 511) {
					readElements512Analog.add(element);
				} else {
					readElements0Analog.add(element);
				}
			}
		}
		if (!readElements0Digital.isEmpty()) {
			this.protocol.addTask(//
					new FC1ReadCoilsTask(0, Priority.LOW,
							readElements0Digital.toArray(new AbstractModbusElement<?>[readElements0Digital.size()])));
		}
		if (!readElements512Digital.isEmpty()) {
			this.protocol.addTask(//
					new FC1ReadCoilsTask(512, Priority.LOW,
							readElements512Digital.toArray(new AbstractModbusElement<?>[readElements512Digital.size()])));
		}
		if (!readElements0Analog.isEmpty()) {
			this.protocol.addTask(//
					new FC3ReadRegistersTask(0, Priority.LOW,
							readElements0Analog.toArray(new AbstractModbusElement<?>[readElements0Analog.size()])));
		}
		if (!readElements0Analog.isEmpty()) {
			this.protocol.addTask(//
					new FC3ReadRegistersTask(512, Priority.LOW,
							readElements0Analog.toArray(new AbstractModbusElement<?>[readElements0Analog.size()])));
		}
	}

	protected AbstractModbusElement<?> createDigitalModbusElement(io.openems.edge.common.channel.ChannelId channelId,
			int address) {
		return m(channelId, new CoilElement(address));
	}
	
	protected AbstractModbusElement<?> createAnalogModbusElement(io.openems.edge.common.channel.ChannelId channelId,
			int address) {
		return m(channelId, new UnsignedWordElement(address));
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		this.protocol = new ModbusProtocol(this);
		return protocol;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();

		// Shutdown executor
		if (this.configFuture != null) {
			this.configFuture.cancel(true);
		}
		try {
			this.configExecutor.shutdown();
			this.configExecutor.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			this.logWarn(this.log, "tasks interrupted");
		} finally {
			if (!this.configExecutor.isTerminated()) {
				this.logWarn(this.log, "cancel non-finished tasks"); 
			}
			this.configExecutor.shutdownNow();
		}
	}

	@Override
	public String debugLog() {
		if (this.digitalModules.isEmpty() && this.analogModules.isEmpty()) {
			return "";
		}
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < this.digitalModules.size(); i++) {
			b.append("Digital M" + i + ":");
			BooleanReadChannel[] channels = this.digitalModules.get(i).getChannels();
			for (int j = 0; j < channels.length; j++) {
				Optional<Boolean> valueOpt = channels[j].value().asOptional();
				if (valueOpt.isPresent()) {
					b.append(valueOpt.get() ? "x" : "-");
				} else {
					b.append("?");
				}
			}
			if (i < this.digitalModules.size() - 1) {
				b.append("|");
			}
		}
		for (int i = 0; i < this.analogModules.size(); i++) {
			b.append("Analog M" + i + ":");
			IntegerReadChannel[] channels = this.analogModules.get(i).getChannels();
			for (int j = 0; j < channels.length; j++) {
				Optional<Integer> valueOpt = channels[j].value().asOptional();
				if (valueOpt.isPresent()) {
					b.append(valueOpt.get()*multiplicationFactor + ":");
				} else {
					b.append("?");
				}
			}
			if (i < this.analogModules.size() - 1) {
				b.append("|");
			}
		}
		return b.toString();
	}

	@Override
	public BooleanReadChannel[] digitalInputChannels() {
		List<BooleanReadChannel> channels = new ArrayList<>();
		for (FieldbusDigitalModule module : this.digitalModules) {
			for (BooleanReadChannel channel : module.getChannels()) {
				channels.add(channel);
			}
		}
		BooleanReadChannel[] result = new BooleanReadChannel[channels.size()];
		for (int i = 0; i < channels.size(); i++) {
			result[i] = channels.get(i);
		}
		return result;
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		List<BooleanWriteChannel> channels = new ArrayList<>();
		for (FieldbusDigitalModule module : this.digitalModules) {
			for (BooleanReadChannel channel : module.getChannels()) {
				if (channel instanceof BooleanWriteChannel) {
					channels.add((BooleanWriteChannel) channel);
				}
			}
		}
		BooleanWriteChannel[] result = new BooleanWriteChannel[channels.size()];
		for (int i = 0; i < channels.size(); i++) {
			result[i] = channels.get(i);
		}
		return result;
	}

	protected BooleanReadChannel addDigitalChannel(FieldbusDigitalChannelId channelId) {
		return (BooleanReadChannel) super.addChannel(channelId);
	}
	
	protected IntegerReadChannel addAnalogChannel(FieldbusAnalogChannelId channelId) {
		return (IntegerReadChannel) super.addChannel(channelId);
	}

	@Override
	public IntegerReadChannel[] analogInputChannels() {
		List<IntegerReadChannel> channels = new ArrayList<>();
		for (FieldbusAnalogModule module : this.analogModules) {
			for (IntegerReadChannel channel : module.getChannels()) {
				channels.add(channel);
			}
		}
		IntegerReadChannel[] result = new IntegerReadChannel[channels.size()];
		for (int i = 0; i < channels.size(); i++) {
			result[i] = channels.get(i);
		}
		return result;
	}
}
