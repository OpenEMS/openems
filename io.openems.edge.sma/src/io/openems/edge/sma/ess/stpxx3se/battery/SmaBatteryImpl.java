package io.openems.edge.sma.ess.stpxx3se.battery;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_3;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.SMA.StpSe.Battery", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SmaBatteryImpl extends AbstractOpenemsModbusComponent
		implements Battery, SmaBattery, ModbusComponent, OpenemsComponent {

	// see https://files.sma.de/downloads/HS-BM-10-DS-en-14.pdf
	// for different battery system voltages
	private static final int DEFAULT_CHARGE_MAX_VOLTAGE = 103;
	private static final int DEFAULT_DISCHARGE_MIN_VOLTAGE = 95;

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public SmaBatteryImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				SmaBattery.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.limitMinMaxVoltages();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(30843, Priority.LOW, //
						m(Battery.ChannelId.CURRENT, new SignedDoublewordElement(30843), //
								SCALE_FACTOR_MINUS_3),
						m(Battery.ChannelId.SOC, new UnsignedDoublewordElement(30845)),
						new DummyRegisterElement(30847, 30850), //
						m(Battery.ChannelId.VOLTAGE, new UnsignedDoublewordElement(30851), SCALE_FACTOR_MINUS_2), //
						new DummyRegisterElement(30853, 30954), //
						m(SmaBattery.ChannelId.BAT_STATUS, new UnsignedDoublewordElement(30955)) //
				), //

				new FC3ReadRegistersTask(31391, Priority.HIGH, //
						m(new UnsignedDoublewordElement(31391)).build() //
								.onUpdateCallback(this::setStatus), //
						m(SmaBattery.ChannelId.CUR_BAT_CHA, new UnsignedDoublewordElement(31393)), //
						m(SmaBattery.ChannelId.CUR_BAT_DSCH, new UnsignedDoublewordElement(31395)), //
						m(SmaBattery.ChannelId.BAT_CHRG, new UnsignedQuadruplewordElement(31397)), //
						m(SmaBattery.ChannelId.BAT_DSCH, new UnsignedQuadruplewordElement(31401)) //
				), //

				new FC3ReadRegistersTask(32251, Priority.LOW, //
						// We read only 0xFFFF here...
						// m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, new
						// UnsignedDoublewordElement(32239)), //
						// new DummyRegisterElement(32241, 32244), //
						// We read only 0xFFFF here...
						// m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, new
						// UnsignedDoublewordElement(32245)), //
						// new DummyRegisterElement(32247, 32250), //
						m(Battery.ChannelId.CHARGE_MAX_CURRENT, new UnsignedDoublewordElement(32251), //
								SCALE_FACTOR_MINUS_3), //
						new DummyRegisterElement(32253, 32256), //
						m(Battery.ChannelId.DISCHARGE_MAX_CURRENT, new UnsignedDoublewordElement(32257), //
								SCALE_FACTOR_MINUS_3) //
				), //

				new FC3ReadRegistersTask(34661, Priority.HIGH,
						m(SmaBattery.ChannelId.ACT_BAT_CHRG, new UnsignedQuadruplewordElement(34661)), //
						m(SmaBattery.ChannelId.ACT_BAT_DSCH, new UnsignedQuadruplewordElement(34665)) //
				), //

				new FC3ReadRegistersTask(40187, Priority.LOW, //
						m(Battery.ChannelId.CAPACITY, new UnsignedDoublewordElement(40187))), //

				new FC16WriteRegistersTask(40149, //
						m(SmaBattery.ChannelId.SET_ACTIVE_POWER, new SignedDoublewordElement(40149)), //
						m(SmaBattery.ChannelId.SET_CONTROL_MODE, new UnsignedDoublewordElement(40151)), //
						// //TODO cle, 2024.07.25, laut Doku ist das Register 30827. Es gibt kein 40153
						m(SmaBattery.ChannelId.SET_REACTIVE_POWER, new SignedDoublewordElement(40153))) //

		);

		// TODO evaulate status
		// Steuerung über ext. Modbus verfügbar 31061 INFO State daraus machen, wenn
		// nicht aktiv
	}

	private void limitMinMaxVoltages() {
		this.getCapacityChannel().onChange((o, n) -> {
			if (n == null || !n.isDefined()) {
				return;
			}
			// detect size of SMA Home Storage
			var factor = Math.min(n.get() / 3500 + 1, 5);
			this._setChargeMaxVoltage(factor * DEFAULT_CHARGE_MAX_VOLTAGE);
			this._setDischargeMinVoltage(factor * DEFAULT_DISCHARGE_MIN_VOLTAGE);

		});
	}

	private void setStatus(Long value) {
		this._setBatteryError(value == 35L);
		this._setBatteryWarning(value == 455L);
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		this._setStartStop(value);
	}

}
