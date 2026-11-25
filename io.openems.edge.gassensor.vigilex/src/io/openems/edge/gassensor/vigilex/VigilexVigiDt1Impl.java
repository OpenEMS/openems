package io.openems.edge.gassensor.vigilex;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverters.SCALE_FACTOR_MINUS_2;
import static io.openems.edge.common.channel.ChannelUtils.setWriteValueIfNotRead;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GasSensor.Vigilex.VigiDt1", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
@GenerateTargetsFromReferences("Modbus")
public class VigilexVigiDt1Impl extends AbstractOpenemsModbusComponent
		implements VigilexVigiDt1, ModbusComponent, OpenemsComponent, EventHandler {

	private Config config;

	private static final int H2_SETPOINT_MIN = 500;
	private static final int H2_SETPOINT_MAX = 2500;
	private static final int VOC_SETPOINT_MIN = 150;
	private static final int VOC_SETPOINT_MAX = 500;

	private final Logger log = LoggerFactory.getLogger(VigilexVigiDt1Impl.class);

	@Override
	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))")
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public VigilexVigiDt1Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				VigilexVigiDt1.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
		this.validateConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		this.config = config;
		super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
		this.validateConfig(config);
	}

	private void validateConfig(Config config) {
		if (config.hydrogen_re1_low_setpoint() >= config.hydrogen_re1_high_setpoint()) {
			this.logWarn(this.log, "H2 RE1: low setpoint must be < high setpoint, skipping");
		}
		if (config.hydrogen_re2_low_setpoint() >= config.hydrogen_re2_high_setpoint()) {
			this.logWarn(this.log, "H2 RE2: low setpoint must be < high setpoint, skipping");
		}
		if (config.voc_re1_low_setpoint() >= config.voc_re1_high_setpoint()) {
			this.logWarn(this.log, "VOC RE1: low setpoint must be < high setpoint, skipping");
		}
		if (config.voc_re2_low_setpoint() >= config.voc_re2_high_setpoint()) {
			this.logWarn(this.log, "VOC RE2: low setpoint must be < high setpoint, skipping");
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			try {
				this.applySetpoints(this.config);
			} catch (OpenemsNamedException e) {
				this.logWarn(this.log, "Failed to apply setpoints: " + e.getMessage());
			}
		}
		}
	}

	private void applySetpoints(Config config) throws OpenemsNamedException {
		if (config.hydrogen_re1_low_setpoint() < config.hydrogen_re1_high_setpoint()) {
			this.applySetpoint(VigilexVigiDt1.ChannelId.HYDROGEN_RE1_LOW_SETPOINT, config.hydrogen_re1_low_setpoint(),
					H2_SETPOINT_MIN, H2_SETPOINT_MAX);
			this.applySetpoint(VigilexVigiDt1.ChannelId.HYDROGEN_RE1_HIGH_SETPOINT, config.hydrogen_re1_high_setpoint(),
					H2_SETPOINT_MIN, H2_SETPOINT_MAX);
		}
		if (config.hydrogen_re2_low_setpoint() < config.hydrogen_re2_high_setpoint()) {
			this.applySetpoint(VigilexVigiDt1.ChannelId.HYDROGEN_RE2_LOW_SETPOINT, config.hydrogen_re2_low_setpoint(),
					H2_SETPOINT_MIN, H2_SETPOINT_MAX);
			this.applySetpoint(VigilexVigiDt1.ChannelId.HYDROGEN_RE2_HIGH_SETPOINT, config.hydrogen_re2_high_setpoint(),
					H2_SETPOINT_MIN, H2_SETPOINT_MAX);
		}
		if (config.voc_re1_low_setpoint() < config.voc_re1_high_setpoint()) {
			this.applySetpoint(VigilexVigiDt1.ChannelId.VOC_RE1_LOW_SETPOINT, config.voc_re1_low_setpoint(),
					VOC_SETPOINT_MIN, VOC_SETPOINT_MAX);
			this.applySetpoint(VigilexVigiDt1.ChannelId.VOC_RE1_HIGH_SETPOINT, config.voc_re1_high_setpoint(),
					VOC_SETPOINT_MIN, VOC_SETPOINT_MAX);
		}
		if (config.voc_re2_low_setpoint() < config.voc_re2_high_setpoint()) {
			this.applySetpoint(VigilexVigiDt1.ChannelId.VOC_RE2_LOW_SETPOINT, config.voc_re2_low_setpoint(),
					VOC_SETPOINT_MIN, VOC_SETPOINT_MAX);
			this.applySetpoint(VigilexVigiDt1.ChannelId.VOC_RE2_HIGH_SETPOINT, config.voc_re2_high_setpoint(),
					VOC_SETPOINT_MIN, VOC_SETPOINT_MAX);
		}
	}

	private void applySetpoint(VigilexVigiDt1.ChannelId channelId, int value, int min, int max)
			throws OpenemsNamedException {
		if (value < min || value > max) {
			this.logWarn(this.log, "Setpoint [" + channelId.id() + "] value [" + value + "] out of range (" + min + "…"
					+ max + "). Skipping write.");
			return;
		}
		setWriteValueIfNotRead((IntegerWriteChannel) this.channel(channelId), value);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //

				new FC3ReadRegistersTask(213, Priority.LOW, //
						m(VigilexVigiDt1.ChannelId.RE1_STATE, new UnsignedWordElement(213)), //
						m(VigilexVigiDt1.ChannelId.RE2_STATE, new UnsignedWordElement(214)), //
						m(VigilexVigiDt1.ChannelId.RE1_DELAY, new UnsignedWordElement(215)), //
						m(VigilexVigiDt1.ChannelId.RE2_DELAY, new UnsignedWordElement(216))), //

				new FC3ReadRegistersTask(221, Priority.LOW, //
						m(VigilexVigiDt1.ChannelId.HYDROGEN_RE1_LOW_SETPOINT, new UnsignedWordElement(221)), //
						m(VigilexVigiDt1.ChannelId.HYDROGEN_RE1_HIGH_SETPOINT, new UnsignedWordElement(222)), //
						m(VigilexVigiDt1.ChannelId.HYDROGEN_RE2_LOW_SETPOINT, new UnsignedWordElement(223)), //
						m(VigilexVigiDt1.ChannelId.HYDROGEN_RE2_HIGH_SETPOINT, new UnsignedWordElement(224)), //
						m(VigilexVigiDt1.ChannelId.VOC_RE1_LOW_SETPOINT, new UnsignedWordElement(225)), //
						m(VigilexVigiDt1.ChannelId.VOC_RE1_HIGH_SETPOINT, new UnsignedWordElement(226)), //
						m(VigilexVigiDt1.ChannelId.VOC_RE2_LOW_SETPOINT, new UnsignedWordElement(227)), //
						m(VigilexVigiDt1.ChannelId.VOC_RE2_HIGH_SETPOINT, new UnsignedWordElement(228))), //

				new FC3ReadRegistersTask(256, Priority.HIGH, //
						m(VigilexVigiDt1.ChannelId.TEMPERATURE, new SignedWordElement(256), SCALE_FACTOR_MINUS_2), //
						m(VigilexVigiDt1.ChannelId.HUMIDITY, new UnsignedWordElement(257), SCALE_FACTOR_MINUS_2), //
						m(VigilexVigiDt1.ChannelId.VOC, new UnsignedWordElement(258)), //
						m(VigilexVigiDt1.ChannelId.HYDROGEN, new UnsignedWordElement(259), SCALE_FACTOR_MINUS_2)), //

				new FC6WriteRegisterTask(215, //
						m(VigilexVigiDt1.ChannelId.RE1_DELAY, new UnsignedWordElement(215))), //
				new FC6WriteRegisterTask(216, //
						m(VigilexVigiDt1.ChannelId.RE2_DELAY, new UnsignedWordElement(216))), //

				new FC6WriteRegisterTask(221, //
						m(VigilexVigiDt1.ChannelId.HYDROGEN_RE1_LOW_SETPOINT, new UnsignedWordElement(221))), //
				new FC6WriteRegisterTask(222, //
						m(VigilexVigiDt1.ChannelId.HYDROGEN_RE1_HIGH_SETPOINT, new UnsignedWordElement(222))), //
				new FC6WriteRegisterTask(223, //
						m(VigilexVigiDt1.ChannelId.HYDROGEN_RE2_LOW_SETPOINT, new UnsignedWordElement(223))), //
				new FC6WriteRegisterTask(224, //
						m(VigilexVigiDt1.ChannelId.HYDROGEN_RE2_HIGH_SETPOINT, new UnsignedWordElement(224))), //

				new FC6WriteRegisterTask(225, //
						m(VigilexVigiDt1.ChannelId.VOC_RE1_LOW_SETPOINT, new UnsignedWordElement(225))), //
				new FC6WriteRegisterTask(226, //
						m(VigilexVigiDt1.ChannelId.VOC_RE1_HIGH_SETPOINT, new UnsignedWordElement(226))), //
				new FC6WriteRegisterTask(227, //
						m(VigilexVigiDt1.ChannelId.VOC_RE2_LOW_SETPOINT, new UnsignedWordElement(227))), //
				new FC6WriteRegisterTask(228, //
						m(VigilexVigiDt1.ChannelId.VOC_RE2_HIGH_SETPOINT, new UnsignedWordElement(228))) //
		);
	}

	@Override
	public String debugLog() {
		return "H2:" + this.channel(VigilexVigiDt1.ChannelId.HYDROGEN).value().asString() //
				+ "|VOC:" + this.channel(VigilexVigiDt1.ChannelId.VOC).value().asString() //
				+ "|RE1:" + this.channel(VigilexVigiDt1.ChannelId.RE1_STATE).value().asString() //
				+ "|RE2:" + this.channel(VigilexVigiDt1.ChannelId.RE2_STATE).value().asString();
	}
}