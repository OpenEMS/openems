package io.openems.edge.pvinverter.sunspec;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIRECT_1_TO_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S_101;
import static io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S_102;
import static io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S_103;
import static io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S_111;
import static io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S_112;
import static io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S_113;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S101;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S102;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S103;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S111;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S112;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S113;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S120;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SinglePhase;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

public abstract class AbstractSunSpecPvInverter extends AbstractOpenemsSunSpecComponent
		implements SunSpecPvInverter, ManagedSymmetricPvInverter, ElectricityMeter, OpenemsComponent, EventHandler {

	private static enum InverterType {
		SINGLE_PHASE(S_101, S_111), //
		SPLIT_PHASE(S_102, S_112), //
		THREE_PHASE(S_103, S_113);

		private final List<DefaultSunSpecModel> blocks;

		private InverterType(DefaultSunSpecModel... blocks) {
			this.blocks = Lists.newArrayList(blocks);
		}
	}

	private final Logger log = LoggerFactory.getLogger(AbstractSunSpecPvInverter.class);
	private final SetPvLimitHandler setPvLimitHandler = new SetPvLimitHandler(this);

	private boolean readOnly;
	private Phase phase;
	private InverterType inverterType = null;

	public AbstractSunSpecPvInverter(Map<SunSpecModel, Priority> activeModels,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) throws OpenemsException {
		super(activeModels, firstInitialChannelIds, furtherInitialChannelIds);
		this._setActiveConsumptionEnergy(0);

		// Automatically calculate sum values from L1/L2/L3
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	/**
	 * Make sure to call this method from the inheriting OSGi Component.
	 *
	 * @param context               ComponentContext of this component. Receive it
	 *                              from parameter for @Activate
	 * @param id                    ID of this component. Typically 'config.id()'
	 * @param alias                 Human-readable name of this Component. Typically
	 *                              'config.alias()'. Defaults to 'id' if empty
	 * @param enabled               Whether the component should be enabled.
	 *                              Typically 'config.enabled()'
	 * @param readOnly              In Read-Only mode no power-limitation commands
	 *                              are sent to the inverter
	 * @param unitId                Unit-ID of the Modbus target
	 * @param cm                    An instance of ConfigurationAdmin. Receive it
	 *                              using @Reference
	 * @param modbusReference       The name of the @Reference setter method for the
	 *                              Modbus bridge - e.g. 'Modbus' if you have a
	 *                              setModbus()-method
	 * @param modbusId              The ID of the Modbus bridge. Typically
	 *                              'config.modbus_id()'
	 * @param readFromCommonBlockNo the starting block number
	 * @param phase                 the phase the inverter is connected
	 * @return true if the target filter was updated. You may use it to abort the
	 *         activate() method.
	 * @throws OpenemsException on error
	 */
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, boolean readOnly,
			int unitId, ConfigurationAdmin cm, String modbusReference, String modbusId, int readFromCommonBlockNo,
			Phase phase) throws OpenemsException {
		this.readOnly = readOnly;
		this.phase = phase;
		return super.activate(context, id, alias, enabled, unitId, cm, modbusReference, modbusId,
				readFromCommonBlockNo);
	}

	@Override
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int unitId,
			ConfigurationAdmin cm, String modbusReference, String modbusId, int readFromCommonBlockNo)
			throws OpenemsException {
		throw new IllegalArgumentException("Use the other activate() method.");
	}

	/**
	 * Make sure to call this method from the inheriting OSGi Component.
	 */
	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Make sure to call this method from the inheriting OSGi Component.
	 *
	 * <p>
	 * Requires:
	 *
	 * <pre>{@code
	 * &#64;EventTopics({ //
	 * 	EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
	 * })}
	 * </pre>
	 *
	 */
	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled() || !this.isSunSpecInitializationCompleted()) {
			this.channel(SunSpecPvInverter.ChannelId.PV_LIMIT_FAILED).setNextValue(false);
			this.channel(SunSpecPvInverter.ChannelId.READ_ONLY_MODE_PV_LIMIT_FAILED).setNextValue(false);
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			// Get ActivePowerLimit that should be applied
			var activePowerLimitChannel = (IntegerWriteChannel) this
					.channel(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT);
			var activePowerLimitOpt = activePowerLimitChannel.getNextWriteValueAndReset();

			// Set warning if read-only mode is active but a PV limit was requested
			this.channel(SunSpecPvInverter.ChannelId.READ_ONLY_MODE_PV_LIMIT_FAILED)
					.setNextValue(this.readOnly && activePowerLimitOpt.isPresent());

			// In read-only mode: stop here
			if (this.readOnly) {
				return;
			}

			try {
				this.setPvLimitHandler.accept(activePowerLimitOpt);

				this.channel(SunSpecPvInverter.ChannelId.PV_LIMIT_FAILED).setNextValue(false);
			} catch (OpenemsNamedException e) {
				this.channel(SunSpecPvInverter.ChannelId.PV_LIMIT_FAILED).setNextValue(true);
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
		return "L:" + this.getActivePower().asString();
	}

	@Override
	protected void onSunSpecInitializationCompleted() {
		this.logInfo(this.log, "SunSpec initialization finished. " + this.channels().size() + " Channels available.");

		this.channel(SunSpecPvInverter.ChannelId.WRONG_PHASE_CONFIGURED).setNextValue(
				this.inverterType == InverterType.SINGLE_PHASE ? this.phase == Phase.ALL : this.phase != Phase.ALL);

		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.FREQUENCY, //
				SCALE_FACTOR_3, //
				S111.HZ, S112.HZ, S113.HZ, S101.HZ, S102.HZ, S103.HZ);

		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_POWER, //
				DIRECT_1_TO_1, //
				S111.W, S112.W, S113.W, S101.W, S102.W, S103.W);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.REACTIVE_POWER, //
				DIRECT_1_TO_1, //
				S111.V_AR, S112.V_AR, S113.V_AR, S101.V_AR, S102.V_AR, S103.V_AR);

		// Individual Phases Power
		switch (this.inverterType) {
		case SINGLE_PHASE -> {
			var phase = switch (this.phase) {
			case ALL, L1 -> SinglePhase.L1; // Fallback to L1 on wrong configuration
			case L2 -> SinglePhase.L2;
			case L3 -> SinglePhase.L3;
			};
			if (phase != null) {
				SinglePhaseMeter.calculateSinglePhaseFromActivePower(this, (meter) -> phase);
			}
		}
		case SPLIT_PHASE, THREE_PHASE -> {
			ElectricityMeter.calculatePhasesFromActivePower(this);
			ElectricityMeter.calculatePhasesFromReactivePower(this);
		}
		}

		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
				DIRECT_1_TO_1, //
				S111.WH, S112.WH, S113.WH, S101.WH, S102.WH, S103.WH);
		this.mapFirstPointToChannel(//
				ManagedSymmetricPvInverter.ChannelId.MAX_APPARENT_POWER, //
				DIRECT_1_TO_1, //
				S120.W_RTG);

		// Voltage
		this.mapFirstPointToChannel(ElectricityMeter.ChannelId.VOLTAGE_L1, //
				SCALE_FACTOR_3, //
				S111.PH_VPH_A, S112.PH_VPH_A, S113.PH_VPH_A, S101.PH_VPH_A, S102.PH_VPH_A, S103.PH_VPH_A);
		this.mapFirstPointToChannel(ElectricityMeter.ChannelId.VOLTAGE_L2, //
				SCALE_FACTOR_3, //
				S111.PH_VPH_B, S112.PH_VPH_B, S113.PH_VPH_B, S101.PH_VPH_B, S102.PH_VPH_B, S103.PH_VPH_B);
		this.mapFirstPointToChannel(ElectricityMeter.ChannelId.VOLTAGE_L3, //
				SCALE_FACTOR_3, //
				S111.PH_VPH_C, S112.PH_VPH_C, S113.PH_VPH_C, S101.PH_VPH_C, S102.PH_VPH_C, S103.PH_VPH_C);

		// Current
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.CURRENT, //
				SCALE_FACTOR_3, //
				S111.A, S112.A, S113.A, S101.A, S102.A, S103.A);
		this.mapFirstPointToChannel(ElectricityMeter.ChannelId.CURRENT_L1, //
				SCALE_FACTOR_3, //
				S111.APH_A, S112.APH_A, S113.APH_A, S101.APH_A, S102.APH_A, S103.APH_A);
		this.mapFirstPointToChannel(ElectricityMeter.ChannelId.CURRENT_L2, //
				SCALE_FACTOR_3, //
				S111.APH_B, S112.APH_B, S113.APH_B, S101.APH_B, S102.APH_B, S103.APH_B);
		this.mapFirstPointToChannel(ElectricityMeter.ChannelId.CURRENT_L3, //
				SCALE_FACTOR_3, //
				S111.APH_C, S112.APH_C, S113.APH_C, S101.APH_C, S102.APH_C, S103.APH_C);
	}

	@Override
	protected void addBlock(int startAddress, SunSpecModel model, Priority priority) throws OpenemsException {
		super.addBlock(startAddress, model, priority);

		// Can we evaluate the InverterType from this Block?
		Stream.of(InverterType.values()) //
				.filter(type -> type.blocks.stream().anyMatch(t -> t.equals(model))) //
				.findFirst() //
				.ifPresent(type -> this.inverterType = type);
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
