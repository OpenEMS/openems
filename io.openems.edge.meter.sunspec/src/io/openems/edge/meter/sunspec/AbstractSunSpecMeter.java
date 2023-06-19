package io.openems.edge.meter.sunspec;

import java.util.Map;
import java.util.Optional;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S201;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S202;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S203;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S204;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;

public abstract class AbstractSunSpecMeter extends AbstractOpenemsSunSpecComponent
		implements AsymmetricMeter, SymmetricMeter, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(AbstractSunSpecMeter.class);

	public AbstractSunSpecMeter(Map<SunSpecModel, Priority> activeModels,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) throws OpenemsException {
		super(activeModels, firstInitialChannelIds, furtherInitialChannelIds);
	}

	/**
	 * Make sure to call this method from the inheriting OSGi Component.
	 *
	 * @throws OpenemsException on error
	 */
	@Override
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int unitId,
			ConfigurationAdmin cm, String modbusReference, String modbusId, int readFromCommonBlockNo)
			throws OpenemsException {
		return super.activate(context, id, alias, enabled, unitId, cm, modbusReference, modbusId,
				readFromCommonBlockNo);
	}

	/**
	 * Make sure to call this method from the inheriting OSGi Component.
	 */
	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
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
				S204.HZ, S203.HZ, S202.HZ, S201.HZ);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.ACTIVE_POWER, //
				ElementToChannelConverter.INVERT, //
				S204.W, S203.W, S202.W, S201.W);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.REACTIVE_POWER, //
				ElementToChannelConverter.INVERT, //
				S204.VAR, S203.VAR, S202.VAR, S201.VAR);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				S204.TOT_WH_EXP, S203.TOT_WH_EXP, S202.TOT_WH_EXP, S201.TOT_WH_EXP);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				S204.TOT_WH_IMP, S203.TOT_WH_IMP, S202.TOT_WH_IMP, S201.TOT_WH_IMP);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.VOLTAGE, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				S204.PH_V, S203.PH_V, S202.PH_V, S201.PH_V, //
				S204.PH_VPH_A, S203.PH_VPH_A, S202.PH_VPH_A, S201.PH_VPH_A, //
				S204.PH_VPH_B, S203.PH_VPH_B, S202.PH_VPH_B, S201.PH_VPH_B, //
				S204.PH_VPH_C, S203.PH_VPH_C, S202.PH_VPH_C, S201.PH_VPH_C);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.CURRENT, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				S204.A, S203.A, S202.A, S201.A);

		/*
		 * AsymmetricMeter
		 */
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, //
				ElementToChannelConverter.INVERT, //
				S204.WPH_A, S203.WPH_A, S202.WPH_A, S201.WPH_A);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, //
				ElementToChannelConverter.INVERT, //
				S204.WPH_B, S203.WPH_B, S202.WPH_B, S201.WPH_B);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, //
				ElementToChannelConverter.INVERT, //
				S204.WPH_C, S203.WPH_C, S202.WPH_C, S201.WPH_C);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.CURRENT_L1, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				S204.APH_A, S203.APH_A, S202.APH_A, S201.APH_A);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.CURRENT_L2, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				S204.APH_B, S203.APH_B, S202.APH_B, S201.APH_B);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.CURRENT_L3, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				S204.APH_C, S203.APH_C, S202.APH_C, S201.APH_C);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, //
				ElementToChannelConverter.INVERT, //
				S204.V_A_RPH_A, S203.V_A_RPH_A, S202.V_A_RPH_A, S201.V_A_RPH_A);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, //
				ElementToChannelConverter.INVERT, //
				S204.V_A_RPH_B, S203.V_A_RPH_B, S202.V_A_RPH_B, S201.V_A_RPH_B);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, //
				ElementToChannelConverter.INVERT, //
				S204.V_A_RPH_C, S203.V_A_RPH_C, S202.V_A_RPH_C, S201.V_A_RPH_C);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.VOLTAGE_L1, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				S204.PH_VPH_A, S203.PH_VPH_A, S202.PH_VPH_A, S201.PH_VPH_A);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.VOLTAGE_L2, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				S204.PH_VPH_B, S203.PH_VPH_B, S202.PH_VPH_B, S201.PH_VPH_B);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.VOLTAGE_L3, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				S204.PH_VPH_C, S203.PH_VPH_C, S202.PH_VPH_C, S201.PH_VPH_C);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				S204.TOT_WH_EXP_PH_A, S203.TOT_WH_EXP_PH_A, S202.TOT_WH_EXP_PH_A, S201.TOT_WH_EXP_PH_A);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				S204.TOT_WH_EXP_PH_B, S203.TOT_WH_EXP_PH_B, S202.TOT_WH_EXP_PH_B, S201.TOT_WH_EXP_PH_B);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				S204.TOT_WH_EXP_PH_C, S203.TOT_WH_EXP_PH_C, S202.TOT_WH_EXP_PH_C, S201.TOT_WH_EXP_PH_C);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				S204.TOT_WH_IMP_PH_A, S203.TOT_WH_IMP_PH_A, S202.TOT_WH_IMP_PH_A, S201.TOT_WH_IMP_PH_A);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				S204.TOT_WH_IMP_PH_B, S203.TOT_WH_IMP_PH_B, S202.TOT_WH_IMP_PH_B, S201.TOT_WH_IMP_PH_B);
		this.mapFirstPointToChannel(//
				AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				S204.TOT_WH_IMP_PH_C, S203.TOT_WH_IMP_PH_C, S202.TOT_WH_IMP_PH_C, S201.TOT_WH_IMP_PH_C);
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
