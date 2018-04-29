package io.openems.edge.ess.fenecon.openemsv1;

import java.util.Arrays;
import java.util.stream.Stream;

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

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Priority;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.LongWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.component.OpenemsComponent;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Ess.Fenecon.OpenemsV1", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class OpenemsV1 extends AbstractOpenemsModbusComponent implements OpenemsComponent {

	private final static int UNIT_ID = 1;

	@Reference
	protected ConfigurationAdmin cm;

	public OpenemsV1() {
		Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateChannel(this, channelId);
					}
					return null;
				}), Arrays.stream(OpenemsV1.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SET_MIN_SOC:
						return new LongWriteChannel(this, channelId);
					case VERSION_MAJOR:
						return new LongReadChannel(this, channelId);
					}
					return null;
				}) //
		).flatMap(channel -> channel).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	protected void setModbus(BridgeModbusTcp modbus) {
		super.setModbus(modbus);
	}

	protected void unsetModbus(BridgeModbusTcp modbus) {
		super.unsetModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		VERSION_MAJOR(new Doc()), //
		SET_MIN_SOC(new Doc());

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		return new ModbusProtocol(unitId, //
				new FC3ReadRegistersTask(0, Priority.LOW, //
						m(OpenemsV1.ChannelId.VERSION_MAJOR, new UnsignedDoublewordElement(0))),
				new FC16WriteRegistersTask(2, //
						m(OpenemsV1.ChannelId.SET_MIN_SOC, new UnsignedDoublewordElement(2).debug())));
	}
}
