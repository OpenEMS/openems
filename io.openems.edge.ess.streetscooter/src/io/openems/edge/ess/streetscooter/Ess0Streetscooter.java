package io.openems.edge.ess.streetscooter;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config0.class, factory = true)
@Component(name = "Ess.Streetscooter.0", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC
		+ "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS)
public class Ess0Streetscooter extends AbstractEssStreetscooter
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, ModbusSlave {

	private static final int ICU_0_SET_POWER_ADDRESS = 4000;
	private static final int ICU_0_ENABLED_ADDRESS = 4000;
	private static final int BATTERY_0_ADDRESS_OFFSET = 0;
	private static final int INVERTER_0_ADDRESS_OFFSET = 0;

	private static final int BATTERY_0_OVERLOAD_ADDRESS = 0001;
	private static final int BATTERY_0_CONNECTED_ADDRESS = 0000;
	private static final int INVERTER_0_CONNECTED_ADDRESS = 2000;
	private static final int ICU_0_RUNSTATE_ADDRESS = 4000;

	@Reference
	private Power power;

	@Reference
	private ConfigurationAdmin cm;

	public Ess0Streetscooter() {
		super();
	}

	@Activate
	protected void activate(ComponentContext context, Config0 config0) {
		super.activate(context, config0.id(), config0.alias(), config0.enabled(), config0.readonly(), UNIT_ID, this.cm,
				"Modbus", config0.modbus_id());
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Override
	protected int getIcuSetPowerAddress() {
		return ICU_0_SET_POWER_ADDRESS;
	}

	@Override
	protected int getIcuEnabledAddress() {
		return ICU_0_ENABLED_ADDRESS;
	}

	@Override
	protected int getAdressOffsetForBattery() {
		return BATTERY_0_ADDRESS_OFFSET;
	}

	@Override
	protected int getAdressOffsetForInverter() {
		return INVERTER_0_ADDRESS_OFFSET;
	}

	@Override
	protected int getBatteryOverloadAddress() {
		return BATTERY_0_OVERLOAD_ADDRESS;
	}

	@Override
	protected int getBatteryConnectedAddress() {
		return BATTERY_0_CONNECTED_ADDRESS;
	}

	@Override
	protected int getInverterConnectedAddress() {
		return INVERTER_0_CONNECTED_ADDRESS;
	}

	@Override
	protected int getIcuRunstateAddress() {
		return ICU_0_RUNSTATE_ADDRESS;
	}

	@Override
	public Power getPower() {
		return this.power;
	}

}
