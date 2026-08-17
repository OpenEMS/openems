package io.openems.edge.fronius.gen24.dccharger;

import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.Map;
import java.util.function.Consumer;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.fronius.enums.PvString;
import io.openems.edge.fronius.gen24.batteryinverter.S160SunSpecModel;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

/**
 * DC Charger for a single PV string of the Fronius Gen24 hybrid inverter.
 *
 * <p>
 * Like {@code FroniusGen24BatteryImpl}, this component is fully independent of
 * {@code BatteryInverterFroniusGen24Impl} - no OSGi {@code @Reference} between
 * the two. It performs its own SunSpec discovery for Model S160 against the
 * same physical Modbus device the BatteryInverter also reads, and maps either
 * Module 1 or Module 2 (selected via {@link Config#pvString()}) to the standard
 * {@link EssDcCharger} channels.
 *
 * <p>
 * Like GoodWe/FENECON Commercial40, {@code BatteryInverterFroniusGen24Impl}
 * aggregates Chargers via {@code addCharger()}: its {@code getDcPvPower()} sums
 * the {@code ACTUAL_POWER} of all registered {@link FroniusGen24DcCharger}s
 * rather than reading the SunSpec Module 1/2 registers itself. This Charger's
 * role is therefore twofold: it feeds {@code getDcPvPower()} via registration,
 * and separately exposes the individual PV string as its own
 * {@link EssDcCharger} component (e.g. for per-string monitoring/UI).
 *
 * <p>
 * IMPORTANT: {@code modbus_id}/{@code modbusUnitId} in {@link Config} must
 * point at the same physical device (same Unit-ID) as the BatteryInverter's
 * Modbus configuration.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Fronius.Gen24.DcCharger", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
@EventTopics({ //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
})
@GenerateTargetsFromReferences("Modbus")
public class FroniusGen24DcChargerImpl extends AbstractOpenemsSunSpecComponent implements FroniusGen24DcCharger,
		EssDcCharger, TimedataProvider, EventHandler, ModbusComponent, OpenemsComponent {

	private static final int READ_FROM_MODBUS_BLOCK = 1;

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			.put(DefaultSunSpecModel.S_103, Priority.LOW) //
			.put(S160SunSpecModel.S_160, Priority.HIGH) //
			.build();

	private final Logger log = LoggerFactory.getLogger(FroniusGen24DcChargerImpl.class);

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this, //
			EssDcCharger.ChannelId.ACTUAL_ENERGY);

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	private Config config;

	public FroniusGen24DcChargerImpl() {
		super(ACTIVE_MODELS, OpenemsComponent.ChannelId.values(), ModbusComponent.ChannelId.values(),
				EssDcCharger.ChannelId.values(), FroniusGen24DcCharger.ChannelId.values());
	}

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))" //
	)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {

		this.config = config;

		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(),
				READ_FROM_MODBUS_BLOCK);
	}

	@Deactivate
	@Override
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected void onSunSpecInitializationCompleted() {

		this.logInfo(this.log, "SunSpec initialization finished. " + this.channels().size() + " Channels available.");

		this.mapFirstPointToChannel(FroniusGen24DcCharger.ChannelId.OPERATING_STATE,
				enumConverter(DefaultSunSpecModel.S103_St.values()), DefaultSunSpecModel.S103.ST);

		this.installListeners();
	}

	/**
	 * Builds a converter that resolves a raw Modbus/SunSpec integer value to the
	 * matching {@link io.openems.common.types.OptionsEnum} constant.
	 *
	 * <p>
	 * {@link ElementToChannelConverter#DIRECT_1_TO_1} is not sufficient here: it
	 * passes the raw value through unchanged, so the target Channel would end up
	 * holding a plain {@code Integer} instead of the declared enum constant -
	 * causing a {@link ClassCastException} the moment anything reads the Channel as
	 * its declared enum type.
	 *
	 * @param values the target enum's {@code values()}
	 * @return the converter
	 */
	private static ElementToChannelConverter enumConverter(io.openems.common.types.OptionsEnum[] values) {
		return new ElementToChannelConverter(value -> {
			if (value == null) {
				return null;
			}
			int intValue = ((Number) value).intValue();
			for (var option : values) {
				if (option.getValue() == intValue) {
					return option;
				}
			}
			return values.length > 0 ? values[0].getUndefined() : null;
		});
	}

	private void installListeners() {

		final Consumer<Value<Float>> powerConsumer = value -> {
			if (value.isDefined() && !Float.isNaN(value.get()) && value.get() < 1e9f) {
				this._setActualPower(Math.round(value.get()));
			} else {
				this._setActualPower(null);
			}
		};

		final Consumer<Value<Float>> voltageConsumer = value -> {
			if (value.isDefined() && !Float.isNaN(value.get()) && value.get() < 1e9f) {
				this._setVoltage(Math.round(1000.0f * value.get()));
			} else {
				this._setVoltage(null);
			}
		};

		final Consumer<Value<Float>> ampereConsumer = value -> {
			if (value.isDefined() && !Float.isNaN(value.get()) && value.get() < 1e9f) {
				this._setCurrent(Math.round(1000.0f * value.get()));
			} else {
				this._setCurrent(null);
			}
		};

		try {
			this.getConfiguredModuleDcwChannel().onSetNextValue(powerConsumer);
			this.getModuleDcvChannel().onSetNextValue(voltageConsumer);
			this.getModuleDcaChannel().onSetNextValue(ampereConsumer);
		} catch (OpenemsException e) {
			this.log.error("Failed to install listeners", e);
		}
	}

	private Channel<Float> getModuleDcaChannel() throws OpenemsException {
		return this.getSunSpecChannelOrError(this.config.pvString() == PvString.ONE //
				? S160SunSpecModel.S160.MODULE_1_D_C_A //
				: S160SunSpecModel.S160.MODULE_2_D_C_A);
	}

	private Channel<Float> getModuleDcvChannel() throws OpenemsException {
		return this.getSunSpecChannelOrError(this.config.pvString() == PvString.ONE //
				? S160SunSpecModel.S160.MODULE_1_D_C_V //
				: S160SunSpecModel.S160.MODULE_2_D_C_V);
	}

	@Override
	public Channel<Float> getModule1DcwChannel() throws OpenemsException {
		return this.getSunSpecChannelOrError(S160SunSpecModel.S160.MODULE_1_D_C_W);
	}

	@Override
	public Channel<Float> getModule2DcwChannel() throws OpenemsException {
		return this.getSunSpecChannelOrError(S160SunSpecModel.S160.MODULE_2_D_C_W);
	}

	private Channel<Float> getConfiguredModuleDcwChannel() throws OpenemsException {
		return this.config.pvString() == PvString.ONE //
				? this.getModule1DcwChannel() //
				: this.getModule2DcwChannel();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		if (event.getTopic().equals(TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
			this.calculateProductionEnergy.update(this.getActualPower().get());
		}
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActualPower().asString() + "|State:" + this.getOperatingState().asString();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
