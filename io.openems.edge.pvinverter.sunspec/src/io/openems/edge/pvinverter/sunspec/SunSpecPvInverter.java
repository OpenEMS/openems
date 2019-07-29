package io.openems.edge.pvinverter.sunspec;

import java.util.Optional;

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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModelType;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PV-Inverter.SunSpec", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
		})
public class SunSpecPvInverter extends AbstractOpenemsSunSpecComponent
		implements ManagedSymmetricPvInverter, SymmetricMeter, OpenemsComponent, EventHandler {

	private final static SunSpecModelType[] MODEL_TYPES = { //
			SunSpecModelType.COMMON, SunSpecModelType.INVERTER //
	};

	@Reference
	protected ConfigurationAdmin cm;

	private final Logger log = LoggerFactory.getLogger(SunSpecPvInverter.class);
	private final SetPvLimitHandler setPvLimitHandler = new SetPvLimitHandler(this);

	public SunSpecPvInverter() {
		super(//
				MODEL_TYPES, //
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				PvChannelId.values() //
		);
		this.getMaxApparentPower().setNextValue(15_000); // TODO read from SunSpec
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			try {
				this.setPvLimitHandler.run();

				this.channel(PvChannelId.PV_LIMIT_FAILED).setNextValue(false);
			} catch (OpenemsNamedException e) {
				this.channel(PvChannelId.PV_LIMIT_FAILED).setNextValue(true);
			}
			break;
		}
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.PRODUCTION;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}

	@Override
	protected void addUnknownBlock(int startAddress, int sunSpecBlockId) {
		this.logInfo(this.log, "SunSpec-Model [" + sunSpecBlockId + "] is not handled.");
	}

	@Override
	protected void onSunSpecInitializationCompleted() {
		this.logInfo(this.log, "SunSpec initialization finished. " + this.channels().size() + " Channels available.");

		/*
		 * SymmetricMeter
		 */
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.FREQUENCY, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				SunSpecModel.S111.HZ, SunSpecModel.S112.HZ, SunSpecModel.S113.HZ, SunSpecModel.S101.HZ,
				SunSpecModel.S102.HZ, SunSpecModel.S103.HZ);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.ACTIVE_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				SunSpecModel.S111.W, SunSpecModel.S112.W, SunSpecModel.S113.W, SunSpecModel.S101.W, SunSpecModel.S102.W,
				SunSpecModel.S103.W);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.REACTIVE_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				SunSpecModel.S111.V_AR, SunSpecModel.S112.V_AR, SunSpecModel.S113.V_AR, SunSpecModel.S101.V_AR,
				SunSpecModel.S102.V_AR, SunSpecModel.S103.V_AR);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				SunSpecModel.S111.WH, SunSpecModel.S112.WH, SunSpecModel.S113.WH, SunSpecModel.S101.WH,
				SunSpecModel.S102.WH, SunSpecModel.S103.WH);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.VOLTAGE, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				SunSpecModel.S111.PH_VPH_A, SunSpecModel.S111.PH_VPH_B, SunSpecModel.S111.PH_VPH_C,
				SunSpecModel.S112.PH_VPH_A, SunSpecModel.S112.PH_VPH_B, SunSpecModel.S112.PH_VPH_C,
				SunSpecModel.S113.PH_VPH_A, SunSpecModel.S113.PH_VPH_B, SunSpecModel.S113.PH_VPH_C,
				SunSpecModel.S101.PH_VPH_A, SunSpecModel.S101.PH_VPH_B, SunSpecModel.S101.PH_VPH_C,
				SunSpecModel.S102.PH_VPH_A, SunSpecModel.S102.PH_VPH_B, SunSpecModel.S102.PH_VPH_C,
				SunSpecModel.S103.PH_VPH_A, SunSpecModel.S103.PH_VPH_B, SunSpecModel.S103.PH_VPH_C);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.CURRENT, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				SunSpecModel.S111.A, SunSpecModel.S112.A, SunSpecModel.S113.A, SunSpecModel.S101.A, SunSpecModel.S102.A,
				SunSpecModel.S103.A);
	}

	@Override
	protected <T> Optional<T> getSunSpecChannel(SunSpecPoint point) {
		return super.getSunSpecChannel(point);
	}
}
