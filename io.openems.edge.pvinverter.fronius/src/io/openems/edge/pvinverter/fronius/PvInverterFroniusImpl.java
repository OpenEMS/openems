package io.openems.edge.pvinverter.fronius;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.ImmutableMap;
import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S123_WMaxLim_Ena;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.pvinverter.sunspec.AbstractSunSpecPvInverter;
import io.openems.edge.pvinverter.sunspec.Phase;
import io.openems.edge.pvinverter.sunspec.SunSpecPvInverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PV-Inverter.Fronius", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})

public class PvInverterFroniusImpl extends AbstractSunSpecPvInverter
		implements PvInverterFronius, SunSpecPvInverter, ManagedSymmetricPvInverter, ElectricityMeter, ModbusComponent,
		OpenemsComponent, EventHandler, ModbusSlave, TimedataProvider {

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) // from 40002

			// .put(DefaultSunSpecModel.S_103, Priority.LOW) //
			.put(DefaultSunSpecModel.S_113, Priority.LOW) // from 40069
			.put(DefaultSunSpecModel.S_120, Priority.LOW) // from 40159
			.put(DefaultSunSpecModel.S_123, Priority.LOW) // from 40237
			// .put(DefaultSunSpecModel.S_160, Priority.LOW) // from 40263
			.build();

	private static final int READ_FROM_MODBUS_BLOCK = 1;

	private final Logger log = LoggerFactory.getLogger(PvInverterFroniusImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private volatile Timedata timedata = null;

	private List<CalculateEnergyFromPower> energyCalculators = new ArrayList<>();

	private Config config;

	private int numberOfModules = 0;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public PvInverterFroniusImpl() {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				SunSpecPvInverter.ChannelId.values(), //
				PvInverterFronius.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {

		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.readOnly(),
				config.modbusUnitId(), this.cm, "Modbus", config.modbus_id(), READ_FROM_MODBUS_BLOCK, Phase.ALL)) {
			return;
		}

		// No need fetching number of modules if 0 is configured
		if (this.config.modbusBaseAddress() > 0) {
			this.addInitialModbusTask(this.getModbusProtocol());
		}

	}

	private int BASE_ADDRESS;
	private int MODULE_START_ADDRESS;
	private static final int REGISTER_OFFSET = 20; // Number of registers per module
	private boolean staticTasksAdded = false;

	/**
	 * Overrides because Fronius needs value of S123_WMaxLim_Ena to be set every
	 * time a new value is given.
	 *
	 * @param int value - the new limit value
	 * @throws OpenemsException on error
	 */
	@Override
	public void setActivePowerLimit(int value) throws OpenemsNamedException {

		EnumWriteChannel wMaxLimEnaChannel;
		if (this.isSunSpecInitializationCompleted()) {
			// Get Power Limitation Enabled WriteChannel
			wMaxLimEnaChannel = this.getSunSpecChannelOrError(DefaultSunSpecModel.S123.W_MAX_LIM_ENA);
		} else {
			this.log.info("SunSpec model not completely intialized. Skipping PV Limiter");
			return;
		}

		this.getActivePowerLimitChannel().setNextWriteValue(value);

		// has to be written every time
		wMaxLimEnaChannel.setNextWriteValue(S123_WMaxLim_Ena.ENABLED);

	}

	/**
	 * Adds the initial Modbus task to read the number of modules and scale factors.
	 *
	 * @param protocol the {@link ModbusProtocol}
	 * @throws OpenemsException on error
	 */
	private void addInitialModbusTask(ModbusProtocol protocol) throws OpenemsException {
		this.BASE_ADDRESS = this.config.modbusBaseAddress() + 2; // Starting address for S160 Block. i.e. 40264 for
																	// Fronius Symo.
		this.MODULE_START_ADDRESS = BASE_ADDRESS + 17; // Starting address for modules
		protocol.addTask(//
				new FC3ReadRegistersTask(BASE_ADDRESS, Priority.HIGH,

						m(PvInverterFronius.ChannelId.DCA_SF, new SignedWordElement(BASE_ADDRESS)),
						m(PvInverterFronius.ChannelId.DCV_SF, new SignedWordElement(BASE_ADDRESS + 1)),
						m(PvInverterFronius.ChannelId.DCW_SF, new SignedWordElement(BASE_ADDRESS + 2)),
						m(PvInverterFronius.ChannelId.DCWH_SF, new SignedWordElement(BASE_ADDRESS + 3)),
						new DummyRegisterElement(BASE_ADDRESS + 4, BASE_ADDRESS + 5),
						m(PvInverterFronius.ChannelId.N, new SignedWordElement(BASE_ADDRESS + 6))

				));
	}

	private void addStaticModbusTasks(ModbusProtocol protocol, int numberOfModules) throws OpenemsException {
		for (int i = 0; i < numberOfModules; i++) {
			int moduleBaseAddress = MODULE_START_ADDRESS + (i * REGISTER_OFFSET);
			String currentChannelName = "ST" + (i + 1) + "_DC_CURRENT_INTERNAL";
			String voltageChannelName = "ST" + (i + 1) + "_DC_VOLTAGE_INTERNAL";
			String powerChannelName = "ST" + (i + 1) + "_DC_POWER_INTERNAL";
			String energyChannelName = "ST" + (i + 1) + "_DC_ENERGY_INTERNAL";

			protocol.addTask(//
					new FC3ReadRegistersTask(moduleBaseAddress, Priority.LOW,
							m(PvInverterFronius.ChannelId.valueOf(currentChannelName), //
									new UnsignedWordElement(moduleBaseAddress)),
							m(PvInverterFronius.ChannelId.valueOf(voltageChannelName), //
									new UnsignedWordElement(moduleBaseAddress + 1)),
							m(PvInverterFronius.ChannelId.valueOf(powerChannelName), //
									new UnsignedWordElement(moduleBaseAddress + 2)),
							m(PvInverterFronius.ChannelId.valueOf(energyChannelName), //
									new UnsignedDoublewordElement(moduleBaseAddress + 3))));
		}

	}

	private void pvDataHandler() throws OpenemsNamedException {

		// No base address for S160 is configured
		if (this.config.modbusBaseAddress() == 0) {
			return;
		}
		if (!this.isSunSpecInitializationCompleted()) {
			// Do nothing until SunSpec is initialized
			return;
		}
		if (this.staticTasksAdded == false) {

			try { // We need to know the number of modules
				IntegerReadChannel numberOfModulesChannel = this.channel(PvInverterFronius.ChannelId.N);
				this.numberOfModules = numberOfModulesChannel.value().getOrError().intValue();
			} catch (OpenemsException e) {
				this.log.error("Number of modules unknown");
				return;
			}
			if (this.numberOfModules > 0) {

				try {
					this.addStaticModbusTasks(this.getModbusProtocol(), this.numberOfModules);
					this.staticTasksAdded = true;
					// Add energy calculation channels
					// Initialize energy calculators
					for (int i = 0; i < this.numberOfModules; i++) { // Limit to 12 modules
						String energyChannelName = "ST" + (i + 1) + "_DC_ENERGY";
						IntegerReadChannel energyChannelId = this.getChannelByName(energyChannelName);
						CalculateEnergyFromPower calculateEnergy = new CalculateEnergyFromPower(this,
								energyChannelId.channelId());
						this.energyCalculators.add(calculateEnergy);
					}

					return;
				} catch (OpenemsException e) {
					this.log.error("Error adding static Modbus tasks", e);
				}
			}

		}

		try {
			// modbus Task is active and Sunspec is initialized
			IntegerReadChannel currentScaleFactorChannel = this.channel(PvInverterFronius.ChannelId.DCA_SF);
			int currentScaleFactor = currentScaleFactorChannel.value().getOrError().intValue();

			IntegerReadChannel voltageScaleFactorChannel = this.channel(PvInverterFronius.ChannelId.DCV_SF);
			int voltageScaleFactor = voltageScaleFactorChannel.value().getOrError().intValue();

			IntegerReadChannel powerScaleFactorChannel = this.channel(PvInverterFronius.ChannelId.DCW_SF);
			int powerScaleFactor = powerScaleFactorChannel.value().getOrError().intValue();

			IntegerReadChannel energyScaleFactorChannel = this.channel(PvInverterFronius.ChannelId.DCWH_SF);
			int energyScaleFactor = energyScaleFactorChannel.value().getOrError().intValue();

			for (int i = 0; i < this.numberOfModules; i++) {

				// Internal values without scale factor
				String currentChannelNameInternal = "ST" + (i + 1) + "_DC_CURRENT_INTERNAL";
				String voltageChannelNameInternal = "ST" + (i + 1) + "_DC_VOLTAGE_INTERNAL";
				String powerChannelNameInternal = "ST" + (i + 1) + "_DC_POWER_INTERNAL";
				String energyChannelNameInternal = "ST" + (i + 1) + "_DC_ENERGY_INTERNAL";

				IntegerReadChannel currentChannelInternal = this.getChannelByName(currentChannelNameInternal);
				IntegerReadChannel voltageChannelInternal = this.getChannelByName(voltageChannelNameInternal);
				IntegerReadChannel powerChannelInternal = this.getChannelByName(powerChannelNameInternal);
				IntegerReadChannel energyChannelInternal = this.getChannelByName(energyChannelNameInternal);

				// Target Channels
				String currentChannelName = "ST" + (i + 1) + "_DC_CURRENT";
				String voltageChannelName = "ST" + (i + 1) + "_DC_VOLTAGE";
				String powerChannelName = "ST" + (i + 1) + "_DC_POWER";
				String energyChannelName = "ST" + (i + 1) + "_DC_ENERGY";

				// final value is stored in mA
				this.updateChannelValues(currentChannelInternal, currentChannelName, (currentScaleFactor + 3));
				this.updateChannelValues(voltageChannelInternal, voltageChannelName, voltageScaleFactor);
				this.updateChannelValues(powerChannelInternal, powerChannelName, powerScaleFactor);
				/**
				 * if the energy channel has no value (i.e. STP60-110) we calculate it
				 */
				if (energyChannelInternal.value().getOrError().intValue() == 0) {
					Integer dcPower = this.getChannelByName(powerChannelName).value().getOrError();
					if (dcPower == null) {
						// Not available
						this.energyCalculators.get(i).update(null);
					} else if (dcPower > 0) {
						this.energyCalculators.get(i).update(dcPower);
					} else { // UNDEFINED??
						this.energyCalculators.get(i).update(null);
					}
				} else {
					this.updateChannelValues(energyChannelInternal, energyChannelName, energyScaleFactor);
				}

			}

		} catch (OpenemsException e) {
			this.log.error("Number of modules unknown");
			return;
		}

	}

	private IntegerReadChannel getChannelByName(String channelName) {
		try {
			return this.channel(PvInverterFronius.ChannelId.valueOf(channelName));
		} catch (IllegalArgumentException e) {
			this.log.error("Channel with name [" + channelName + "] does not exist.", e);
			return null;
		}
	}

	/**
	 * Updates the channel values based on the scale factor.
	 *
	 * @param internalChannel     the internal channel
	 * @param externalChannelName the external channel name
	 * @param scaleFactor         the scale factor
	 * @throws OpenemsNamedException on error
	 */
	private void updateChannelValues(IntegerReadChannel internalChannel, String externalChannelName, int scaleFactor)
			throws OpenemsNamedException {
		if (internalChannel != null) {
			try {
				Integer value = internalChannel.value().getOrError().intValue();

				if (value == 65535) { // return if "fill-values" are used
					logDebug(log, "Error Channel: " + externalChannelName + " is 65535 (SF:" + scaleFactor
							+ "). No values saved.");
					return;
				}

				if (value != null) { // Sometimes wrong values while no pv production
					double scaledValue = value * Math.pow(10, scaleFactor);
					int targetValue = (int) scaledValue;
					IntegerReadChannel externalChannel = this.getChannelByName(externalChannelName);
					if (externalChannel != null) {
						externalChannel.setNextValue(targetValue);
						logDebug(log,
								"Channel: " + externalChannelName + ":  " + targetValue + " (SF:" + scaleFactor + ")");
					} else {
						logError(log, "Error Channel: " + externalChannelName + " is NULL (SF:" + scaleFactor + ")");
					}

				}
			} catch (OpenemsException e) {
				logError(log, "Error Channel: " + externalChannelName + " (SF:" + scaleFactor + ")");
			}

		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);

		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:

			try {

				this.pvDataHandler();
			} catch (OpenemsNamedException e) {
				this.log.warn("Cannot write String channel data yet");
			}

			break;
		}
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricPvInverter.getModbusSlaveNatureTable(accessMode));
	}

	/**
	 * Uses Info Log for further debug features.
	 */
	@Override
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}
}
