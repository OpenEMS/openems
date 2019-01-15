package io.openems.edge.controller.api.modbus;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

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

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
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
import io.openems.edge.common.worker.AbstractWorker;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.core.ApiWorker;
import io.openems.edge.controller.api.core.WritePojo;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.ModbusTcp", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ModbusTcpApi extends AbstractOpenemsComponent implements Controller, OpenemsComponent, JsonApi {

	public final static int UNIT_ID = 1;
	public final static int DEFAULT_PORT = 502;
	public final static int DEFAULT_MAX_CONCURRENT_CONNECTIONS = 5;

	private final Logger log = LoggerFactory.getLogger(ModbusTcpApi.class);

	private final ApiWorker apiWorker = new ApiWorker();
	private final MyProcessImage processImage;

	/**
	 * Holds the link between Modbus address and ModbusRecord
	 */
	protected final TreeMap<Integer, ModbusRecord> records = new TreeMap<>();

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected Meta metaComponent = null;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	protected void addComponent(ModbusSlave component) {
		this._components.put(component.id(), component);
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	protected volatile Timedata timedataService = null;

	@Reference
	protected ConfigurationAdmin cm;

	protected volatile Map<String, ModbusSlave> _components = new HashMap<>();
	private String[] componentIds = new String[0];
	private int port = ModbusTcpApi.DEFAULT_PORT;
	private int maxConcurrentConnections = ModbusTcpApi.DEFAULT_MAX_CONCURRENT_CONNECTIONS;

	public ModbusTcpApi() {
		this.processImage = new MyProcessImage(this);

		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Activate
	void activate(ComponentContext context, Config config) throws ModbusException, OpenemsException {
		super.activate(context, config.id(), config.enabled());

		// update filter for 'components'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Component", config.component_ids())) {
			return;
		}

		this.port = config.port();
		this.maxConcurrentConnections = config.maxConcurrentConnections();
		this.componentIds = config.component_ids();
		this.apiWorker.setTimeoutSeconds(config.apiTimeout());

		if (!this.isEnabled()) {
			// abort if disabled
			return;
		}

		// Add Meta-Component to _components
		this.addComponent(this.metaComponent);

		// Initialize Modbus Records
		this.initializeModbusRecords();

		// Start Modbus-Server
		this.startApiWorker.activate(config.id());
	}

	@Deactivate
	protected void deactivate() {
		this.startApiWorker.deactivate();
		ModbusSlaveFactory.close();
		super.deactivate();
	}

	private final AbstractWorker startApiWorker = new AbstractWorker() {

		private final static int DEFAULT_WAIT_TIME = 5000; // 5 seconds

		private com.ghgande.j2mod.modbus.slave.ModbusSlave slave = null;

		@Override
		protected void forever() {
			if (this.slave == null) {
				try {
					// start new server
					this.slave = ModbusSlaveFactory.createTCPSlave(ModbusTcpApi.this.port,
							ModbusTcpApi.this.maxConcurrentConnections);
					slave.addProcessImage(UNIT_ID, ModbusTcpApi.this.processImage);
					slave.open();
					ModbusTcpApi.this.logInfo(ModbusTcpApi.this.log, "Modbus/TCP Api started on port ["
							+ ModbusTcpApi.this.port + "] with UnitId [" + ModbusTcpApi.UNIT_ID + "].");
				} catch (ModbusException e) {
					ModbusSlaveFactory.close();
					ModbusTcpApi.this.logError(ModbusTcpApi.this.log, "Unable to start Modbus/TCP Api on port ["
							+ ModbusTcpApi.this.port + "]: " + e.getMessage());
				}

			} else {
				// regular check for errors
				String error = slave.getError();
				if (error != null) {
					ModbusTcpApi.this.logError(ModbusTcpApi.this.log,
							"Unable to start Modbus/TCP Api on port [" + ModbusTcpApi.this.port + "]: " + error);
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

	private void initializeModbusRecords() {
		// Add generic header
		this.records.put(0, new ModbusRecordUint16Hash(0, "OpenEMS"));
		int nextAddress = 1;

		// add Meta-Component
		nextAddress = this.addMetaComponentToProcessImage(nextAddress);

		// add remaining components; sorted by configured componentIds
		for (String id : this.componentIds) {
			// find next component in order
			ModbusSlave component = this._components.get(id);
			if (component == null) {
				log.warn("Required Component [" + id + "] is not available.");
				break;
			}

			// add component to process image
			nextAddress = this.addComponentToProcessImage(nextAddress, component);
		}

		for (Entry<Integer, ModbusRecord> entry : this.records.entrySet()) {
			log.info(entry.getKey() + ": " + entry.getValue());
		}
	}

	/**
	 * Adds the Meta-Component to the Process Image
	 * 
	 * @param startAddress
	 * @return
	 */
	private int addMetaComponentToProcessImage(int startAddress) {
		ModbusSlave component = this.metaComponent;
		ModbusSlaveTable table = component.getModbusSlaveTable();

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
	 * Adds a Component to the Process Image
	 * 
	 * @param startAddress
	 * @param component
	 * @return
	 */
	private int addComponentToProcessImage(int startAddress, ModbusSlave component) {
		ModbusSlaveTable table = component.getModbusSlaveTable();

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
	 * Adds a Record to the process image at the given address
	 * 
	 * @param address
	 * @param record
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
					this.log.warn("Unable to write to Read-Only-Channel [" + readChannel.address() + "]");
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
	public void run() {
		this.apiWorker.run();
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
	public JsonrpcResponseSuccess handleJsonrpcRequest(JsonrpcRequest message) throws OpenemsNamedException {
		switch (message.getMethod()) {
		case GetModbusProtocolRequest.METHOD:
			return new GetModbusProtocolResponse(message.getId(), this.records);
		}
		return null;
	}
}
