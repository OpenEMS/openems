package io.openems.edge.controller.api.modbus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.ConfigUtils;
import io.openems.common.utils.FunctionUtils;
import io.openems.common.worker.AbstractWorker;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.jsonapi.JsonrpcEndpointGuard;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.modbusslave.ModbusRecord;
import io.openems.edge.common.modbusslave.ModbusRecordChannel;
import io.openems.edge.common.modbusslave.ModbusRecordCycleValue;
import io.openems.edge.common.modbusslave.ModbusRecordString16;
import io.openems.edge.common.modbusslave.ModbusRecordUint16BlockLength;
import io.openems.edge.common.modbusslave.ModbusRecordUint16Hash;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.common.ApiWorker;
import io.openems.edge.controller.api.common.ApiWorker.WriteHandler;
import io.openems.edge.controller.api.common.Status;
import io.openems.edge.controller.api.common.WriteObject;
import io.openems.edge.controller.api.common.WritePojo;
import io.openems.edge.controller.api.modbus.jsonrpc.GetModbusProtocolExportXlsxRequest;
import io.openems.edge.controller.api.modbus.jsonrpc.GetModbusProtocolExportXlsxResponse;
import io.openems.edge.controller.api.modbus.jsonrpc.GetModbusProtocolRequest;
import io.openems.edge.controller.api.modbus.jsonrpc.GetModbusProtocolResponse;

