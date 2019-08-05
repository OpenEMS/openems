package io.openems.edge.meter.sunspec;

import java.util.Optional;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModelType;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;

public abstract class AbstractSunSpecMeter extends AbstractOpenemsSunSpecComponent
		implements AsymmetricMeter, SymmetricMeter, OpenemsComponent {

	private static final SunSpecModelType[] MODEL_TYPES = { //
			SunSpecModelType.COMMON, SunSpecModelType.METER //
	};

	private final Logger log = LoggerFactory.getLogger(AbstractSunSpecMeter.class);

	public AbstractSunSpecMeter() {
		super(//
				MODEL_TYPES, //
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				MeterChannelId.values() //
		);
	}

	/**
	 * Make sure to call this method from the inheriting OSGi Component.
	 */
	@Override
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int unitId,
			ConfigurationAdmin cm, String modbusReference, String modbusId, int readFromCommonBlockNo) {
		return super.activate(context, id, alias, enabled, unitId, cm, modbusReference, modbusId,
				readFromCommonBlockNo);
	}

	/**
	 * Make sure to call this method from the inheriting OSGi Component.
	 */
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
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
				SunSpecModel.S204.HZ, SunSpecModel.S203.HZ, SunSpecModel.S202.HZ, SunSpecModel.S201.HZ);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.ACTIVE_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				SunSpecModel.S204.W, SunSpecModel.S203.W, SunSpecModel.S202.W, SunSpecModel.S201.W);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.REACTIVE_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				SunSpecModel.S204.VAR, SunSpecModel.S203.VAR, SunSpecModel.S202.VAR, SunSpecModel.S201.VAR);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				SunSpecModel.S204.TOT_WH_IMP, SunSpecModel.S203.TOT_WH_IMP, SunSpecModel.S202.TOT_WH_IMP,
				SunSpecModel.S201.TOT_WH_IMP);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				SunSpecModel.S204.TOT_WH_EXP, SunSpecModel.S203.TOT_WH_EXP, SunSpecModel.S202.TOT_WH_EXP,
				SunSpecModel.S201.TOT_WH_EXP);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.VOLTAGE, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				SunSpecModel.S204.PH_V, SunSpecModel.S203.PH_V, SunSpecModel.S202.PH_V, SunSpecModel.S201.PH_V, //
				SunSpecModel.S204.PH_VPH_A, SunSpecModel.S203.PH_VPH_A, SunSpecModel.S202.PH_VPH_A,
				SunSpecModel.S201.PH_VPH_A, //
				SunSpecModel.S204.PH_VPH_B, SunSpecModel.S203.PH_VPH_B, SunSpecModel.S202.PH_VPH_B,
				SunSpecModel.S201.PH_VPH_B, //
				SunSpecModel.S204.PH_VPH_C, SunSpecModel.S203.PH_VPH_C, SunSpecModel.S202.PH_VPH_C,
				SunSpecModel.S201.PH_VPH_C);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.CURRENT, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				SunSpecModel.S204.A, SunSpecModel.S203.A, SunSpecModel.S202.A, SunSpecModel.S201.A);

		/*
		 * AsymmetricMeter
		 */
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				SunSpecModel.S204.WPH_A, SunSpecModel.S203.WPH_A, SunSpecModel.S202.WPH_A, SunSpecModel.S201.WPH_A);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				SunSpecModel.S204.WPH_B, SunSpecModel.S203.WPH_B, SunSpecModel.S202.WPH_B, SunSpecModel.S201.WPH_B);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				SunSpecModel.S204.WPH_C, SunSpecModel.S203.WPH_C, SunSpecModel.S202.WPH_C, SunSpecModel.S201.WPH_C);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.CURRENT_L1, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				SunSpecModel.S204.APH_A, SunSpecModel.S203.APH_A, SunSpecModel.S202.APH_A, SunSpecModel.S201.APH_A);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.CURRENT_L2, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				SunSpecModel.S204.APH_B, SunSpecModel.S203.APH_B, SunSpecModel.S202.APH_B, SunSpecModel.S201.APH_B);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.CURRENT_L3, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				SunSpecModel.S204.APH_C, SunSpecModel.S203.APH_C, SunSpecModel.S202.APH_C, SunSpecModel.S201.APH_C);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				SunSpecModel.S204.V_A_RPH_A, SunSpecModel.S203.V_A_RPH_A, SunSpecModel.S202.V_A_RPH_A,
				SunSpecModel.S201.V_A_RPH_A);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				SunSpecModel.S204.V_A_RPH_B, SunSpecModel.S203.V_A_RPH_B, SunSpecModel.S202.V_A_RPH_B,
				SunSpecModel.S201.V_A_RPH_B);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				SunSpecModel.S204.V_A_RPH_C, SunSpecModel.S203.V_A_RPH_C, SunSpecModel.S202.V_A_RPH_C,
				SunSpecModel.S201.V_A_RPH_C);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.VOLTAGE_L1, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				SunSpecModel.S204.PH_VPH_A, SunSpecModel.S203.PH_VPH_A, SunSpecModel.S202.PH_VPH_A,
				SunSpecModel.S201.PH_VPH_A);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.VOLTAGE_L2, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				SunSpecModel.S204.PH_VPH_B, SunSpecModel.S203.PH_VPH_B, SunSpecModel.S202.PH_VPH_B,
				SunSpecModel.S201.PH_VPH_B);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.VOLTAGE_L3, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				SunSpecModel.S204.PH_VPH_C, SunSpecModel.S203.PH_VPH_C, SunSpecModel.S202.PH_VPH_C,
				SunSpecModel.S201.PH_VPH_C);
	}

	@Override
	protected <T extends Channel<?>> Optional<T> getSunSpecChannel(SunSpecPoint point) {
		return super.getSunSpecChannel(point);
	}

	@Override
	protected <T extends Channel<?>> T getSunSpecChannelOrError(SunSpecPoint point) throws OpenemsException {
		return super.getSunSpecChannelOrError(point);
	}
}
