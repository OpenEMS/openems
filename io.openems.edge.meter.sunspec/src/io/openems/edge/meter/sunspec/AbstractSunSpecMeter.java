package io.openems.edge.meter.sunspec;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIRECT_1_TO_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;

import java.util.Map;
import java.util.Optional;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;

public abstract class AbstractSunSpecMeter extends AbstractOpenemsSunSpecComponent
		implements ElectricityMeter, OpenemsComponent {

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

		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.FREQUENCY, //
				SCALE_FACTOR_3, //
				DefaultSunSpecModel.S204.HZ, DefaultSunSpecModel.S203.HZ, DefaultSunSpecModel.S202.HZ,
				DefaultSunSpecModel.S201.HZ);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_POWER, //
				INVERT, //
				DefaultSunSpecModel.S204.W, DefaultSunSpecModel.S203.W, DefaultSunSpecModel.S202.W,
				DefaultSunSpecModel.S201.W);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.REACTIVE_POWER, //
				INVERT, //
				DefaultSunSpecModel.S204.VAR, DefaultSunSpecModel.S203.VAR, DefaultSunSpecModel.S202.VAR,
				DefaultSunSpecModel.S201.VAR);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, //
				DIRECT_1_TO_1, //
				DefaultSunSpecModel.S204.TOT_WH_EXP, DefaultSunSpecModel.S203.TOT_WH_EXP,
				DefaultSunSpecModel.S202.TOT_WH_EXP, DefaultSunSpecModel.S201.TOT_WH_EXP);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
				DIRECT_1_TO_1, //
				DefaultSunSpecModel.S204.TOT_WH_IMP, DefaultSunSpecModel.S203.TOT_WH_IMP,
				DefaultSunSpecModel.S202.TOT_WH_IMP, DefaultSunSpecModel.S201.TOT_WH_IMP);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.VOLTAGE, //
				SCALE_FACTOR_3, //
				DefaultSunSpecModel.S204.PH_V, DefaultSunSpecModel.S203.PH_V, DefaultSunSpecModel.S202.PH_V,
				DefaultSunSpecModel.S201.PH_V, //
				DefaultSunSpecModel.S204.PH_VPH_A, DefaultSunSpecModel.S203.PH_VPH_A, DefaultSunSpecModel.S202.PH_VPH_A,
				DefaultSunSpecModel.S201.PH_VPH_A, //
				DefaultSunSpecModel.S204.PH_VPH_B, DefaultSunSpecModel.S203.PH_VPH_B, DefaultSunSpecModel.S202.PH_VPH_B,
				DefaultSunSpecModel.S201.PH_VPH_B, //
				DefaultSunSpecModel.S204.PH_VPH_C, DefaultSunSpecModel.S203.PH_VPH_C, DefaultSunSpecModel.S202.PH_VPH_C,
				DefaultSunSpecModel.S201.PH_VPH_C);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.CURRENT, //
				SCALE_FACTOR_3, //
				DefaultSunSpecModel.S204.A, DefaultSunSpecModel.S203.A, DefaultSunSpecModel.S202.A,
				DefaultSunSpecModel.S201.A);

		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_POWER_L1, //
				INVERT, //
				DefaultSunSpecModel.S204.WPH_A, DefaultSunSpecModel.S203.WPH_A, DefaultSunSpecModel.S202.WPH_A,
				DefaultSunSpecModel.S201.WPH_A);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_POWER_L2, //
				INVERT, //
				DefaultSunSpecModel.S204.WPH_B, DefaultSunSpecModel.S203.WPH_B, DefaultSunSpecModel.S202.WPH_B,
				DefaultSunSpecModel.S201.WPH_B);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_POWER_L3, //
				INVERT, //
				DefaultSunSpecModel.S204.WPH_C, DefaultSunSpecModel.S203.WPH_C, DefaultSunSpecModel.S202.WPH_C,
				DefaultSunSpecModel.S201.WPH_C);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.CURRENT_L1, //
				SCALE_FACTOR_3, //
				DefaultSunSpecModel.S204.APH_A, DefaultSunSpecModel.S203.APH_A, DefaultSunSpecModel.S202.APH_A,
				DefaultSunSpecModel.S201.APH_A);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.CURRENT_L2, //
				SCALE_FACTOR_3, //
				DefaultSunSpecModel.S204.APH_B, DefaultSunSpecModel.S203.APH_B, DefaultSunSpecModel.S202.APH_B,
				DefaultSunSpecModel.S201.APH_B);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.CURRENT_L3, //
				SCALE_FACTOR_3, //
				DefaultSunSpecModel.S204.APH_C, DefaultSunSpecModel.S203.APH_C, DefaultSunSpecModel.S202.APH_C,
				DefaultSunSpecModel.S201.APH_C);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.REACTIVE_POWER_L1, //
				INVERT, //
				DefaultSunSpecModel.S204.V_A_RPH_A, DefaultSunSpecModel.S203.V_A_RPH_A,
				DefaultSunSpecModel.S202.V_A_RPH_A, DefaultSunSpecModel.S201.V_A_RPH_A);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.REACTIVE_POWER_L2, //
				INVERT, //
				DefaultSunSpecModel.S204.V_A_RPH_B, DefaultSunSpecModel.S203.V_A_RPH_B,
				DefaultSunSpecModel.S202.V_A_RPH_B, DefaultSunSpecModel.S201.V_A_RPH_B);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.REACTIVE_POWER_L3, //
				INVERT, //
				DefaultSunSpecModel.S204.V_A_RPH_C, DefaultSunSpecModel.S203.V_A_RPH_C,
				DefaultSunSpecModel.S202.V_A_RPH_C, DefaultSunSpecModel.S201.V_A_RPH_C);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.VOLTAGE_L1, //
				SCALE_FACTOR_3, //
				DefaultSunSpecModel.S204.PH_VPH_A, DefaultSunSpecModel.S203.PH_VPH_A, DefaultSunSpecModel.S202.PH_VPH_A,
				DefaultSunSpecModel.S201.PH_VPH_A);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.VOLTAGE_L2, //
				SCALE_FACTOR_3, //
				DefaultSunSpecModel.S204.PH_VPH_B, DefaultSunSpecModel.S203.PH_VPH_B, DefaultSunSpecModel.S202.PH_VPH_B,
				DefaultSunSpecModel.S201.PH_VPH_B);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.VOLTAGE_L3, //
				SCALE_FACTOR_3, //
				DefaultSunSpecModel.S204.PH_VPH_C, DefaultSunSpecModel.S203.PH_VPH_C, DefaultSunSpecModel.S202.PH_VPH_C,
				DefaultSunSpecModel.S201.PH_VPH_C);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, //
				DIRECT_1_TO_1, //
				DefaultSunSpecModel.S204.TOT_WH_EXP_PH_A, DefaultSunSpecModel.S203.TOT_WH_EXP_PH_A,
				DefaultSunSpecModel.S202.TOT_WH_EXP_PH_A, DefaultSunSpecModel.S201.TOT_WH_EXP_PH_A);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, //
				DIRECT_1_TO_1, //
				DefaultSunSpecModel.S204.TOT_WH_EXP_PH_B, DefaultSunSpecModel.S203.TOT_WH_EXP_PH_B,
				DefaultSunSpecModel.S202.TOT_WH_EXP_PH_B, DefaultSunSpecModel.S201.TOT_WH_EXP_PH_B);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, //
				DIRECT_1_TO_1, //
				DefaultSunSpecModel.S204.TOT_WH_EXP_PH_C, DefaultSunSpecModel.S203.TOT_WH_EXP_PH_C,
				DefaultSunSpecModel.S202.TOT_WH_EXP_PH_C, DefaultSunSpecModel.S201.TOT_WH_EXP_PH_C);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, //
				DIRECT_1_TO_1, //
				DefaultSunSpecModel.S204.TOT_WH_IMP_PH_A, DefaultSunSpecModel.S203.TOT_WH_IMP_PH_A,
				DefaultSunSpecModel.S202.TOT_WH_IMP_PH_A, DefaultSunSpecModel.S201.TOT_WH_IMP_PH_A);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, //
				DIRECT_1_TO_1, //
				DefaultSunSpecModel.S204.TOT_WH_IMP_PH_B, DefaultSunSpecModel.S203.TOT_WH_IMP_PH_B,
				DefaultSunSpecModel.S202.TOT_WH_IMP_PH_B, DefaultSunSpecModel.S201.TOT_WH_IMP_PH_B);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, //
				DIRECT_1_TO_1, //
				DefaultSunSpecModel.S204.TOT_WH_IMP_PH_C, DefaultSunSpecModel.S203.TOT_WH_IMP_PH_C,
				DefaultSunSpecModel.S202.TOT_WH_IMP_PH_C, DefaultSunSpecModel.S201.TOT_WH_IMP_PH_C);
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
