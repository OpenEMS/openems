package io.openems.edge.evse.chargepoint.keba.modbus;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_3;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.evse.chargepoint.keba.modbus.KebaModbusUtils.calculateActivePowerL1L2L3;
import static io.openems.edge.evse.chargepoint.keba.modbus.KebaModbusUtils.handleFirmwareVersion;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.net.UnknownHostException;

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
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.chargepoint.keba.common.CommonConfig;
import io.openems.edge.evse.chargepoint.keba.common.EvseKeba;
import io.openems.edge.evse.chargepoint.keba.common.EvseKebaUtils;
import io.openems.edge.evse.chargepoint.keba.common.Keba;
import io.openems.edge.evse.chargepoint.keba.common.KebaModbus;
import io.openems.edge.evse.chargepoint.keba.common.KebaUtils;
import io.openems.edge.evse.chargepoint.keba.common.ProductTypeAndFeatures;
import io.openems.edge.evse.chargepoint.keba.common.enums.PhaseSwitchSource;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.Keba.Modbus", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class EvseKebaModbusImpl extends KebaModbus implements EvseKeba, EvseChargePoint, ElectricityMeter,
		OpenemsComponent, TimedataProvider, EventHandler, ModbusComponent {

	private final KebaUtils kebaUtils = new KebaUtils(this);
	private final EvseKebaUtils evseKebaUtils = new EvseKebaUtils(this);
	private final KebaModbusUtils kebaModbusUtils = new KebaModbusUtils(this);

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private CommonConfig config;

	public EvseKebaModbusImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				Keba.ChannelId.values(), //
				EvseKeba.ChannelId.values(), //
				KebaModbus.ChannelId.values() //
		);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws UnknownHostException, OpenemsException {
		this.config = CommonConfig.from(config);
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId() /* Unit-ID */,
				this.cm, "Modbus", config.modbus_id())) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = CommonConfig.from(config);
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
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
		// NOTE: Changes here must be copied to EvcsKebaModbusImpl as well

		// KEBA protocol definition states:
		// The interval for reading registers is 0,5 seconds. The interval for writing
		// registers is 5 seconds.
		// Consequently we set most registers to Priority.LOW
		final var phaseRotated = this.getPhaseRotation();
		var modbusProtocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(1000, Priority.LOW, //
						m(Keba.ChannelId.CHARGING_STATE, new UnsignedDoublewordElement(1000))), //
				new FC3ReadRegistersTask(1004, Priority.LOW, //
						m(Keba.ChannelId.CABLE_STATE, new UnsignedDoublewordElement(1004))),
				new FC3ReadRegistersTask(1006, Priority.LOW, //
						m(KebaModbus.ChannelId.ERROR_CODE, new UnsignedDoublewordElement(1006))),
				new FC3ReadRegistersTask(1008, Priority.LOW, //
						m(phaseRotated.channelCurrentL1(), new UnsignedDoublewordElement(1008))),
				new FC3ReadRegistersTask(1010, Priority.LOW, //
						m(phaseRotated.channelCurrentL2(), new UnsignedDoublewordElement(1010))),
				new FC3ReadRegistersTask(1012, Priority.LOW, //
						m(phaseRotated.channelCurrentL3(), new UnsignedDoublewordElement(1012))),
				new FC3ReadRegistersTask(1014, Priority.LOW, //
						m(KebaModbus.ChannelId.SERIAL_NUMBER, new UnsignedDoublewordElement(1014))),
				new FC3ReadRegistersTask(1016, Priority.LOW, //
						m(new UnsignedDoublewordElement(1016)).build().onUpdateCallback(value -> {
							var ptaf = ProductTypeAndFeatures.from(value);
							// TODO add Warning for PTAF_PRODUCT_FAMILY.KC_P30: KEBA P30 Modbus/TCP is not
							// supported
							setValue(this, KebaModbus.ChannelId.PTAF_PRODUCT_FAMILY, ptaf.productFamily());
							setValue(this, KebaModbus.ChannelId.PTAF_DEVICE_CURRENT, ptaf.deviceCurrent());
							setValue(this, KebaModbus.ChannelId.PTAF_CONNECTOR, ptaf.connector());
							setValue(this, KebaModbus.ChannelId.PTAF_PHASES, ptaf.phases());
							setValue(this, KebaModbus.ChannelId.PTAF_METERING, ptaf.metering());
							setValue(this, KebaModbus.ChannelId.PTAF_RFID, ptaf.rfid());
							setValue(this, KebaModbus.ChannelId.PTAF_BUTTON, ptaf.button());
						})),
				new FC3ReadRegistersTask(1018, Priority.LOW, //
						m(KebaModbus.ChannelId.FIRMWARE, new UnsignedDoublewordElement(1018))
								.onUpdateCallback((v) -> handleFirmwareVersion(this, v))),
				new FC3ReadRegistersTask(1020, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedDoublewordElement(1020),
								SCALE_FACTOR_MINUS_3) //
								.onUpdateCallback(power -> calculateActivePowerL1L2L3(this, power))),
				new FC3ReadRegistersTask(1036, Priority.LOW, //
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
								new UnsignedDoublewordElement(1036), this.kebaModbusUtils.energyScaleFactor)),
				new FC3ReadRegistersTask(1040, Priority.LOW, //
						m(phaseRotated.channelVoltageL1(), new UnsignedDoublewordElement(1040), SCALE_FACTOR_3)),
				new FC3ReadRegistersTask(1042, Priority.LOW, //
						m(phaseRotated.channelVoltageL2(), new UnsignedDoublewordElement(1042), SCALE_FACTOR_3)),
				new FC3ReadRegistersTask(1044, Priority.LOW, //
						m(phaseRotated.channelVoltageL3(), new UnsignedDoublewordElement(1044), SCALE_FACTOR_3)),
				new FC3ReadRegistersTask(1046, Priority.LOW, //
						m(Keba.ChannelId.POWER_FACTOR, new UnsignedDoublewordElement(1046), SCALE_FACTOR_MINUS_1)),
				new FC3ReadRegistersTask(1100, Priority.LOW, //
						m(KebaModbus.ChannelId.MAX_CHARGING_CURRENT, new UnsignedDoublewordElement(1100))),
				// todo: read Register 1500 RFID once solution is found
				// this register is can not always be read with keba firmware 1.1.9 or less
				// there is currently no way of knowing when it can be read
				new FC3ReadRegistersTask(1502, Priority.LOW, //
						m(EvseKeba.ChannelId.ENERGY_SESSION, //
								new UnsignedDoublewordElement(1502), this.kebaModbusUtils.energyScaleFactor)),
				new FC3ReadRegistersTask(1550, Priority.LOW, //
						m(Keba.ChannelId.PHASE_SWITCH_SOURCE, new UnsignedDoublewordElement(1550))),
				new FC3ReadRegistersTask(1552, Priority.LOW, //
						m(Keba.ChannelId.PHASE_SWITCH_STATE, new UnsignedDoublewordElement(1552))),
				new FC3ReadRegistersTask(1600, Priority.LOW, //
						m(KebaModbus.ChannelId.FAILSAFE_CURRENT_SETTING, new UnsignedDoublewordElement(1600))),
				new FC3ReadRegistersTask(1602, Priority.LOW, //
						m(KebaModbus.ChannelId.FAILSAFE_TIMEOUT_SETTING, new UnsignedDoublewordElement(1602))));

		if (!this.config.readOnly()) {
			modbusProtocol.addTasks(//
					new FC6WriteRegisterTask(5004,
							m(Keba.ChannelId.SET_CHARGING_CURRENT, new UnsignedWordElement(5004))),
					new FC6WriteRegisterTask(5010, // TODO Scalefactor for Unit: 10 Wh
							m(EvseKeba.ChannelId.SET_ENERGY_LIMIT, new UnsignedWordElement(5010))),
					new FC6WriteRegisterTask(5012, m(Keba.ChannelId.SET_UNLOCK_PLUG, new UnsignedWordElement(5012))),
					new FC6WriteRegisterTask(5014, m(Keba.ChannelId.SET_ENABLE, new UnsignedWordElement(5014))),
					new FC6WriteRegisterTask(5050,
							m(Keba.ChannelId.SET_PHASE_SWITCH_SOURCE, new UnsignedWordElement(5050))),
					new FC6WriteRegisterTask(5052,
							m(Keba.ChannelId.SET_PHASE_SWITCH_STATE, new UnsignedWordElement(5052))));
		}
		return modbusProtocol;
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		return this.evseKebaUtils.getChargePointAbilities(this.config);
	}

	@Override
	public String debugLog() {
		return this.kebaUtils.debugLog();
	}

	@Override
	public void apply(ChargePointActions actions) {
		this.evseKebaUtils.applyChargePointActions(this.config, actions);
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
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
			this.kebaUtils.onBeforeProcessImage();
			this.evseKebaUtils.onBeforeProcessImage();
		}
		}
	}

	@Override
	public PhaseSwitchSource getRequiredPhaseSwitchSource() {
		return PhaseSwitchSource.VIA_MODBUS;
	}

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
	}
}
