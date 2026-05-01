package io.openems.edge.heat.askoma;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.channel.ChannelUtils.setWriteValueIfNotRead;
import static io.openems.edge.heat.askoma.AskomaConstants.FAST_HEAT_DURATION;
import static io.openems.edge.heat.askoma.AskomaConstants.OFF_ACTIVE_POWER;
import static io.openems.edge.meter.api.ElectricityMeter.calculatePhasesFromActivePower;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.io.IOException;
import java.time.Instant;
import java.util.Hashtable;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.ChannelUtils;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.heat.api.Heat;
import io.openems.edge.heat.api.ManagedHeatElement;
import io.openems.edge.heat.api.Status;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Heat.Askoma", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=CONSUMPTION_METERED" //
		})
@GenerateTargetsFromReferences("Modbus")
public class HeatAskomaImpl extends AbstractOpenemsModbusComponent implements HeatAskoma, ModbusComponent,
		OpenemsComponent, ElectricityMeter, Heat, ManagedHeatElement, TimedataProvider, Controller {

	private final Logger log = LoggerFactory.getLogger(HeatAskomaImpl.class);

	// gets the total energy consumption in kWh
	private final CalculateEnergyFromPower totalEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

	private Config config = null;
	private Instant fastHeatStartedAt;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private ConfigurationAdmin configurationAdmin;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private Sum sum;

	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))")
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public HeatAskomaImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				HeatAskoma.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Heat.ChannelId.values(), //
				ManagedHeatElement.ChannelId.values(), //
				Controller.ChannelId.values() //
		);

		calculatePhasesFromActivePower(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		var protocol = new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(109, Priority.HIGH, //
						m(new BitsWordElement(109, this) //
								.bit(0, HeatAskoma.ChannelId.HEATER1_ACTIVE) //
								.bit(1, HeatAskoma.ChannelId.HEATER2_ACTIVE) //
								.bit(2, HeatAskoma.ChannelId.HEATER3_ACTIVE) //
								.bit(3, HeatAskoma.ChannelId.PUMP_ACTIVE) //
								.bit(4, HeatAskoma.ChannelId.RELAYBOARD_IS_CONNECTED) //
								.bit(5, HeatAskoma.ChannelId.HEATER_1_2_3_CURRENT_FLOW) //
								.bit(6, HeatAskoma.ChannelId.HEAT_PUMP_REQUEST_ACTIVE) //
								.bit(7, HeatAskoma.ChannelId.EMERGENCY_MODE_ACTIVE) //
								.bit(8, HeatAskoma.ChannelId.LEGIONELLA_PROTECTION_ACTIVE) //
								.bit(9, HeatAskoma.ChannelId.ANALOG_INPUT_ACTIVE) //
								.bit(10, HeatAskoma.ChannelId.LOAD_SETPOINT_ACTIVE) //
								.bit(11, HeatAskoma.ChannelId.LOAD_FEEDIN_ACTIVE) //
								.bit(12, HeatAskoma.ChannelId.AUTO_HEATER_OFF_ACTIVE) //
								.bit(13, HeatAskoma.ChannelId.PUMP_RELAY_FOLLOW_UP_ACTIVE) //
								.bit(14, HeatAskoma.ChannelId.TEMPERATURE_LIMIT_REACHED) //
								.bit(15, HeatAskoma.ChannelId.ANY_ERROR_OCCURRED)), //

						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedWordElement(110))),

				new FC3ReadRegistersTask(597, Priority.LOW, //
						m(HeatAskoma.ChannelId.TEMPERATURE_SETPOINT, new UnsignedWordElement(597), SCALE_FACTOR_1)),

				new FC4ReadInputRegistersTask(638, Priority.HIGH, //
						m(Heat.ChannelId.TEMPERATURE, new UnsignedWordElement(638), SCALE_FACTOR_1))); //

		if (!this.config.readOnly()) {
			// Askoma spec: MODBUS_CMD_LOAD_FEEDIN_VALUE, signed int16, -30000..30000 W
			protocol.addTask(new FC3ReadRegistersTask(202, Priority.HIGH, //
					m(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, new SignedWordElement(202))));

			protocol.addTask(new FC6WriteRegisterTask(202, //
					m(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, new SignedWordElement(202))));
		}

		return protocol;
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.CONSUMPTION_METERED;
	}

	@Override
	public void run() throws OpenemsNamedException {
		this.totalEnergy.update(this.getActivePower().get());
		if (this.config.readOnly()) {
			// Write control is not allowed in read-only mode
			setValue(this, ManagedHeatElement.ChannelId.CONTROL_NOT_ALLOWED, true);
		} else {
			setValue(this, ManagedHeatElement.ChannelId.CONTROL_NOT_ALLOWED, false);
			this.applyTargetActivePower();
		}
		this.updateStatusChannel();
		this.updateModeChannel();

	}

	private void updateModeChannel() {
		setValue(this, HeatAskoma.ChannelId.MODE, ChannelMode.fromMode(this.config.mode()));
	}

	private void applyTargetActivePower() {
		switch (this.config.mode()) {
		case OFF -> this.handleModeOff();
		case FAST_HEAT -> this.handleModeFastHeat();
		case SURPLUS -> this.handleModeSurplus();
		}
	}

	private void handleModeOff() {
		this.resetFastHeatState();
		this.setTargetActivePowerForHeatElement(OFF_ACTIVE_POWER);
	}

	private void handleModeFastHeat() {
		if (this.fastHeatStartedAt == null) {
			this.fastHeatStartedAt = this.componentManager.getClock().instant();
		}
		var currentTemp = this.getTemperatureChannel().value();
		var targetTemp = this.getTemperatureSetpoint();
		this.logInfo(this.log, "handle Fast Heat currentTemp %s, targetTemp %s".formatted(currentTemp, targetTemp));

		if (isTargetTemperatureReached(currentTemp, targetTemp)) {
			this.switchToSurplus();
			return;
		}

		// fallback
		if (this.isFastHeatExpired()) {
			this.switchToSurplus();
			return;
		}

		// Request maximum heating power (slightly above configured max to reach the top
		// step)
		final var maxPower = this.config.maxHeatPower() + 50;
		// due to using Register 202 the value must be negativ
		this.setTargetActivePowerForHeatElement(-maxPower);
	}

	private boolean isFastHeatExpired() {
		if (this.fastHeatStartedAt == null) {
			return false;
		}
		return !this.componentManager.getClock().instant().isBefore(this.fastHeatStartedAt.plus(FAST_HEAT_DURATION));
	}

	private void switchToSurplus() {
		this.resetFastHeatState();
		this.updateConfigToSurplus();
	}

	private static boolean isTargetTemperatureReached(Value<Integer> currentTemp, Value<Integer> targetTemp) {
		return currentTemp.isDefined() && targetTemp.isDefined() && currentTemp.get() >= targetTemp.get();
	}

	private void handleModeSurplus() {
		this.resetFastHeatState();
		var gridActivePower = this.sum.getGridActivePower().orElse(0);
		this.setTargetActivePowerForHeatElement(gridActivePower);
	}

	private void setTargetActivePowerForHeatElement(Integer requestedActivePower) {
		IntegerWriteChannel targetActivePowerChannel = this.channel(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER);
		try {
			setWriteValueIfNotRead(targetActivePowerChannel, requestedActivePower);
		} catch (OpenemsNamedException e) {
			this.logError(this.log,
					"Unable to set TARGET_ACTIVE_POWER to [" + requestedActivePower + "]: " + e.getMessage());
		}
	}

	protected void updateStatusChannel() {
		ChannelUtils.setValue(this, Heat.ChannelId.STATUS, this.calculateStatus());
	}

	private Status calculateStatus() {
		if (this.getHeaterCurrentFlow().orElse(false)) {
			// heating
			return Status.EXCESS;
		}
		if (this.getTemperatureLimiteReached().orElse(false)) {
			// Temperature limit reached
			return Status.TEMPERATURE_REACHED;
		}
		// no heating
		return Status.NO_CONTROL_SIGNAL;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	private void updateConfigToSurplus() {
		try {
			var configuration = this.configurationAdmin.getConfiguration(this.servicePid(), "?");
			var properties = configuration.getProperties();
			if (properties == null) {
				properties = new Hashtable<>();
			}
			properties.put("mode", Mode.SURPLUS.name());
			configuration.update(properties);
		} catch (IOException e) {
			this.logError(this.log, "Failed to update mode to Surplus: " + e.getMessage());
		}
	}

	private void resetFastHeatState() {
		this.fastHeatStartedAt = null;
	}

}
