package io.openems.edge.goodwe.stsbox;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.common.utils.StringUtils.isNullOrEmpty;
import static io.openems.edge.common.channel.ChannelUtils.setWriteValueIfNotRead;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.goodwe.common.enums.GensetInstalledStatus;
import io.openems.edge.goodwe.common.enums.MultiplexingMode;
import io.openems.edge.goodwe.genset.GoodWeStsBoxGensetMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GoodWe.StsBox", //
		immediate = true, //
		configurationPolicy = REQUIRE) //
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class GoodWeStsBoxImpl extends AbstractOpenemsModbusComponent
		implements OpenemsComponent, GoodWeStsBox, ModbusComponent {

	private final Logger log = LoggerFactory.getLogger(GoodWeStsBoxImpl.class);

	private Config config;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = STATIC, cardinality = OPTIONAL, policyOption = GREEDY)
	private volatile GoodWeStsBoxGensetMeter genset;

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public GoodWeStsBoxImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				GoodWeStsBox.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.applyConfig(context, config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.applyConfig(context, config);
	}

	@Deactivate
	private void deactivate(ComponentContext context, Config config) {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //

				new FC3ReadRegistersTask(33288, Priority.LOW, //
						m(GoodWeStsBox.ChannelId.SERIAL_NUMBER, new StringWordElement(33288, 8))),

				new FC3ReadRegistersTask(35039, Priority.LOW, //
						m(GoodWeStsBox.ChannelId.VERSION, new UnsignedWordElement(35039)), //
						m(GoodWeStsBox.ChannelId.SUB_VERSION, new UnsignedWordElement(35040))),

				new FC3ReadRegistersTask(45300, Priority.LOW,
						m(GoodWeStsBox.ChannelId.PORT_MUTLIPLEXING_MODE, new UnsignedWordElement(45300))),

				new FC3ReadRegistersTask(45593, Priority.LOW,
						m(GoodWeStsBox.ChannelId.GENSET_UPPER_VOLTAGE_LIMIT, new UnsignedWordElement(45593)),
						m(GoodWeStsBox.ChannelId.GENSET_LOWER_VOLTAGE_LIMIT, new UnsignedWordElement(45594)),
						m(GoodWeStsBox.ChannelId.GENSET_UPPER_FREQUENCY_LIMIT, new UnsignedWordElement(45595),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodWeStsBox.ChannelId.GENSET_LOWER_FREQUENCY_LIMIT, new UnsignedWordElement(45596),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodWeStsBox.ChannelId.DELAY_BEFORE_LOAD, new UnsignedWordElement(45597))),

				new FC3ReadRegistersTask(47745, Priority.LOW,
						m(GoodWeStsBox.ChannelId.GENSET_START_MODE_SELECTION, new UnsignedWordElement(47745)),
						m(GoodWeStsBox.ChannelId.ONE_CLICK_ENABLE, new UnsignedWordElement(47746)),
						m(GoodWeStsBox.ChannelId.GENSET_CHARGE_LIMIT, new UnsignedWordElement(47747),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(47748, 47755),
						m(GoodWeStsBox.ChannelId.OPEN_VOLTAGE, new UnsignedWordElement(47756)),
						m(GoodWeStsBox.ChannelId.CLOSED_VOLTAGE, new UnsignedWordElement(47757)),
						m(GoodWeStsBox.ChannelId.GENSET_RUN_TIME, new UnsignedWordElement(47758)),
						m(GoodWeStsBox.ChannelId.GENSET_RATED_POWER, new UnsignedWordElement(47759),
								ElementToChannelConverter.SCALE_FACTOR_1)),

				new FC16WriteRegistersTask(45300, //
						m(GoodWeStsBox.ChannelId.PORT_MUTLIPLEXING_MODE, new UnsignedWordElement(45300))),

				new FC16WriteRegistersTask(45593,
						m(GoodWeStsBox.ChannelId.GENSET_UPPER_VOLTAGE_LIMIT, new UnsignedWordElement(45593)),
						m(GoodWeStsBox.ChannelId.GENSET_LOWER_VOLTAGE_LIMIT, new UnsignedWordElement(45594)),
						m(GoodWeStsBox.ChannelId.GENSET_UPPER_FREQUENCY_LIMIT, new UnsignedWordElement(45595),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodWeStsBox.ChannelId.GENSET_LOWER_FREQUENCY_LIMIT, new UnsignedWordElement(45596),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)),

				new FC16WriteRegistersTask(45597, //
						m(GoodWeStsBox.ChannelId.DELAY_BEFORE_LOAD, new UnsignedWordElement(45597))),

				new FC16WriteRegistersTask(47745,
						m(GoodWeStsBox.ChannelId.GENSET_START_MODE_SELECTION, new UnsignedWordElement(47745))),

				new FC16WriteRegistersTask(47746,
						m(GoodWeStsBox.ChannelId.ONE_CLICK_ENABLE, new UnsignedWordElement(47746)),
						m(GoodWeStsBox.ChannelId.GENSET_CHARGE_LIMIT, new UnsignedWordElement(47747),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(47748, 47755),
						m(GoodWeStsBox.ChannelId.OPEN_VOLTAGE, new UnsignedWordElement(47756)),
						m(GoodWeStsBox.ChannelId.CLOSED_VOLTAGE, new UnsignedWordElement(47757)),
						m(GoodWeStsBox.ChannelId.GENSET_RUN_TIME, new UnsignedWordElement(47758)),
						m(GoodWeStsBox.ChannelId.GENSET_RATED_POWER, new UnsignedWordElement(47759),
								ElementToChannelConverter.SCALE_FACTOR_1)) //
		);
	}

	private void applyConfig(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;

		// updateReferenceFilter for genset
		if (isNullOrEmpty(config.genset_id())) {
			OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "genset", "(false=true)");
		} else {
			OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "genset", config.genset_id());
		}

		// validate charge SoC Start/End
		if (this.config.chargeSocStart() >= this.config.chargeSocEnd()) {
			this.logInfo(this.log, "Make sure Charge SOC Start should be lower than Charge SOC End");
		}

		// configureChannelsFromConfigValues
		if (this.config.portMultiplexingMode() != MultiplexingMode.UNDEFINED) {
			setWriteValueIfNotRead(this.channel(GoodWeStsBox.ChannelId.PORT_MUTLIPLEXING_MODE), //
					this.config.portMultiplexingMode());
		}

		final var gensetInstalled = this.genset == null //
				? GensetInstalledStatus.NOT_INSTALLED //
				: GensetInstalledStatus.INSTALLED;

		setWriteValueIfNotRead(this.channel(GoodWeStsBox.ChannelId.GENSET_START_MODE_SELECTION), //
				gensetInstalled);

		switch (this.config.portMultiplexingMode()) {
		case UNDEFINED, BACKUP, LARGE_LOAD //
			-> doNothing();
		case GENSET //
			-> this.configureChannelsForGensetMode();
		}
	}

	private void configureChannelsForGensetMode() throws OpenemsNamedException {
		setWriteValueIfNotRead(this.channel(GoodWeStsBox.ChannelId.GENSET_RATED_POWER), //
				this.config.ratedPower());
		setWriteValueIfNotRead(this.channel(GoodWeStsBox.ChannelId.DELAY_BEFORE_LOAD), //
				this.config.preheatingTime());
		setWriteValueIfNotRead(this.channel(GoodWeStsBox.ChannelId.GENSET_RUN_TIME), //
				Math.round(this.config.runtime() / 6.0F));
		setWriteValueIfNotRead(this.channel(GoodWeStsBox.ChannelId.ONE_CLICK_ENABLE), //
				this.config.enableCharge());
		setWriteValueIfNotRead(this.channel(GoodWeStsBox.ChannelId.GENSET_CHARGE_LIMIT), //
				this.config.maxPowerPercent());
		setWriteValueIfNotRead(this.channel(GoodWeStsBox.ChannelId.OPEN_VOLTAGE), //
				this.config.chargeSocStart());
		setWriteValueIfNotRead(this.channel(GoodWeStsBox.ChannelId.CLOSED_VOLTAGE), //
				this.config.chargeSocEnd());
		setWriteValueIfNotRead(this.channel(GoodWeStsBox.ChannelId.GENSET_LOWER_VOLTAGE_LIMIT), //
				this.config.voltageLowerLimit());
		setWriteValueIfNotRead(this.channel(GoodWeStsBox.ChannelId.GENSET_UPPER_VOLTAGE_LIMIT), //
				this.config.voltageUpperLimit());
		setWriteValueIfNotRead(this.channel(GoodWeStsBox.ChannelId.GENSET_UPPER_FREQUENCY_LIMIT), //
				this.config.frequencyUpperLimit());
		setWriteValueIfNotRead(this.channel(GoodWeStsBox.ChannelId.GENSET_LOWER_FREQUENCY_LIMIT), //
				this.config.frequencyLowerLimit());
	}
}
