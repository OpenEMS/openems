package io.openems.edge.controller.api.modbus;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import com.google.common.base.CaseFormat;
import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.modbusslave.ModbusType;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.session.User;
import io.openems.common.worker.AbstractWorker;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.modbusslave.ModbusRecord;
import io.openems.edge.common.modbusslave.ModbusRecordChannel;
import io.openems.edge.common.modbusslave.ModbusRecordString16;
import io.openems.edge.common.modbusslave.ModbusRecordUint16BlockLength;
import io.openems.edge.common.modbusslave.ModbusRecordUint16Hash;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.common.ApiWorker;
import io.openems.edge.controller.api.common.WritePojo;
import io.openems.edge.controller.api.modbus.jsonrpc.GetModbusProtocolExportXlsxRequest;
import io.openems.edge.controller.api.modbus.jsonrpc.GetModbusProtocolExportXlsxResponse;
import io.openems.edge.controller.api.modbus.jsonrpc.GetModbusProtocolRequest;
import io.openems.edge.controller.api.modbus.jsonrpc.GetModbusProtocolResponse;

public abstract class AbstractModbusApi extends AbstractOpenemsComponent
		implements ModbusApi, Controller, OpenemsComponent, JsonApi {

	public static final int UNIT_ID = 1;
	public static final int DEFAULT_PORT_TCP = 502;
	public static final String DEFAULT_PORT_SERIAL = "/dev/ttyUSB0";
	public static final int DEFAULT_MAX_CONCURRENT_CONNECTIONS = 5;

	protected final ApiWorker apiWorker = new ApiWorker(this);

	private final Logger log = LoggerFactory.getLogger(AbstractModbusApi.class);
	private final MyProcessImage processImage;
	private final String implementationName;
	private String port;
	private String connectionType;
	private String[] componentIdsArray;

	/**
	 * Holds the link between Modbus address and ModbusRecord.
	 */
	protected final TreeMap<Integer, ModbusRecord> records = new TreeMap<>();

	/**
	 * Holds the link between Modbus start address of a Component and the
	 * Component-ID.
	 */
	private final TreeMap<Integer, String> components = new TreeMap<>();

	protected void addComponent(OpenemsComponent component) {
		this._components.put(component.id(), component);
	}

	private volatile Map<String, OpenemsComponent> _components = new HashMap<>();

	public AbstractModbusApi(String implementationName,
							 io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
							 io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		this.implementationName = implementationName;
		this.processImage = new MyProcessImage(this);
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled, ConfigurationAdmin cm,
							ComponentManager cpm, Meta metaComponent, String[] componentIds, int apiTimeout, String port)
			throws OpenemsException {
		super.activate(context, id, alias, enabled);

		// configuration settings
		this.port = port;

		switch (implementationName) {
			case "Modbus/TCP-Api Custom":
			case "Modbus/Serial-Api Custom":

				// Verify that the input has the correct format. Abort if not.
				if (processChannelInput(componentIds, cpm)) {
					return;
				}
				break;
			case "Modbus/TCP-Api Read-Write":
			case "Modbus/TCP-Api Read-Only":
			case "Modbus/Serial-Api Read-Write":
			case "Modbus/Serial-Api Read-Only":
				componentIdsArray = componentIds;
				break;
		}

		// update filter for 'components'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "Component", componentIdsArray)) {
			return;
		}

		this.apiWorker.setTimeoutSeconds(apiTimeout);

		if (!this.isEnabled()) {
			// abort if disabled
			return;
		}

		switch (implementationName) {
			case "Modbus/TCP-Api Custom":
				this.connectionType = "TCP";
				break;
			case "Modbus/Serial-Api Custom":
				this.connectionType = "Serial";
				break;
			case "Modbus/TCP-Api Read-Write":
			case "Modbus/TCP-Api Read-Only":
			case "Modbus/Serial-Api Read-Write":
			case "Modbus/Serial-Api Read-Only":
				this.connectionType = connectionType;

				// Add Meta-Component to _components
				this.addComponent(metaComponent);

				// Initialize Modbus Records
				this.initializeModbusRecords(metaComponent, componentIds);
				break;
		}

		printModbusAddressMapping();

		// Start Modbus-Server
		this.startApiWorker.activate(id);
	}

	protected void deactivate() {
		this.startApiWorker.deactivate();
		ModbusSlaveFactory.close();
		super.deactivate();
	}

	private final AbstractWorker startApiWorker = new AbstractWorker() {

		private static final int DEFAULT_WAIT_TIME = 5000; // 5 seconds

		private final Logger log = LoggerFactory.getLogger(AbstractWorker.class);

		private com.ghgande.j2mod.modbus.slave.ModbusSlave slave = null;

		@Override
		protected void forever() {
			//int port = AbstractModbusTcpApi.this.port;
			if (this.slave == null) {
				try {
					// start new server
					//this.slave = ModbusSlaveFactory.createTCPSlave(port, AbstractModbusTcpApi.this.maxConcurrentConnections);
					this.slave = createModbusSlave();
					this.slave.addProcessImage(UNIT_ID, AbstractModbusApi.this.processImage);
					this.slave.open();
					AbstractModbusApi.this.logInfo(this.log, AbstractModbusApi.this.implementationName
							+ " started on port [" + port + "] with UnitId [" + AbstractModbusApi.UNIT_ID + "].");
					AbstractModbusApi.this._setUnableToStart(false);
				} catch (ModbusException e) {
					ModbusSlaveFactory.close();
					AbstractModbusApi.this.logError(this.log,
							"Unable to start " + AbstractModbusApi.this.implementationName + " on port [" + port
									+ "]: " + e.getMessage());
					AbstractModbusApi.this._setUnableToStart(true);
				}

			} else {
				// regular check for errors
				String error = this.slave.getError();
				if (error != null) {
					AbstractModbusApi.this.logError(this.log,
							"Unable to start Modbus/TCP Api on port [" + port + "]: " + error);
					AbstractModbusApi.this._setUnableToStart(true);
					this.slave = null;
					// stop server
					ModbusSlaveFactory.close();
				}
			}
		}

		@Override
		protected int getCycleTime() {
			return DEFAULT_WAIT_TIME;
		}

	};

	/**
	 * Initialize Modbus-Records for all configured Component-IDs.
	 * 
	 * @param metaComponent the {@link Meta} component
	 * @param componentIds  the configured Component-IDs.
	 */
	private void initializeModbusRecords(Meta metaComponent, String[] componentIds) {
		// Add generic header
		this.records.put(0, new ModbusRecordUint16Hash(0, "OpenEMS"));
		int nextAddress = 1;

		// add Meta-Component
		nextAddress = this.addMetaComponentToProcessImage(nextAddress, metaComponent);

		// add remaining components; sorted by configured componentIds
		for (String id : componentIds) {
			// find next component in order
			ModbusSlave component = (ModbusSlave) this._components.get(id);
			if (component == null) {
				this.logWarn(this.log, "Required Component [" + id + "] is not available.");
				continue;
			}

			// add component to process image
			nextAddress = this.addComponentToProcessImage(nextAddress, component);
		}
	}

	/**
	 * Adds the Meta-Component to the Process Image.
	 * 
	 * @param startAddress the start-address
	 * @param component    the {@link Meta} component
	 * @return the next start-address
	 */
	private int addMetaComponentToProcessImage(int startAddress, Meta component) {
		ModbusSlaveTable table = component.getModbusSlaveTable(this.getAccessMode());

		// add the Component-Model Length
		int nextAddress = this.addRecordToProcessImage(startAddress,
				new ModbusRecordUint16BlockLength(-1, component.id(), (short) table.getLength()), component);

		// add Records
		for (ModbusSlaveNatureTable natureTable : table.getNatureTables()) {
			for (ModbusRecord record : natureTable.getModbusRecords()) {
				this.addRecordToProcessImage(nextAddress + record.getOffset(), record, component);
			}
		}
		return startAddress + table.getLength();
	}

	/**
	 * Adds a Component to the Process Image.
	 * 
	 * @param startAddress the start-address
	 * @param component    the OpenEMS Component
	 * @return the next start-address
	 */
	private int addComponentToProcessImage(int startAddress, ModbusSlave component) {
		this.components.put(startAddress, component.alias());
		ModbusSlaveTable table = component.getModbusSlaveTable(this.getAccessMode());

		// add the Component-ID and Component-Model Length
		int nextAddress = this.addRecordToProcessImage(startAddress,
				new ModbusRecordString16(-1, "Component-ID", component.id()), component);
		this.addRecordToProcessImage(nextAddress,
				new ModbusRecordUint16BlockLength(-1, component.id(), (short) table.getLength()), component);
		nextAddress = startAddress + 20;
		int nextNatureAddress = nextAddress;

		// add all Nature-Tables
		for (ModbusSlaveNatureTable natureTable : table.getNatureTables()) {
			// add the Interface Hash-Code and Length
			nextAddress = this.addRecordToProcessImage(nextNatureAddress,
					new ModbusRecordUint16Hash(-1, natureTable.getNature().getSimpleName()), component);
			nextAddress = this.addRecordToProcessImage(nextAddress, new ModbusRecordUint16BlockLength(-1,
					natureTable.getNature().getSimpleName(), (short) natureTable.getLength()), component);

			// add Records
			for (ModbusRecord record : natureTable.getModbusRecords()) {
				this.addRecordToProcessImage(nextNatureAddress + 2 + record.getOffset(), record, component);
			}

			nextNatureAddress = nextNatureAddress += natureTable.getLength();
		}

		// calculate next address after this component
		return startAddress + table.getLength();
	}

	/**
	 * Adds a Record to the process image at the given address.
	 * 
	 * @param address   the address
	 * @param record    the record
	 * @param component the OpenEMS Component
	 * @return the next address after this record
	 */
	private int addRecordToProcessImage(int address, ModbusRecord record, OpenemsComponent component) {
		record.setComponentId(component.id());

		// Handle writes to the Channel; limited to ModbusRecordChannels
		if (record instanceof ModbusRecordChannel) {
			ModbusRecordChannel r = (ModbusRecordChannel) record;
			r.onWriteValue((value) -> {
				Channel<?> readChannel = component.channel(r.getChannelId());
				if (!(readChannel instanceof WriteChannel)) {
					this.logWarn(this.log, "Unable to write to Read-Only-Channel [" + readChannel.address() + "]");
					return;
				}
				WriteChannel<?> channel = (WriteChannel<?>) readChannel;
				this.apiWorker.addValue(channel, new WritePojo(value));
			});
		}

		this.records.put(address, record);
		return address + record.getType().getWords();
	}

	/**
	 * This method takes the string array "channelInput" of the config entry "Channels" and processes the contents.
	 * The channels given by this array are mapped to modbus registers according to the configuration.
	 *
	 * @param channelInput string array containing input from the user.
	 * @return false for success, true for error.
	 */
	private boolean processChannelInput(String[] channelInput, ComponentManager cpm) {
		componentIdsArray = new String[channelInput.length];
		int counter = 0;

		for (String entry : channelInput) {
			String[] inputArray;
			String registerAddressString;
			String modbusTypeString;
			String componentIdString;
			String channelIdString;
			String accessModeString;

			try {
				inputArray = entry.split("/");

				// Just for debugging.
				int arrayLine = 0;
				this.logDebug(this.log, "Debugging Channels input:");
				for (String entryOfArray : inputArray) {
					switch (arrayLine) {
						case 0:
							this.logDebug(this.log, "Address: " + entryOfArray);
							break;
						case 1:
							this.logDebug(this.log, "ModbusType: " + entryOfArray);
							break;
						case 2:
							this.logDebug(this.log, "component-ID: " + entryOfArray);
							break;
						case 3:
							this.logDebug(this.log, "channel-ID: " + entryOfArray);
							break;
						case 4:
							this.logDebug(this.log, "AccessMode: " + entryOfArray);
							break;
					}
					arrayLine++;
				}

				registerAddressString = inputArray[0].trim();
				modbusTypeString = inputArray[1].trim();
				componentIdString = inputArray[2].trim();
				channelIdString = inputArray[3].trim();
				try {
					accessModeString = inputArray[4].trim();
				} catch (Exception e) {
					accessModeString = "READ_WRITE";
				}
			} catch (Exception e) {
				this.logWarn(this.log, "Wrong format in configuration option \"Channels\", line "
						+ (counter + 1) + ". Discarding entry.");
				continue;
			}

			// Check if Module exists and is active.
			OpenemsComponent componentEntry;
			try {
				boolean componentExistsAndIsEnabled = cpm.getComponent(componentIdString) instanceof OpenemsComponent
						&& cpm.getComponent(componentIdString).isEnabled();
				if (componentExistsAndIsEnabled) {
					componentEntry = cpm.getComponent(componentIdString);
					addComponent(componentEntry);
				} else {
					this.logWarn(this.log, "Bad entry in configuration option \"Channels\", line "
							+ (counter + 1) + ": [" + componentIdString + "] is not a valid component-ID. Discarding entry.");
					continue;
				}
			} catch (OpenemsError.OpenemsNamedException e) {
				this.logWarn(this.log, "Bad entry in configuration option \"Channels\", line "
						+ (counter + 1) + ": [" + componentIdString + "] is not a valid component-ID. Discarding entry.");
				continue;
			}

			// Check if Channel exists.
			Channel<?> channelEntry;
			// The channel Id needs to be in UpperCamel format.
			if (channelIdString.toUpperCase().equals(channelIdString)) {
				channelIdString = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, channelIdString);
			}
			try {
				channelEntry = componentEntry.channel(channelIdString);
			} catch (Exception e) {
				this.logWarn(this.log, "Bad entry in configuration option \"Channels\", line "
						+ (counter + 1) + ": [" + channelIdString + "] is not a valid channel of component ["
						+ componentIdString + "]. Discarding entry");
				this.logWarn(this.log, e.toString());
				continue;
			}

			// Verify and parse Modbus Type.
			modbusTypeString = modbusTypeString.toUpperCase();
			ModbusType modbusTypeEntry = ModbusType.UINT16;
			switch (modbusTypeString) {
				case "UINT16":
					modbusTypeEntry = ModbusType.UINT16;
					break;
				case "FLOAT32":
					modbusTypeEntry = ModbusType.FLOAT32;
					break;
				case "FLOAT64":
					modbusTypeEntry = ModbusType.FLOAT64;
					break;
				case "STRING16":
					modbusTypeEntry = ModbusType.STRING16;
					break;
				default:
					this.logWarn(this.log, "Bad entry in configuration option \"Channels\", line "
							+ (counter + 1) + ": [" + modbusTypeString + "] is not a valid Modbus type. "
							+ "Assigning Modbus type based on channel type.");
					switch (channelEntry.getType()) {
						case BOOLEAN:
						case SHORT:
							modbusTypeEntry = ModbusType.UINT16;
							break;
						case INTEGER:
						case FLOAT:
							modbusTypeEntry = ModbusType.FLOAT32;
							break;
						case LONG:
						case DOUBLE:
							modbusTypeEntry = ModbusType.FLOAT64;
							break;
						case STRING:
							modbusTypeEntry = ModbusType.STRING16;
							break;
					}
					this.logWarn(this.log, "Channel " + channelIdString + " is of type " + channelEntry.getType().toString()
							+ ". Assigning Modbus type " + modbusTypeEntry.toString() + ".");
			}

			// Verify and parse address.
			int registerAddress;
			try {
				registerAddress = Integer.parseInt(registerAddressString.trim());
			} catch (NumberFormatException e) {
				this.logWarn(this.log, "Wrong format in configuration option \"Channels\", line "
						+ (counter + 1) + ": [" + registerAddressString + "] is not a number. Assigning next free address.");
				registerAddress = mapToNextFreeAddress(modbusTypeEntry);
				this.logWarn(this.log, "Assigned " + componentIdString + "/" + channelIdString
						+ " to Modbus address " + registerAddress + ".");
			}
			if (registerAddress < 0 || (registerAddress + modbusTypeEntry.getWords() - 1) > 9998) {
				this.logWarn(this.log, "Wrong format in configuration option \"Channels\", line "
						+ (counter + 1) + ": [" + registerAddress + "] is not a possible Modbus holding register address. "
						+ "Assigning next free address.");
				registerAddress = mapToNextFreeAddress(modbusTypeEntry);
				this.logWarn(this.log, "Assigned " + componentIdString + "/" + channelIdString
						+ " to Modbus address " + registerAddress + ".");
			}

			// Verify that the intended address range is free to use.
			if (records.isEmpty() == false) {

				// Check adjacent lower entry.
				if (records.floorKey(registerAddress) != null) {
					int usedRangeLowerEntry = records.floorKey(registerAddress)
							+ records.floorEntry(registerAddress).getValue().getType().getWords() - 1;
					if (usedRangeLowerEntry >= registerAddress) {
						this.logWarn(this.log, "Modbus register address " + registerAddress + " is already used. Each "
								+ "channel must have a unique address! Assigning next free address.");
						registerAddress = mapToNextFreeAddress(modbusTypeEntry);
						this.logWarn(this.log, "Assigned " + componentIdString + "/" + channelIdString
								+ " to Modbus address " + registerAddress + ".");
					}
				}

				// Check adjacent higher entry.
				if (records.ceilingKey(registerAddress) != null) {
					int usedRangeThisEntry = registerAddress + modbusTypeEntry.getWords() - 1;
					if (usedRangeThisEntry >= records.ceilingKey(registerAddress)) {
						this.logWarn(this.log, "Cannot fit channel " + componentIdString + "/"
								+ channelIdString +	" in Modbus register address " + registerAddress + ". Not enough "
								+ "unassigned registers before the next entry. Assigning next free address.");
						registerAddress = mapToNextFreeAddress(modbusTypeEntry);
						this.logWarn(this.log, "Assigned " + componentIdString + "/" + channelIdString
								+ " to Modbus address " + registerAddress + ".");
					}
				}
			}

			// Verify and parse access mode.
			accessModeString = accessModeString.toUpperCase();
			AccessMode accessModeEntry;
			switch (accessModeString) {
				case "READ_WRITE":
					accessModeEntry = AccessMode.READ_WRITE;
					break;
				case "READ_ONLY":
					accessModeEntry = AccessMode.READ_ONLY;
					break;
				case "WRITE_ONLY":
					accessModeEntry = AccessMode.WRITE_ONLY;
					break;
				default:
					this.logWarn(this.log, "Bad entry in configuration option \"Channels\", line "
							+ (counter + 1) + ": [" + accessModeString + "] is not a valid access mode. "
							+ "Assigning default access mode READ_WRITE.");
					accessModeEntry = AccessMode.READ_WRITE;
			}

			// Build entry
			ModbusRecordChannel recordChannelEntry = new ModbusRecordChannel(0, modbusTypeEntry, channelEntry.channelId(), accessModeEntry);
			componentIdsArray[counter] = componentIdString;
			this.components.put(registerAddress, componentEntry.alias());
			addRecordToProcessImage(registerAddress, recordChannelEntry, componentEntry);

			counter++;
		}
		if (this.records.isEmpty()) {
			this.logError(this.log, "No channels defined to map to Modbus. Deactivating.");
			return true;
		}
		return false;
	}

	/**
	 * Finds the lowest unoccupied Modbus register address in "records" that has enough following unoccupied
	 * registers to fit the specified Modbus type.
	 *
	 * @param type the Modbus type (UINT16, FLOAT32, ...), so the method knows how many registers are needed.
	 * @return the register address.
	 */
	private int mapToNextFreeAddress(ModbusType type) {
		int previousKey = 0;
		int previousKeyLength = 0;
		boolean previousKeyExists = false;
		int neededRegisters = type.getWords();

		// Travers the map and see if a gap is big enough.
		for (Map.Entry<Integer, ModbusRecord> entry : records.entrySet()) {
			int gap = entry.getKey() - previousKey - previousKeyLength;
			if (gap >= neededRegisters) {
				return previousKey + previousKeyLength;
			}

			previousKeyExists = true;
			previousKey = entry.getKey();
			previousKeyLength = entry.getValue().getType().getWords();
		}

		// If there is no suitable gap in the map, return next valid address after the last entry.
		if (previousKeyExists) {
			return previousKey + previousKeyLength;
		} else {
			// You land here if the map (records) is empty.
			return 0;
		}
	}

	/**
	 * Prints to the log the list of modbus registers that have a channel mapped to them.
	 */
	private void printModbusAddressMapping() {
		this.logInfo(this.log, "");
		this.logInfo(this.log, "--Modbus address mapping--");
		boolean notice = false;
		for (Map.Entry<Integer, ModbusRecord> entry : records.entrySet()) {
			String keyRange = "";
			switch (entry.getValue().getType()) {
				case UINT16:
					keyRange = entry.getKey().toString();
					break;
				case FLOAT32:
					keyRange = entry.getKey() + "-" + (entry.getKey() + 1);
					notice = true;
					break;
				case FLOAT64:
					keyRange = entry.getKey() + "-" + (entry.getKey() + 3);
					notice = true;
					break;
				case STRING16:
					keyRange = entry.getKey() + "-" + (entry.getKey() + 15);
					notice = true;
					break;
			}
			this.logInfo(this.log, keyRange + " - " + entry.getValue().getType().toString() + " - "
					+ entry.getValue().getName() + " - " + entry.getValue().getAccessMode().toString());
		}
		this.logInfo(this.log, "");
		if (notice) {
			this.logInfo(this.log, "Note: Channels mapped to multiple registers can not be read one register at "
					+ "a time.");
			this.logInfo(this.log, "For example, a FLOAT32 is mapped to address 0. Then you need to read the "
					+ "address range 0-1 to get a value. Reading a single register at address 0 or 1 will return nothing.");
			this.logInfo(this.log, "");
		}
	}


	@Override
	public void run() throws OpenemsNamedException {
		this.apiWorker.run();
	}

	@Override
	protected void logDebug(Logger log, String message) {
		super.logDebug(log, message);
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest message)
			throws OpenemsNamedException {
		switch (message.getMethod()) {
		case GetModbusProtocolRequest.METHOD:
			return CompletableFuture.completedFuture(new GetModbusProtocolResponse(message.getId(), this.records));

		case GetModbusProtocolExportXlsxRequest.METHOD:
			return CompletableFuture.completedFuture(
					new GetModbusProtocolExportXlsxResponse(message.getId(), this.components, this.records));

		}
		return null;
	}

	/**
	 * Gets the AccessMode.
	 * 
	 * @return the {@link AccessMode}
	 */
	protected abstract AccessMode getAccessMode();

	/**
	 * Creates the Modbus slave.
	 *
	 * @return the {@link ModbusSlave}
	 */
	protected abstract com.ghgande.j2mod.modbus.slave.ModbusSlave createModbusSlave() throws ModbusException;

	/**
	 * Gets the Component.
	 * 
	 * @param componentId the Component-ID
	 * 
	 * @return the {@link ModbusSlave} Component
	 */
	protected OpenemsComponent getComponent(String componentId) {
		return this._components.get(componentId);
	}
}
