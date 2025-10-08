package io.openems.edge.evse.chargepoint.alpitronic;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.net.UnknownHostException;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.chargepoint.alpitronic.common.Alpitronic;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.Alpitronic", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class EvseAlpitronicImpl extends EvseAlpitronic implements EvseChargePoint, ElectricityMeter, OpenemsComponent,
		TimedataProvider, EventHandler, ModbusComponent {

	@Reference
	protected ConfigurationAdmin cm;

	private AlpitronicsUtils utils = new AlpitronicsUtils(this);

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	private final IntFunction<Integer> offset = addr -> addr + this.config.connector().modbusOffset;

	private Config config;

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EvseAlpitronicImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				Alpitronic.ChannelId.values(), //
				EvseAlpitronic.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws UnknownHostException, OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		var modbusProtocol = new ModbusProtocol(this,

				new FC3ReadRegistersTask(this.offset.apply(0), Priority.LOW,
						m(Alpitronic.ChannelId.RAW_CHARGE_POWER_SET,
								new UnsignedDoublewordElement(this.offset.apply(0)))),

				new FC16WriteRegistersTask(this.offset.apply(0),
						m(Alpitronic.ChannelId.APPLY_CHARGE_POWER_LIMIT,
								new UnsignedDoublewordElement(this.offset.apply(0))),
						m(Alpitronic.ChannelId.SETPOINT_REACTIVE_POWER,
								new UnsignedDoublewordElement(this.offset.apply(2)))),

				new FC4ReadInputRegistersTask(this.offset.apply(0), Priority.LOW,
						m(EvseAlpitronic.ChannelId.AVAILABLE_STATE, new UnsignedWordElement(this.offset.apply(0))),
						m(ElectricityMeter.ChannelId.VOLTAGE, new UnsignedDoublewordElement(this.offset.apply(1)),
								SCALE_FACTOR_MINUS_2),
						m(ElectricityMeter.ChannelId.CURRENT, new UnsignedWordElement(this.offset.apply(3)),
								SCALE_FACTOR_MINUS_2),
						/*
						 * TODO: Test charge power register with newer firmware versions. Register value
						 * was always 0 with versions < 1.7.2.
						 */
						m(Alpitronic.ChannelId.RAW_CHARGE_POWER, new UnsignedDoublewordElement(this.offset.apply(4))),
						m(Alpitronic.ChannelId.CHARGED_TIME, new UnsignedWordElement(this.offset.apply(6))),
						m(Alpitronic.ChannelId.CHARGED_ENERGY, new UnsignedWordElement(this.offset.apply(7)),
								SCALE_FACTOR_MINUS_2),
						m(Alpitronic.ChannelId.EV_SOC, new UnsignedWordElement(this.offset.apply(8)),
								SCALE_FACTOR_MINUS_2),
						m(Alpitronic.ChannelId.CONNECTOR_TYPE, new UnsignedWordElement(this.offset.apply(9))),

						/*
						 * Not equals MaximumPower or MinimumPower e.g. EvMaxChargingPower is 99kW, but
						 * ChargePower is 40kW because of temperature, current SoC or
						 * MaximumHardwareLimit.
						 */
						m(Alpitronic.ChannelId.EV_MAX_CHARGING_POWER,
								new UnsignedDoublewordElement(this.offset.apply(10))),
						m(Alpitronic.ChannelId.EV_MIN_CHARGING_POWER,
								new UnsignedDoublewordElement(this.offset.apply(12))),
						m(Alpitronic.ChannelId.VAR_REACTIVE_MAX, new UnsignedDoublewordElement(this.offset.apply(14))),
						m(Alpitronic.ChannelId.VAR_REACTIVE_MIN, new UnsignedDoublewordElement(this.offset.apply(16)),
								INVERT))

		);

		this.addCalculatePowerListeners();
		
		return modbusProtocol;
	}

	/*
	 * TODO: Remove if the charge power register returns valid values with newer
	 * firmware versions.
	 */
	private void addCalculatePowerListeners() {

		// Calculate power from voltage and current
		final Consumer<Value<Integer>> calculatePower = ignore -> {
			this._setActivePower(TypeUtils.getAsType(OpenemsType.INTEGER, TypeUtils.multiply(//
					this.getVoltageChannel().getNextValue().get(), //
					this.getCurrentChannel().getNextValue().get() //
			)));
		};
		this.getVoltageChannel().onSetNextValue(calculatePower);
		this.getCurrentChannel().onSetNextValue(calculatePower);
	}
	
	@Override
	public ChargePointAbilities getChargePointAbilities() {
		return this.utils.getChargePointAbilities(this.config);
	}

	@Override
	public String debugLog() {
		return this.utils.debugLog(this.config);
	}

	@Override
	public void apply(ChargePointActions actions) {
		this.utils.applyChargePointActions(this.config, actions);
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			this.utils.onBeforeProcessImage(this.config);
		}
		}
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return PhaseRotation.L1_L2_L3;
	}

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
	}

	@Override
	public boolean getIsReadyForCharging() {
		return this.getIsReadyForChargingChannel().value().orElse(false);
	}

	@Override
	public int getMinChargePower() {
		return this.config.minHwPower();
	}

	@Override
	public int getMaxChargePower() {
		return this.config.maxHwPower();
	}
}
