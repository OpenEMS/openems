package io.openems.edge.goodwe.ess;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.goodwe.common.AbstractGoodWe;
import io.openems.edge.goodwe.common.ApplyPowerHandler;
import io.openems.edge.goodwe.common.GoodWe;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GoodWe.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
) //
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class GoodWeEssImpl extends AbstractGoodWe implements GoodWeEss, GoodWe, HybridEss, ManagedSymmetricEss,
		SymmetricEss, ModbusComponent, OpenemsComponent, TimedataProvider, EventHandler, ModbusSlave {

	private final AllowedChargeDischargeHandler allowedChargeDischargeHandler = new AllowedChargeDischargeHandler(this);
	private final ApplyPowerHandler applyPowerHandler = new ApplyPowerHandler();

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Power power;

	@Reference
	private Sum sum;

	@Reference
	private ComponentManager componentManager;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;

	public GoodWeEssImpl() throws OpenemsNamedException {
		super(//
				SymmetricEss.ChannelId.ACTIVE_POWER, //
				SymmetricEss.ChannelId.REACTIVE_POWER, //
				HybridEss.ChannelId.DC_DISCHARGE_POWER, //
				SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY, //
				SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, //
				HybridEss.ChannelId.DC_CHARGE_ENERGY, //
				HybridEss.ChannelId.DC_DISCHARGE_ENERGY, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				GoodWe.ChannelId.values(), //
				GoodWeEss.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this._setCapacity(this.config.capacity());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		this.calculateMaxAcPower(this.getMaxApparentPower().orElse(0));

		// Apply Power Set-Point
		this.applyPowerHandler.apply(this, activePower, this.config.controlMode(), this.sum.getGridActivePower(),
				this.getActivePower(), this.getMaxAcImport(), this.getMaxAcExport(), this.power.isPidEnabled());
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|" + this.getGridModeChannel().value().asOptionString()//
				+ "|Allowed:" + this.getAllowedChargePower().asStringWithoutUnit() + ";"
				+ this.getAllowedDischargePower().asString();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updatePowerAndEnergyChannels();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.allowedChargeDischargeHandler.accept(this.componentManager);
			break;
		}
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public Integer getSurplusPower() {
		// TODO logic is insufficient
		if (this.getSoc().orElse(0) < 99) {
			return null;
		}
		var productionPower = this.calculatePvProduction();
		if (productionPower == null || productionPower < 100) {
			return null;
		}
		return productionPower + 200 /* discharge more than PV production to avoid PV curtail */;
	}

	@Override
	public boolean isManaged() {
		return !this.config.controlMode().equals(ControlMode.INTERNAL);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode), //
				HybridEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(GoodWeEss.class, accessMode, 100) //
						.build());
	}
}
