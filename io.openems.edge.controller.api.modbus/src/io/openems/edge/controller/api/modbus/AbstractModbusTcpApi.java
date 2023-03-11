package io.openems.edge.controller.api.modbus;

import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

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
import io.openems.common.utils.ConfigUtils;
import io.openems.common.worker.AbstractWorker;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.modbusslave.ModbusRecord;
import io.openems.edge.common.modbusslave.ModbusRecordChannel;
import io.openems.edge.common.modbusslave.ModbusRecordCycleValue;
import io.openems.edge.common.modbusslave.ModbusRecordString16;
import io.openems.edge.common.modbusslave.ModbusRecordUint16BlockLength;
import io.openems.edge.common.modbusslave.ModbusRecordUint16Hash;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.user.User;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.common.ApiWorker;
import io.openems.edge.controller.api.common.WritePojo;
import io.openems.edge.controller.api.modbus.jsonrpc.GetModbusProtocolExportXlsxRequest;
import io.openems.edge.controller.api.modbus.jsonrpc.GetModbusProtocolExportXlsxResponse;
import io.openems.edge.controller.api.modbus.jsonrpc.GetModbusProtocolRequest;
import io.openems.edge.controller.api.modbus.jsonrpc.GetModbusProtocolResponse;

public abstract class AbstractModbusTcpApi extends AbstractOpenemsComponent
		implements ModbusTcpApi, Controller, OpenemsComponent, JsonApi {

	public static final int UNIT_ID = 1;
	public static final int DEFAULT_PORT = 502;
	public static final int DEFAULT_MAX_CONCURRENT_CONNECTIONS = 5;

	protected final ApiWorker apiWorker = new ApiWorker(this);

	private final Logger log = LoggerFactory.getLogger(AbstractModbusTcpApi.class);
	private final MyProcessImage processImage;
	private final String implementationName;

	/**
	 * Holds the link between Modbus address and ModbusRecord.
	 */
	protected final TreeMap<Integer, ModbusRecord> records = new TreeMap<>();

	/**
	 * Holds the link between Modbus start address of a Component and the
	 * Component-ID.
	 */
	private final TreeMap<Integer, String> components = new TreeMap<>();

	private ConfigRecord config;

	protected synchronized void addComponent(OpenemsComponent component) {
		if (!(component instanceof ModbusSlave)) {
			this.logError(this.log, "Component [" + component.id() + "] does not implement ModbusSlave");
			this._setComponentNoModbusApiFault(true);
			return;
		}
		this._components.add((ModbusSlave) component);
		this.updateComponents();
	}

	protected synchronized void removeComponent(OpenemsComponent component) {
		this._components.remove(component);
		this.updateComponents();
	}

	private volatile List<ModbusSlave> _components = new CopyOnWriteArrayList<>();

	public AbstractModbusTcpApi(String implementationName,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		this.implementationName = implementationName;
		this.processImage = new MyProcessImage(this);
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled, ConfigurationAdmin cm,
			ConfigRecord config) throws OpenemsException {
		super.activate(context, id, alias, enabled);

		// configuration settings
		this.config = config;

		// update filter for 'Components'; allow disable components
		final var filter = ConfigUtils.generateReferenceTargetFilter(this.servicePid(), false, config.componentIds);
		if (OpenemsComponent.updateReferenceFilterRaw(cm, this.servicePid(), "Component", filter)) {
			return;
		}

		this.apiWorker.setTimeoutSeconds(config.apiTimeout);

		if (!this.isEnabled()) {
			// abort if disabled
			return;
		}

		// Start Modbus-Server
		this.startApiWorker.activate(id);

		this.updateComponents();
	}

	/**
	 * Called by addComponent/removeComponent. Initializes the ModbusRecords, once
	 * all Components are available. Fault-State otherwise.
	 */
	private synchronized void updateComponents() {
		// Check if all Components are available
		var config = this.config;
		if (config == null) {
			return;
		}
		if (config.componentIds.length > this._components.size()) {
			if (this.getComponentNoModbusApiFaultChannel().getNextValue().get() != Boolean.TRUE) {
				this._setComponentMissingFault(true); // Either this or that fault
			}
			return;
		}
		this._setComponentMissingFault(false);

		// Initialize Modbus Records
		this.initializeModbusRecords(this.config.metaComponent, this.config.componentIds);
	}

	@Override
	protected void deactivate() {
		this.startApiWorker.deactivate();
		ModbusSlaveFactory.close();
		super.deactivate();

		// wait until modbus slave was completely closed
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			this.log.warn(e.getMessage());
		}
	}

	private final AbstractWorker startApiWorker = new AbstractWorker() {

		private static final int DEFAULT_WAIT_TIME = 5000; // 5 seconds

		private final Logger log = LoggerFactory.getLogger(AbstractWorker.class);

		private com.ghgande.j2mod.modbus.slave.ModbusSlave slave = null;

		@Override
		protected void forever() {
			var port = AbstractModbusTcpApi.this.config.port;
			if (this.slave == null) {
				try {
					// start new server
					this.slave = ModbusSlaveFactory.createTCPSlave(port,
							AbstractModbusTcpApi.this.config.maxConcurrentConnections);
					this.slave.addProcessImage(UNIT_ID, AbstractModbusTcpApi.this.processImage);
					this.slave.open();
					AbstractModbusTcpApi.this.logInfo(this.log, AbstractModbusTcpApi.this.implementationName
							+ " started on port [" + port + "] with UnitId [" + AbstractModbusTcpApi.UNIT_ID + "].");

				} catch (ModbusException e) {
					ModbusSlaveFactory.close();
					AbstractModbusTcpApi.this.logError(this.log,
							"Unable to start " + AbstractModbusTcpApi.this.implementationName + " on port [" + port
									+ "]: " + e.getMessage());
					AbstractModbusTcpApi.this._setUnableToStart(true);
				}

			} else {
				// regular check for errors
				var error = this.slave.getError();
				if (error == null) {
					AbstractModbusTcpApi.this._setUnableToStart(false);

				} else {
					AbstractModbusTcpApi.this.logError(this.log,
							"Unable to start Modbus/TCP Api on port [" + port + "]: " + error);
					AbstractModbusTcpApi.this._setUnableToStart(true);
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
		var nextAddress = 1;

		// add Meta-Component
		nextAddress = this.addMetaComponentToProcessImage(nextAddress, metaComponent);

		// add remaining components; sorted by configured componentIds
		for (String id : componentIds) {
			// find next component in order
			var component = this.getPossiblyDisabledComponent(id);
			if (component == null) { // This should never happen
				this.logWarn(this.log, "Required Component [" + id + "] " //
						+ "is not available. Component may not implement ModbusSlave or is not active.");
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
		var table = component.getModbusSlaveTable(this.getAccessMode());

		// add the Component-Model Length
		var nextAddress = this.addRecordToProcessImage(startAddress,
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
		var table = component.getModbusSlaveTable(this.getAccessMode());

		// add the Component-ID and Component-Model Length
		var nextAddress = this.addRecordToProcessImage(startAddress,
				new ModbusRecordString16(-1, "Component-ID", component.id()), component);
		this.addRecordToProcessImage(nextAddress,
				new ModbusRecordUint16BlockLength(-1, component.id(), (short) table.getLength()), component);
		nextAddress = startAddress + 20;
		var nextNatureAddress = nextAddress;

		// add all Nature-Tables
		for (ModbusSlaveNatureTable natureTable : table.getNatureTables()) {
			// add the Interface Hash-Code and Length
			nextAddress = this.addRecordToProcessImage(nextNatureAddress,
					new ModbusRecordUint16Hash(-1, natureTable.getNatureName()), component);
			nextAddress = this.addRecordToProcessImage(nextAddress,
					new ModbusRecordUint16BlockLength(-1, natureTable.getNatureName(), (short) natureTable.getLength()),
					component);

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
			var r = (ModbusRecordChannel) record;
			r.onWriteValue(value -> {
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

	@Override
	public void run() throws OpenemsNamedException {
		// Enabled?
		if (!this.isEnabled()) {
			return;
		}

		this.updateCycleValues();
		this.apiWorker.run();
	}

	@SuppressWarnings("unchecked")
	/**
	 * Once every cycle: update the values for each registered
	 * {@link ModbusRecordCycleValue}.
	 */
	private void updateCycleValues() {
		this.records.values() //
				.stream() //
				.filter(r -> r instanceof ModbusRecordCycleValue) //
				.map(r -> (ModbusRecordCycleValue<OpenemsComponent>) r) //
				.forEach(r -> {
					OpenemsComponent component = this.getPossiblyDisabledComponent(r.getComponentId());
					if (component != null && component.isEnabled()) {
						r.updateValue(component);
					} else {
						r.updateValue(null);
					}
				});
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
		if (this.getComponentMissingFault().get() == Boolean.TRUE) {
			throw new OpenemsException(this.getComponentMissingFaultChannel().channelDoc().getText());
		}
		if (this.getComponentNoModbusApiFault().get() == Boolean.TRUE) {
			throw new OpenemsException(this.getComponentNoModbusApiFaultChannel().channelDoc().getText());
		}

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
	 * Gets the Component. Be aware, that it might be 'disabled'.
	 *
	 * @param componentId the Component-ID
	 *
	 * @return the {@link ModbusSlave} Component; possibly null
	 */
	protected ModbusSlave getPossiblyDisabledComponent(String componentId) {
		if (componentId == null) {
			return null;
		}
		if (componentId == Meta.SINGLETON_COMPONENT_ID) {
			return this.config.metaComponent;
		}
		return this._components.stream() //
				.filter(c -> componentId.equals(c.id())) //
				.findFirst() //
				.orElse(null);
	}

	protected static class ConfigRecord {
		public final Meta metaComponent;
		public final String[] componentIds;
		public final int apiTimeout;
		public final int port;
		public final int maxConcurrentConnections;

		public ConfigRecord(Meta metaComponent, String[] componentIds, int apiTimeout, int port,
				int maxConcurrentConnections) {
			super();
			this.metaComponent = metaComponent;
			this.componentIds = componentIds;
			this.apiTimeout = apiTimeout;
			this.port = port;
			this.maxConcurrentConnections = maxConcurrentConnections;
		}
	}
}
