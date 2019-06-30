package io.openems.edge.pvinverter.kaco.blueplanet;

import java.util.concurrent.CompletableFuture;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.CheckedConsumer;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.SymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PV-Inverter.KACO.blueplanet", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class KacoBlueplanet extends AbstractOpenemsModbusComponent
		implements SymmetricPvInverter, SymmetricMeter, OpenemsComponent {

	private final static int UNIT_ID = 1;

	private final Logger log = LoggerFactory.getLogger(KacoBlueplanet.class);

	@Reference
	protected ConfigurationAdmin cm;

	private final ModbusProtocol modbusProtocol;

	public KacoBlueplanet() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				SymmetricPvInverter.ChannelId.values(), //
				ChannelId.values() //
		);
		this.getActivePowerLimit().onSetNextWrite(this.setPvLimit);

		this.modbusProtocol = new ModbusProtocol(this);
		this.modbusProtocol.addTasks(//
				new FC3ReadRegistersTask(40144, Priority.HIGH, //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(40144))), //

				new FC3ReadRegistersTask(40295, Priority.LOW, //
						m(ChannelId.W_MAX_LIM_PCT, new UnsignedWordElement(40295)), //
						new DummyRegisterElement(40296, 40298),
						m(ChannelId.W_MAX_LIM_ENA, new UnsignedWordElement(40299))), //
				new FC6WriteRegisterTask(40295, //
						m(ChannelId.W_MAX_LIM_PCT, new UnsignedWordElement(40295))), //
				new FC6WriteRegisterTask(40299, //
						m(ChannelId.W_MAX_LIM_ENA, new UnsignedWordElement(40299))) //
		);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id());

		this.isSunSpec().thenAccept(isSunSpec -> {
			System.out.println("Is SunSpec? " + isSunSpec);

			// TODO: finde start-adressen von allen SunSpec Blöcken und speichere sie in
			// einer BiMap<Integer, Integer> = SunSpec-ID <-> Start-Adresse

		});
	}

	private final CheckedConsumer<Integer> setPvLimit = (power) -> {
		int pLimitPerc = (int) (power / 15_000.0 * 100.0 /* percent */ * 10.0 /* scale factor */);

		// keep percentage in range [0, 100]
		if (pLimitPerc > 1000) {
			pLimitPerc = 1000; 
		}
		if (pLimitPerc < 1) {
			pLimitPerc = 1;
		}

		IntegerWriteChannel wMaxLimPctChannel = this.channel(ChannelId.W_MAX_LIM_PCT);
		this.logInfo(this.log, "Apply new limit: " + pLimitPerc / 10. + " %");
		wMaxLimPctChannel.setNextWriteValue(pLimitPerc);

		// Is limitation enabled?
		IntegerWriteChannel wMaxLimEnaChannel = this.channel(ChannelId.W_MAX_LIM_ENA);
		if (wMaxLimEnaChannel.value().orElse(0) == 0) {
			this.logInfo(this.log, "Enabling W MAX LIM");
			wMaxLimEnaChannel.setNextWriteValue(1);
		}
	};

	/**
	 * Validates that this device complies to SunSpec specification.
	 * 
	 * <p>
	 * Tests if first registers are 0x53756e53 ("SunS").
	 * 
	 * @return a future true if it is SunSpec; otherwise false
	 */
	private CompletableFuture<Boolean> isSunSpec() {
		final CompletableFuture<Boolean> result = new CompletableFuture<Boolean>();
		final AbstractModbusElement<?> element = new UnsignedDoublewordElement(40000);
		final Task task = new FC3ReadRegistersTask(40000, Priority.HIGH, element);
		element.onUpdateCallback(value -> {
			if (value == null) {
				// try again
				return;
			}
			// do not try again
			this.modbusProtocol.removeTask(task);
			if ((Long) value == 0x53756e53) {
				result.complete(true);
			} else {
				result.complete(false);
			}
		});
		this.modbusProtocol.addTask(task);
		return result;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return this.modbusProtocol;
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.PRODUCTION;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		W_MAX_LIM_PCT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.PERCENT)), //
		W_MAX_LIM_ENA(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)) //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}

	}
}
