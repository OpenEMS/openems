package io.openems.edge.edge2edge.ess;

import java.util.function.Consumer;

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

import com.google.common.collect.Lists;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusRecord;
import io.openems.edge.common.modbusslave.ModbusRecordChannel;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.edge2edge.common.AbstractEdge2Edge;
import io.openems.edge.edge2edge.common.Edge2Edge;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Edge2Edge.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class Edge2EdgeEssImpl extends AbstractEdge2Edge implements ManagedSymmetricEss, AsymmetricEss, SymmetricEss,
		Edge2EdgeEss, Edge2Edge, ModbusComponent, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Power power;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public Edge2EdgeEssImpl() throws OpenemsException {
		super(//
				Lists.newArrayList(//
						OpenemsComponent::getModbusSlaveNatureTable, //
						SymmetricEss::getModbusSlaveNatureTable, //
						AsymmetricEss::getModbusSlaveNatureTable, //
						ManagedSymmetricEss::getModbusSlaveNatureTable, //
						StartStoppable::getModbusSlaveNatureTable //
				), //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Edge2Edge.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				Edge2EdgeEss.ChannelId.values() //
		);
		this._setMaxApparentPower(Integer.MAX_VALUE); // has no effect, as long as AllowedCharge/DischargePower are null
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id(), config.remoteComponentId(), config.remoteAccessMode())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected Consumer<Object> getOnUpdateCallback(ModbusSlaveNatureTable modbusSlaveNatureTable, ModbusRecord record) {
		if (modbusSlaveNatureTable.getNatureClass() == ManagedSymmetricEss.class) {
			switch (record.getOffset()) {
			case 0: // "Minimum Power Set-Point"
				return (value) -> this._setAllowedChargePower(TypeUtils.getAsType(OpenemsType.INTEGER, value));

			case 2: // "Maximum Power Set-Point"
				return (value) -> this._setAllowedDischargePower(TypeUtils.getAsType(OpenemsType.INTEGER, value));
			}
		}
		return null;
	}

	@Override
	protected io.openems.edge.common.channel.ChannelId getWriteChannelId(ModbusSlaveNatureTable modbusSlaveNatureTable,
			ModbusRecord record) {
		if (record instanceof ModbusRecordChannel) {
			var c = ((ModbusRecordChannel) record).getChannelId();
			if (c == ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS) {
				return Edge2EdgeEss.ChannelId.REMOTE_SET_ACTIVE_POWER_EQUALS;

			} else if (c == ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_EQUALS) {
				return Edge2EdgeEss.ChannelId.REMOTE_SET_REACTIVE_POWER_EQUALS;
			}
		}
		return null;
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Allowed:" + this.getAllowedChargePower().asStringWithoutUnit() + ";" //
				+ this.getAllowedDischargePower().asString() //
				+ "|" + this.getGridModeChannel().value().asOptionString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		this.setRemoteActivePowerEquals((float) activePower);
		this.setRemoteReactivePowerEquals((float) reactivePower);
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

}
