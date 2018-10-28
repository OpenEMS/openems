package io.openems.edge.controller.api.modbus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.modbusslave.ModbusRecord;
import io.openems.edge.common.modbusslave.ModbusRecordChannel;
import io.openems.edge.common.modbusslave.ModbusRecordString16;
import io.openems.edge.common.modbusslave.ModbusRecordUint16;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.core.ApiController;
import io.openems.edge.controller.api.core.ApiWorker;
import io.openems.edge.controller.api.core.WritePOJO;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.ModbusTcp", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ModbusTcpApi extends AbstractOpenemsComponent implements Controller, ApiController, OpenemsComponent {

	public final static short OPENEMS_IDENTIFIER = (short) "OpenEMS".hashCode();
	public final static int UNIT_ID = 1;

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

	public ModbusTcpApi() {
		this.processImage = new MyProcessImage(this);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws ModbusException, OpenemsException {
		// update filter for 'components'
		if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "Component",
				config.component_ids())) {
			return;
		}
		super.activate(context, config.service_pid(), config.id(), config.enabled());

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

		/*
		 * Start Modbus-Server
		 */
		try {
			ModbusSlaveFactory.close();
			com.ghgande.j2mod.modbus.slave.ModbusSlave slave = ModbusSlaveFactory.createTCPSlave(config.port(),
					config.maxConcurrentConnections());
			slave.addProcessImage(UNIT_ID, this.processImage);
			slave.open();
			String error = slave.getError();
			if (error != null) {
				throw new OpenemsException(error);
			}
			log.info("Modbus/TCP Api started on port [" + config.port() + "] with UnitId [" + ModbusTcpApi.UNIT_ID
					+ "].");
		} catch (ModbusException | OpenemsException e) {
			this.logError(this.log,
					"Unable to start Modbus/TCP Api on port [" + config.port() + "]: " + e.getMessage());
			throw e;
		}
	}

	@Deactivate
	protected void deactivate() {
		ModbusSlaveFactory.close();
		super.deactivate();
	}

	private void initializeModbusRecords() {
		// Add generic header
		this.records.put(0, new ModbusRecordUint16(0, ModbusTcpApi.OPENEMS_IDENTIFIER));
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
				new ModbusRecordUint16(-1, (short) table.getLength()), component);

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
		int nextAddress = this.addRecordToProcessImage(startAddress, new ModbusRecordString16(-1, component.id()),
				component);
		this.addRecordToProcessImage(nextAddress, new ModbusRecordUint16(-1, (short) table.getLength()), component);
		nextAddress = startAddress + 20;
		int nextNatureAddress = nextAddress;

		// add all Nature-Tables
		for (ModbusSlaveNatureTable natureTable : table.getNatureTables()) {
			// add the Interface Hash-Code and Length
			nextAddress = this.addRecordToProcessImage(nextNatureAddress,
					new ModbusRecordUint16(-1, natureTable.getNatureHash()), component);
			nextAddress = this.addRecordToProcessImage(nextAddress,
					new ModbusRecordUint16(-1, (short) natureTable.getLength()), component);

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
				this.apiWorker.addValue(channel, new WritePOJO(value));
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
	public List<OpenemsComponent> getComponents() {
		List<OpenemsComponent> result = new ArrayList<>();
		for (ModbusSlave component : this._components.values()) {
			result.add(component);
		}
		return result;
	}

	@Override
	public ConfigurationAdmin getConfigurationAdmin() {
		return this.cm;
	}

	@Override
	public Timedata getTimedataService() {
		return this.timedataService;
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}
}
