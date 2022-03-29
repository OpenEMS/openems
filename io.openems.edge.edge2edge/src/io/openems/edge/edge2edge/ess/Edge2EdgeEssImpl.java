package io.openems.edge.edge2edge.ess;

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

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Edge2Edge.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class Edge2EdgeEssImpl extends AbstractOpenemsModbusComponent
		implements ManagedSymmetricEss, SymmetricEss, Edge2EdgeEss, ModbusComponent, OpenemsComponent {

	// TODO this should be read dynamically using the RemoteComponentId-config
	// property
	private final static int BASE_ADDRESS = 500;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected Power power;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

//	private Config config = null;

	public Edge2EdgeEssImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				Edge2EdgeEss.ChannelId.values() //
		);
		this._setMaxApparentPower(Integer.MAX_VALUE); // TODO read proper limits from Modbus
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
//		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				/*
				 * Block 'OpenemsComponent'
				 */
				new FC3ReadRegistersTask(BASE_ADDRESS + 22, Priority.LOW, //
						m(new UnsignedWordElement(BASE_ADDRESS + 22)).build().onUpdateCallback(value -> {
							if (value == null) {
								return;
							}
							this.channel(Edge2EdgeEss.ChannelId.REMOTE_FAULT)
									.setNextValue(value == Level.FAULT.getValue());
							this.channel(Edge2EdgeEss.ChannelId.REMOTE_WARNING)
									.setNextValue(value == Level.WARNING.getValue());
							this.channel(Edge2EdgeEss.ChannelId.REMOTE_INFO)
									.setNextValue(value == Level.INFO.getValue());
						})), //
				/*
				 * Block 'SymmetricEss'
				 */
				new FC3ReadRegistersTask(BASE_ADDRESS + 102, Priority.LOW, //
						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(BASE_ADDRESS + 102)), //
						m(SymmetricEss.ChannelId.GRID_MODE, new UnsignedWordElement(BASE_ADDRESS + 103)), //
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(BASE_ADDRESS + 104)), //
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(BASE_ADDRESS + 106)) //
				), //
				/*
				 * Block 'ManagedSymmetricEss'
				 */
				new FC3ReadRegistersTask(BASE_ADDRESS + 202, Priority.LOW, //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER,
								new FloatDoublewordElement(BASE_ADDRESS + 202)), //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER,
								new FloatDoublewordElement(BASE_ADDRESS + 204)) //
				), //
				new FC16WriteRegistersTask(BASE_ADDRESS + 206, //
						m(Edge2EdgeEss.ChannelId.SET_ACTIVE_POWER, new FloatDoublewordElement(BASE_ADDRESS + 206)), //
						m(Edge2EdgeEss.ChannelId.SET_REACTIVE_POWER, new FloatDoublewordElement(BASE_ADDRESS + 208)) //
				) //
		);
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Allowed:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit() + ";"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asString() //
				+ "|" + this.getGridModeChannel().value().asOptionString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		IntegerWriteChannel setActivePowerChannel = this.channel(Edge2EdgeEss.ChannelId.SET_ACTIVE_POWER);
		setActivePowerChannel.setNextWriteValue(activePower);
		IntegerWriteChannel setReactivePowerChannel = this.channel(Edge2EdgeEss.ChannelId.SET_REACTIVE_POWER);
		setReactivePowerChannel.setNextWriteValue(reactivePower);
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}
}
