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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config1.class, factory = true)
@Component(name = "Ess.Streetscooter.1", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC
		+ "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS)
public class Ess1Streetscooter extends AbstractEssStreetscooter
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, ModbusSlave {

	private static final int ICU_1_SET_POWER_ADDRESS = 4002;
	private static final int ICU_1_ENABLED_ADDRESS = 4001;

	private static final int BATTERY_1_ADDRESS_OFFSET = 1000;
	private static final int INVERTER_1_ADDRESS_OFFSET = 1000;

	private static final int BATTERY_1_OVERLOAD_ADDRESS = 1001;
	private static final int BATTERY_1_CONNECTED_ADDRESS = 1000;
	private static final int INVERTER_1_CONNECTED_ADDRESS = 13000;
	private static final int ICU_1_RUNSTATE_ADDRESS = 14001;

	@Reference
	private Power power;

	@Reference
	private ConfigurationAdmin cm;

	public Ess1Streetscooter() {
		super();
	}

	@Activate
	protected void activate(ComponentContext context, Config1 config1) throws OpenemsException {
		if (super.activate(context, config1.id(), config1.alias(), config1.enabled(), config1.readonly(), UNIT_ID,
				this.cm, "Modbus", config1.modbus_id())) {
			return;
		}
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Override
	protected int getIcuSetPowerAddress() {
		return ICU_1_SET_POWER_ADDRESS;
	}

	@Override
	protected int getIcuEnabledAddress() {
		return ICU_1_ENABLED_ADDRESS;
	}

	@Override
	protected int getAddressOffsetForBattery() {
		return BATTERY_1_ADDRESS_OFFSET;
	}

	@Override
	protected int getAddressOffsetForInverter() {
		return INVERTER_1_ADDRESS_OFFSET;
	}

	@Override
	protected int getBatteryOverloadAddress() {
		return BATTERY_1_OVERLOAD_ADDRESS;
	}

	@Override
	protected int getBatteryConnectedAddress() {
		return BATTERY_1_CONNECTED_ADDRESS;
	}

	@Override
	protected int getInverterConnectedAddress() {
		return INVERTER_1_CONNECTED_ADDRESS;
	}

	@Override
	protected int getIcuRunstateAddress() {
		return ICU_1_RUNSTATE_ADDRESS;
	}

	@Override
	public Power getPower() {
		return this.power;
	}
}
