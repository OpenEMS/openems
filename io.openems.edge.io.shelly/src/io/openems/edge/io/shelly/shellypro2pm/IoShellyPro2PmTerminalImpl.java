package io.openems.edge.io.shelly.shellypro2pm;

import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.types.MeterType;
import io.openems.common.types.Result;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.Phase;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.io.shelly.common.component.ShellyMeteredSwitch;
import io.openems.edge.io.shelly.common.component.ShellyMeteredSwitchHandler;
import io.openems.edge.io.shelly.common.component.ShellySwitch;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = TerminalConfig.class, factory = true)
@Component(//
		name = "IO.Shelly.Pro2PM.Terminal", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_EXECUTE_WRITE, //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class IoShellyPro2PmTerminalImpl extends AbstractOpenemsComponent
		implements IoShellyPro2PmTerminal, ShellyMeteredSwitch, ShellySwitch, SinglePhaseMeter, ElectricityMeter,
		DigitalOutput, TimedataProvider, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(IoShellyPro2PmTerminalImpl.class);

	private ShellyMeteredSwitchHandler handler;

	private TerminalEnum terminal;
	private MeterType meterType = null;

	private final AtomicReference<IoShellyPro2PmDevice> device = new AtomicReference<>(null);

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata;
	@Reference
	private ConfigurationAdmin cm;

	public IoShellyPro2PmTerminalImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				SinglePhaseMeter.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				ShellySwitch.ChannelId.values(), //
				ShellyMeteredSwitch.ChannelId.values(), //
				ShellyMeteredSwitch.ErrorChannelId.values() //
		);
	}

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setDevice(IoShellyPro2PmDevice device) {
		this.device.set(device);
	}

	@Activate
	protected void activate(ComponentContext context, TerminalConfig config) {
		this.terminal = config.terminal();
		this.meterType = config.type();

		super.activate(context, config.id(), config.alias(), config.enabled());
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Device", config.device_id())) {
			return;
		}

		var device = this.device.get();
		if (config.enabled() && device != null) {
			this.handler = new ShellyMeteredSwitchHandler(this, device.getShellyService(),
					this.terminal.getShellyIndex(), config.invert());
			device.addStatusCallback(this.processShellyStatus);
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();

		var device = this.device.getAndSet(null);
		if (device != null) {
			device.removeStatusCallback(this.processShellyStatus);
			this.handler = null;
		}
	}

	@Override
	public String debugLog() {
		if (this.handler == null) {
			return "Handler not initialized.";
		}
		return this.handler.generateDebugLog();
	}

	@Override
	public void handleEvent(Event event) {
		if (this.isEnabled() && this.handler != null) {
			this.handler.handleEvent(event);
		}
	}

	private final Consumer<Result<JsonObject>> processShellyStatus = (result) -> {
		try {
			switch (result) {
			case Result.Ok(var json) -> {
				var switchJson = JsonUtils.getAsJsonObject(json, "switch:" + this.terminal.getShellyIndex());
				this.handler.processSwitchData(switchJson);
			}
			case Result.Error(var exception) -> {
				// Exception is already logged by parent device
				this.handler.resetSwitchData();
			}
			}
		} catch (Exception ex) {
			this.logWarn(this.log, "Error while processing shelly status: " + ex.getMessage());
			this.handler.resetSwitchData();
		}
	};

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return (this.handler != null) ? this.handler.getDigitalOutputChannels() : new BooleanWriteChannel[0];
	}

	@Override
	public Phase.SinglePhase getPhase() {
		var device = this.device.get();
		return (device != null) ? device.getPhase() : null;
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
