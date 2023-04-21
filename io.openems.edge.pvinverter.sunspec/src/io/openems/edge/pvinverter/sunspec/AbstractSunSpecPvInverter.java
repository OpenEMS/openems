package io.openems.edge.pvinverter.sunspec;

import java.util.Map;
import java.util.Optional;

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
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

public abstract class AbstractSunSpecPvInverter extends AbstractOpenemsSunSpecComponent
		implements SunSpecPvInverter, ManagedSymmetricPvInverter, ElectricityMeter, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(AbstractSunSpecPvInverter.class);
	private final SetPvLimitHandler setPvLimitHandler = new SetPvLimitHandler(this);

	private boolean readOnly;
	private boolean isSinglePhase;
	private Phase phase;

	public AbstractSunSpecPvInverter(Map<SunSpecModel, Priority> activeModels,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) throws OpenemsException {
		super(activeModels, firstInitialChannelIds, furtherInitialChannelIds);
		this._setActiveConsumptionEnergy(0);
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
	 * <pre>
	 * property = { //
	 *   EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
	 * }
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

		this.channel(SunSpecPvInverter.ChannelId.WRONG_PHASE_CONFIGURED)
				.setNextValue(this.isSinglePhase() ? this.phase == Phase.ALL : this.phase != Phase.ALL);

		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.FREQUENCY, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				DefaultSunSpecModel.S111.HZ, DefaultSunSpecModel.S112.HZ, DefaultSunSpecModel.S113.HZ,
				DefaultSunSpecModel.S101.HZ, DefaultSunSpecModel.S102.HZ, DefaultSunSpecModel.S103.HZ);

		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S111.W, DefaultSunSpecModel.S112.W, DefaultSunSpecModel.S113.W,
				DefaultSunSpecModel.S101.W, DefaultSunSpecModel.S102.W, DefaultSunSpecModel.S103.W);

		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.REACTIVE_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S111.V_AR, DefaultSunSpecModel.S112.V_AR, DefaultSunSpecModel.S113.V_AR,
				DefaultSunSpecModel.S101.V_AR, DefaultSunSpecModel.S102.V_AR, DefaultSunSpecModel.S103.V_AR);

		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S111.WH, DefaultSunSpecModel.S112.WH, DefaultSunSpecModel.S113.WH,
				DefaultSunSpecModel.S101.WH, DefaultSunSpecModel.S102.WH, DefaultSunSpecModel.S103.WH);

		this.mapFirstPointToChannel(//
				ManagedSymmetricPvInverter.ChannelId.MAX_APPARENT_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S120.W_RTG);

		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.CURRENT, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				DefaultSunSpecModel.S111.A, DefaultSunSpecModel.S112.A, DefaultSunSpecModel.S113.A,
				DefaultSunSpecModel.S101.A, DefaultSunSpecModel.S102.A, DefaultSunSpecModel.S103.A);

		if (!this.isSinglePhase) {
			this.mapFirstPointToChannel(//
					ElectricityMeter.ChannelId.VOLTAGE, //
					ElementToChannelConverter.SCALE_FACTOR_3, //
					DefaultSunSpecModel.S112.PH_VPH_A, DefaultSunSpecModel.S112.PH_VPH_B,
					DefaultSunSpecModel.S112.PH_VPH_C, //
					DefaultSunSpecModel.S113.PH_VPH_A, DefaultSunSpecModel.S113.PH_VPH_B,
					DefaultSunSpecModel.S113.PH_VPH_C, //
					DefaultSunSpecModel.S102.PH_VPH_A, DefaultSunSpecModel.S102.PH_VPH_B,
					DefaultSunSpecModel.S102.PH_VPH_C, //
					DefaultSunSpecModel.S103.PH_VPH_A, DefaultSunSpecModel.S103.PH_VPH_B,
					DefaultSunSpecModel.S103.PH_VPH_C);
			return;
		}

		switch (this.phase) {
		case ALL:
			// use l1 when 'ALL' is configured and its not a tree phase inverter
		case L1:
			this.mapFirstPointToChannel(ElectricityMeter.ChannelId.VOLTAGE_L1, //
					ElementToChannelConverter.DIRECT_1_TO_1, //
					DefaultSunSpecModel.S101.PH_VPH_A, DefaultSunSpecModel.S111.PH_VPH_A);
			break;
		case L2:
			this.mapFirstPointToChannel(ElectricityMeter.ChannelId.VOLTAGE_L2, //
					ElementToChannelConverter.DIRECT_1_TO_1, //
					DefaultSunSpecModel.S101.PH_VPH_B, DefaultSunSpecModel.S111.PH_VPH_B);
			break;
		case L3:
			this.mapFirstPointToChannel(ElectricityMeter.ChannelId.VOLTAGE_L3, //
					ElementToChannelConverter.DIRECT_1_TO_1, //
					DefaultSunSpecModel.S101.PH_VPH_C, DefaultSunSpecModel.S111.PH_VPH_C);
			break;
		}

		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.VOLTAGE, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S101.PH_VPH_A, DefaultSunSpecModel.S111.PH_VPH_A, //
				DefaultSunSpecModel.S101.PH_VPH_B, DefaultSunSpecModel.S111.PH_VPH_B, //
				DefaultSunSpecModel.S101.PH_VPH_C, DefaultSunSpecModel.S111.PH_VPH_C);

	}

	@Override
	protected void addBlock(int startAddress, SunSpecModel model, Priority priority) throws OpenemsException {
		super.addBlock(startAddress, model, priority);

		if (Lists.newArrayList(DefaultSunSpecModel.S_101, //
				DefaultSunSpecModel.S_111) //
				.stream() //
				.anyMatch(t -> t.equals(model))) {
			// single phase
			this.isSinglePhase = true;
		} else if (Lists.newArrayList(DefaultSunSpecModel.S_102, //
				DefaultSunSpecModel.S_112) //
				.stream() //
				.anyMatch(t -> t.equals(model))) {
			// split Phase
			this.isSinglePhase = false;
		} else if (Lists.newArrayList(DefaultSunSpecModel.S_103, //
				DefaultSunSpecModel.S_113) //
				.stream() //
				.anyMatch(t -> t.equals(model))) {
			// three Phase
			this.isSinglePhase = false;
		}

	}

	protected final boolean isSinglePhase() {
		return this.isSinglePhase;
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
