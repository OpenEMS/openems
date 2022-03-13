package io.openems.edge.controller.api.modbus;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

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

	private int port = DEFAULT_PORT;
	private int maxConcurrentConnections = DEFAULT_MAX_CONCURRENT_CONNECTIONS;

	protected void addComponent(ModbusSlave component) {
		this._components.put(component.id(), component);
	}

	private volatile Map<String, ModbusSlave> _components = new HashMap<>();

	public AbstractModbusTcpApi(String implementationName,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		this.implementationName = implementationName;
		this.processImage = new MyProcessImage(this);
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled, ConfigurationAdmin cm,
			Meta metaComponent, String[] componentIds, int apiTimeout, int port, int maxConcurrentConnections)
			throws OpenemsException {
		super.activate(context, id, alias, enabled);

		// configuration settings
		this.port = port;
		this.maxConcurrentConnections = maxConcurrentConnections;

		// update filter for 'components'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "Component", componentIds)) {
			return;
		}

		this.apiWorker.setTimeoutSeconds(apiTimeout);

		if (!this.isEnabled()) {
			// abort if disabled
			return;
		}

		// Add Meta-Component to _components
		this.addComponent(metaComponent);

		// Initialize Modbus Records
		this.initializeModbusRecords(metaComponent, componentIds);

		// Start Modbus-Server
		this.startApiWorker.activate(id);
	}

	@Override
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
			var port = AbstractModbusTcpApi.this.port;
			if (this.slave == null) {
				try {
					// start new server
					this.slave = ModbusSlaveFactory.createTCPSlave(port,
							AbstractModbusTcpApi.this.maxConcurrentConnections);
					this.slave.addProcessImage(UNIT_ID, AbstractModbusTcpApi.this.processImage);
					this.slave.open();
					AbstractModbusTcpApi.this.logInfo(this.log, AbstractModbusTcpApi.this.implementationName
							+ " started on port [" + port + "] with UnitId [" + AbstractModbusTcpApi.UNIT_ID + "].");
					AbstractModbusTcpApi.this._setUnableToStart(false);
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
				if (error != null) {
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
			var component = this._components.get(id);
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
	 * Gets the Component.
	 *
	 * @param componentId the Component-ID
	 *
	 * @return the {@link ModbusSlave} Component
	 */
	protected ModbusSlave getComponent(String componentId) {
		return this._components.get(componentId);
	}
}