public abstract class AbstractModbusApi extends AbstractOpenemsComponent
		implements ModbusApi, ComponentJsonApi, Controller {

	public static final int UNIT_ID = 1;
	public static final int DEFAULT_MAX_CONCURRENT_CONNECTIONS = 5;
	private static final int PROCESS_IMAGE_RESET_TIME = 60;

	/**
	 * Holds the link between Modbus start address of a Component and the
	 * Component-ID.
	 */
	protected final TreeMap<Integer, String> components = new TreeMap<>();
	protected final TreeMap<Integer, ModbusRecord> records = new TreeMap<>();
	protected volatile List<ModbusSlave> _components = new CopyOnWriteArrayList<>();
	protected List<OpenemsComponent> invalidComponents = new CopyOnWriteArrayList<>();
	protected final Logger log = LoggerFactory.getLogger(AbstractModbusApi.class);
	protected final MyProcessImage processImage;

	protected Instant lastModbusProcessImageErrorInstant = Instant.MIN;
	protected Clock clock;

	/**
	 * Holds the link between Modbus address and ModbusRecord.
	 */
	protected final ApiWorker apiWorker = new ApiWorker(this,
			new WriteHandler(this.handleWrites(), this::setOverrideStatus, this.handleTimeouts()));

	private AbstractModbusConfig config;

	protected AbstractModbusApi(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[][] furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		this.processImage = new MyProcessImage(this);
	}

	protected void activate(ComponentContext context, ConfigurationAdmin cm, AbstractModbusConfig config, Clock clock)
			throws OpenemsException {
		this.config = config;
		this.clock = clock;
		super.activate(context, config.id(), config.alias(), config.enabled());

		final var filter = ConfigUtils.generateReferenceTargetFilter(this.servicePid(), false, config.componentIds());
		OpenemsComponent.updateReferenceFilterRaw(cm, this.servicePid(), "Component", filter);

		this.apiWorker.setTimeoutSeconds(config.apiTimeout());

		if (!this.isEnabled()) {
			return;
		}

		this.startApiWorker.activate(config.id());

	}

	protected void modified(ComponentContext context, ConfigurationAdmin cm, AbstractModbusConfig config)
			throws OpenemsException {
		super.modified(context, config.id(), config.alias(), config.enabled());

		final var filter = ConfigUtils.generateReferenceTargetFilter(this.servicePid(), false, config.componentIds());
		OpenemsComponent.updateReferenceFilterRaw(cm, this.servicePid(), "Component", filter);

		if (this.config.equals(config)) {
			return;
		}

		this.config = config;

		this.apiWorker.setTimeoutSeconds(config.apiTimeout());

		if (!this.isEnabled()) {
			this.startApiWorker.deactivate();
			return;
		}

		this.startApiWorker.modified(config.id());

	}

	@Override
	protected void deactivate() {
		this.startApiWorker.deactivate();
		super.deactivate();

		// wait until modbus slave was completely closed
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			this.log.warn(e.getMessage());
		}
	}

	protected void onStarted() {
		AbstractModbusApi.this.logInfo(this.log, "ModbusApi started.");
	}

	protected Consumer<Entry<WriteChannel<?>, WriteObject>> handleWrites() {
		return FunctionUtils::doNothing;
	}

	protected void setOverrideStatus(Status status) {
		// do nothing
	}

	protected Runnable handleTimeouts() {
		return FunctionUtils::doNothing;
	}

	protected abstract com.ghgande.j2mod.modbus.slave.ModbusSlave createSlave() throws ModbusException;

	private final AbstractWorker startApiWorker = new AbstractWorker() {

		private static final int DEFAULT_WAIT_TIME = 5000; // 5 seconds

		private final Logger log = LoggerFactory.getLogger(AbstractWorker.class);

		protected com.ghgande.j2mod.modbus.slave.ModbusSlave slave = null;

		protected AbstractModbusConfig currentConfig = null;

		@Override
		protected void forever() throws ModbusException {
			if (this.slave == null) {
				try {
					// start new server
					this.currentConfig = AbstractModbusApi.this.config;
					this.slave = AbstractModbusApi.this.createSlave();
					this.slave.addProcessImage(UNIT_ID, AbstractModbusApi.this.processImage);
					this.slave.open();
					if (isEnabled()) {
						AbstractModbusApi.this.onStarted();
						AbstractModbusApi.this._setUnableToStart(false);
					}
				} catch (ModbusException e) {
					ModbusSlaveFactory.close(this.slave);
					AbstractModbusApi.this.logError(this.log, "Unable to start Modbus-Api: " + e.getMessage());
					AbstractModbusApi.this._setUnableToStart(true);
				}

			} else {
				// regular check for errors
				String error = this.slave.getError();
				if (error != null) {
					AbstractModbusApi.this.logError(this.log, "Unable to start Modbus-Api: " + error);
					AbstractModbusApi.this._setUnableToStart(true);
					this.stopSlave();
				} else if (!this.currentConfig.equals(AbstractModbusApi.this.config)) {
					this.stopSlave();
				}
			}
		}

		private void stopSlave() {
			ModbusSlaveFactory.close(this.slave);
			this.slave = null;
		}

		@Override
		protected int getCycleTime() {
			return DEFAULT_WAIT_TIME;
		}

	};

	@Override
	public void run() throws OpenemsNamedException {
		if (!this.isEnabled()) {
			return;
		}
		this.updateCycleValues();
		this.apiWorker.run();
		this.resetProcessImageError(this.clock);
	}

	/**
	 * Called by addComponent/removeComponent. Initializes the ModbusRecords, once
	 * all Components are available. Fault-State otherwise.
	 */
	protected synchronized void updateComponents() {
		var config = this.config;

		if (config == null) {
			return;
		}
		if (config.componentIds().length > this._components.size()) {
			if (this.getComponentNoModbusApiFaultChannel().getNextValue().get() != true) {
				this._setComponentMissingFault(true); // Either this or that fault
			}
			return;
		}
		this._setComponentMissingFault(false);

		this.initializeModbusRecords(this.config.metaComponent(), this.config.componentIds());
	}

	protected synchronized void addComponent(OpenemsComponent component) {
		if (!(component instanceof ModbusSlave ms)) {
			this.logError(this.log, "Component [" + component.id() + "] does not implement ModbusSlave");
			this.invalidComponents.add(component);
			this._setComponentNoModbusApiFault(true);
			return;
		}
		this._components.add(ms);
		this.updateComponents();
	}

	protected synchronized void removeComponent(OpenemsComponent component) {
		this._components.remove(component);
		if (this.invalidComponents.remove(component)) {
			if (this.invalidComponents.isEmpty()) {
				this._setComponentNoModbusApiFault(false);
			}
			return;
		}
		this.updateComponents();
	}

	/**
	 * Once every cycle: update the values for each registered
	 * {@link ModbusRecordCycleValue}.
	 */
	@SuppressWarnings("unchecked")
	protected void updateCycleValues() {
		this.records.values() //
				.stream() //
				.filter(ModbusRecordCycleValue.class::isInstance) //
				.map(ModbusRecordCycleValue.class::cast) //
				.forEach(r -> {
					var component = this.getPossiblyDisabledComponent(r.getComponentId());
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
			return this.config.metaComponent();
		}
		return this._components.stream() //
				.filter(c -> componentId.equals(c.id())) //
				.findFirst() //
				.orElse(null);
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
		if (record instanceof ModbusRecordChannel r) {
			r.onWriteValue(value -> {
				var readChannel = component.channel(r.getChannelId());
				if (!(readChannel instanceof WriteChannel wc)) {
					this.logWarn(this.log, "Unable to write to Read-Only-Channel [" + readChannel.address() + "]");
					return;
				}
				this.apiWorker.addValue(wc, new WritePojo(value));
			});
		}

		this.records.put(address, record);
		return address + record.getType().getWords();
	}

	/**
	 * Initialize Modbus-Records for all configured Component-IDs.
	 *
	 * @param metaComponent the {@link Meta} component
	 * @param componentIds  the configured Component-IDs.
	 */
	private void initializeModbusRecords(Meta metaComponent, String[] componentIds) {
		this.records.clear();
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

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(GetModbusProtocolRequest.METHOD, def -> {
			def.setGuards(this.componentMissingGuard(), this.componentNoModbusApiGuard());
		}, call -> {
			return new GetModbusProtocolResponse(call.getRequest().getId(), this.records);
		});
		builder.handleRequest(GetModbusProtocolExportXlsxRequest.METHOD, def -> {
			def.setGuards(this.componentMissingGuard(), this.componentNoModbusApiGuard());
		}, call -> {
			return new GetModbusProtocolExportXlsxResponse(call.getRequest().getId(), this.components, this.records);
		});
	}

	private JsonrpcEndpointGuard componentMissingGuard() {
		return call -> {
			if (this.getComponentMissingFault().get() == true) {
				throw new OpenemsException(this.getComponentMissingFaultChannel().channelDoc().getText());
			}
		};
	}

	private JsonrpcEndpointGuard componentNoModbusApiGuard() {
		return call -> {
			if (this.getComponentNoModbusApiFault().get() == true) {
				throw new OpenemsException(this.getComponentNoModbusApiFaultChannel().channelDoc().getText());
			}
		};
	}

	/**
	 * Format a given channelAddress to a ChannelId.
	 * 
	 * @param channel WriteChannel
	 * @return component_channelId as String
	 */
	public static String formatChannelName(WriteChannel<?> channel) {
		return channel.getComponent().id() + "_" + channel.channelId().name();
	}

	/**
	 * Sets the {@link #PROCESS_IMAGE_FAULT} channel to true and saves the instant
	 * of its last occurrence.
	 * 
	 * @param clock the clock
	 */
	public void setProcessImageFault(Clock clock) {
		this.lastModbusProcessImageErrorInstant = Instant.now(clock);
		this._setProcessImageFault(true);
	}

	/**
	 * Resets the PROCESS_IMAGE_FAULT channel to false if the last recorded error
	 * instant is older than {@value #PROCESS_IMAGE_RESET_TIME} seconds.
	 * 
	 * @param clock the clock
	 */
	public void resetProcessImageError(Clock clock) {
		this._setProcessImageFault(//
				this.lastModbusProcessImageErrorInstant //
						.plusSeconds(PROCESS_IMAGE_RESET_TIME) //
						.isAfter(Instant.now(clock))); //
	}

}
