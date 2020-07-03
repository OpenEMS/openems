package io.openems.edge.fenecon.dess.ess;

import java.util.OptionalDouble;

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

import com.google.common.collect.EvictingQueue;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.fenecon.dess.FeneconDessConstants;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Fenecon.Dess.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class FeneconDessEss extends AbstractOpenemsModbusComponent
		implements AsymmetricEss, SymmetricEss, OpenemsComponent {

	private static final int MAX_APPARENT_POWER = 9_000; // [VA]
	private static final int CAPACITY = 10_000; // [Wh]

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public FeneconDessEss() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ChannelId.values() //
		);

		this._setMaxApparentPower(MAX_APPARENT_POWER);
		this._setCapacity(CAPACITY);

		// automatically calculate Active/ReactivePower from L1/L2/L3
		AsymmetricEss.initializePowerSumChannels(this);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), FeneconDessConstants.UNIT_ID, this.cm,
				"Modbus", config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SYSTEM_STATE(Doc.of(SystemState.values())), //
		ORIGINAL_SOC(new IntegerDoc()//
				.onInit(channel -> { //
					final EvictingQueue<Integer> lastSocValues = EvictingQueue.create(100);
					((IntegerReadChannel) channel).onChange((oldValue, newValue) -> {
						Integer originalSocValue = newValue.get();
						Integer correctedSocValue = null;
						if (originalSocValue != null) {
							lastSocValues.add(originalSocValue);
							OptionalDouble averageSoc = lastSocValues.stream().mapToInt(Integer::intValue).average();
							if (averageSoc.isPresent()) {
								correctedSocValue = (int) averageSoc.getAsDouble();
							}
						}
						SymmetricEss parent = (SymmetricEss) channel.getComponent();
						parent._setSoc(correctedSocValue);
					});
				})),
		BSMU_WORK_STATE(Doc.of(BsmuWorkState.values()) //
				.onInit(channel -> { //
					// on each update set Grid-Mode channel
					((Channel<Integer>) channel).onChange((oldValue, newValue) -> {
						BsmuWorkState state = newValue.asEnum();
						SymmetricEss parent = (SymmetricEss) channel.getComponent();
						switch (state) {
						case ON_GRID:
							parent._setGridMode(GridMode.ON_GRID);
							break;
						case OFF_GRID:
							parent._setGridMode(GridMode.OFF_GRID);
							break;
						case FAULT:
						case UNDEFINED:
						case BEING_ON_GRID:
						case BEING_PRE_CHARGE:
						case BEING_STOP:
						case DEBUG:
						case INIT:
						case LOW_CONSUMPTION:
						case PRE_CHARGE:
							parent._setGridMode(GridMode.UNDEFINED);
							break;
						}
					});
				})), //
		STACK_CHARGE_STATE(Doc.of(StackChargeState.values())); //

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
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(10000, Priority.LOW, //
						m(FeneconDessEss.ChannelId.SYSTEM_STATE, new UnsignedWordElement(10000)), //
						m(FeneconDessEss.ChannelId.BSMU_WORK_STATE, new UnsignedWordElement(10001)), //
						m(FeneconDessEss.ChannelId.STACK_CHARGE_STATE, new UnsignedWordElement(10002))), //
				new FC3ReadRegistersTask(10143, Priority.LOW, //
						// m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(10143)), //
						m(FeneconDessEss.ChannelId.ORIGINAL_SOC, new UnsignedWordElement(10143)), //
						new DummyRegisterElement(10144, 10150),
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY,
								new UnsignedDoublewordElement(10151).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3), //
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(10153).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3)), //
				new FC3ReadRegistersTask(11133, Priority.HIGH, //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L1, new UnsignedWordElement(11133), DELTA_10000), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L1, new UnsignedWordElement(11134), DELTA_10000)), //
				new FC3ReadRegistersTask(11163, Priority.HIGH, //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L2, new UnsignedWordElement(11163), DELTA_10000), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L2, new UnsignedWordElement(11164), DELTA_10000)), //
				new FC3ReadRegistersTask(11193, Priority.HIGH, //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L3, new UnsignedWordElement(11193), DELTA_10000), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L3, new UnsignedWordElement(11194), DELTA_10000)) //
		); //
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString(); //
	}

	private static final ElementToChannelConverter DELTA_10000 = new ElementToChannelConverter(//
			// element -> channel
			value -> {
				if (value == null) {
					return null;
				}
				int intValue = (Integer) value;
				if (intValue == 0) {
					return 0; // ignore '0'
				}
				return intValue - 10_000; // apply delta of 10_000
			}, //

			// channel -> element
			value -> value);
}