package io.openems.edge.sma.ess.stpxx3se.dccharger;

import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.sma.ess.stpxx3se.batteryinverter.BatteryInverterSmaStpSe;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.SMA.StpSe.DcCharger", //
		immediate = true, //
		configurationPolicy = REQUIRE)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
})
public class SmaStpSeDcChargerImpl extends AbstractOpenemsComponent
		implements EssDcCharger, TimedataProvider, EventHandler, OpenemsComponent {

	protected Config config = null;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	private BatteryInverterSmaStpSe inverter;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this, //
			EssDcCharger.ChannelId.ACTUAL_ENERGY);

	private boolean mapOnce = true;

	public SmaStpSeDcChargerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException, OpenemsNamedException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!this.config.enabled()) {
			return;
		}

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			if (this.inverter.isInitialized()) {
				this.mapChannelValuesOnce();
			}
		}
		case TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> {
			this.calculateProductionEnergy.update(this.getActualPower().get());
		}
		}
	}

	private void mapChannelValuesOnce() {
		if (!this.mapOnce) {
			return;
		}

		final Consumer<Value<Float>> powerConsumer = value -> {
			if (value.isDefined()) {
				this._setActualPower(TypeUtils.getAsType(OpenemsType.INTEGER, value));
			} else {
				this._setActualPower(null);
			}
		};

		final Consumer<Value<Float>> voltageConsumer = value -> {
			if (value.isDefined()) {
				this._setVoltage((int) (1000.0f * (float) TypeUtils.getAsType(OpenemsType.FLOAT, value)));
			} else {
				this._setVoltage(null);
			}
		};
		final Consumer<Value<Float>> ampereConsumer = value -> {
			if (value.isDefined()) {
				this._setCurrent((int) (1000.0f * (float) TypeUtils.getAsType(OpenemsType.FLOAT, value)));
			} else {
				this._setCurrent(null);
			}

		};

		try {
			switch (this.config.pvString()) {
			case ONE -> {
				this.inverter.getModule1DcwChannel().onSetNextValue(powerConsumer);
				this.inverter.getModule1DcvChannel().onSetNextValue(voltageConsumer);
				this.inverter.getModule1DcaChannel().onSetNextValue(ampereConsumer);
			}
			case TWO -> {
				this.inverter.getModule2DcwChannel().onSetNextValue(powerConsumer);
				this.inverter.getModule2DcvChannel().onSetNextValue(voltageConsumer);
				this.inverter.getModule2DcaChannel().onSetNextValue(ampereConsumer);
			}
			}
			this.mapOnce = false;
		} catch (OpenemsException e) {
			this.mapOnce = true;
			e.printStackTrace();
		}

	}

	@Override
	public String debugLog() {
		return "L:" + this.getActualPower().asString();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

}
