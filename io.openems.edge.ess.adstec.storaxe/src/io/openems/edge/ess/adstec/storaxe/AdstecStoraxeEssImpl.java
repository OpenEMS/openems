package io.openems.edge.ess.adstec.storaxe;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Adstec.StoraXe.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class AdstecStoraxeEssImpl extends AbstractOpenemsModbusComponent
		implements SymmetricEss, OpenemsComponent, ModbusSlave {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public AdstecStoraxeEssImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				AdstecStoraxeEss.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id());
		this._setCapacity(config.capacity());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(1, Priority.LOW, //
						m(SymmetricEss.ChannelId.GRID_MODE, new UnsignedWordElement(1), GRID_MODE_CONVERTER),
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new UnsignedWordElement(2), ElementToChannelConverter.SCALE_FACTOR_2),
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new UnsignedWordElement(3), ElementToChannelConverter.SCALE_FACTOR_2)),
				new FC3ReadRegistersTask(125, Priority.LOW, //
						m(SymmetricEss.ChannelId.MAX_APPARENT_POWER, new UnsignedWordElement(125), ElementToChannelConverter.SCALE_FACTOR_2),
						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(126), ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC3ReadRegistersTask(134, Priority.LOW, //
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, new UnsignedDoublewordElement(134), ElementToChannelConverter.SCALE_FACTOR_3),
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY, new UnsignedDoublewordElement(136), ElementToChannelConverter.SCALE_FACTOR_3),
						m(SymmetricEss.ChannelId.MIN_CELL_VOLTAGE, new UnsignedWordElement(138), ElementToChannelConverter.DIRECT_1_TO_1),
						m(SymmetricEss.ChannelId.MAX_CELL_VOLTAGE, new UnsignedWordElement(139), ElementToChannelConverter.DIRECT_1_TO_1)
				) //
		);
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString(); //
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				AsymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(AdstecStoraxeEss.class, accessMode, 300) //
						.build());
	}

	@Override
	public Value<Integer> getReactivePower() {
		// We need to override because the ess returns neg for capacitative, pos for inductive, but the channel expects
		// pos for from-battery, neg for to-battery.
		return new Value<Integer>(
				this.getReactivePowerChannel(),
				Math.abs(this.getReactivePowerChannel().value().get()) * Integer.signum(this.getActivePower().get()));
	}

	private static final ElementToChannelConverter GRID_MODE_CONVERTER = new ElementToChannelConverter(
			value -> {
				switch ((Integer)value) {
				case 1:
					return 2;
				case 2:
					return 1;
				default:
					return 0;
				}
			},
			value -> {
				switch ((Integer)value) {
				case 1:
					return 2;
				case 2:
					return 1;
				default:
					return 0;
				}
			});			
}