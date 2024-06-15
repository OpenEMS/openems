package io.openems.edge.ess.fronius.gen24;

import java.util.Map;
import java.util.function.Consumer;

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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Fronius.GEN24", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class FroniusGen24EssImpl extends AbstractOpenemsSunSpecComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(FroniusGen24EssImpl.class);
	private static final int READ_FROM_MODBUS_BLOCK = 1;

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			// .put(DefaultSunSpecModel.S_103, Priority.LOW) //
			// .put(DefaultSunSpecModel.S_122, Priority.LOW) // GEN24
			// .put(DefaultSunSpecModel.S_123, Priority.LOW) // GEN24
			.put(DefaultSunSpecModel.S_124, Priority.HIGH) // GEN24
			.put(DefaultSunSpecModel.S_160, Priority.HIGH) // GEN24
			.build();

	@Reference
	private Power power;

	@Reference
	protected ConfigurationAdmin cm;

	private Config config;
	private boolean sunSpecInitialized = false;

	public FroniusGen24EssImpl() throws OpenemsException {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				FroniusGen24Ess.ChannelId.values() //
		);

	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id(), READ_FROM_MODBUS_BLOCK)) {
			return;
		}

		this.installUpdateActivePower();
		this.getStateChannel().setNextValue(Level.WARNING);
		if (this.config.capacity() != 0) {
			this._setCapacity(this.config.capacity());
		}

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.sunSpecInitialized = false;
	}

	@Override
	protected void onSunSpecInitializationCompleted() {
		this.sunSpecInitialized = true;
		this.logInfo(this.log, "SunSpec initialization finished. " + this.channels().size() + " Channels available.");

		this.mapFirstPointToChannel(SymmetricEss.ChannelId.MAX_APPARENT_POWER, ElementToChannelConverter.DIRECT_1_TO_1,
				DefaultSunSpecModel.S124.W_CHA_MAX);
		this.mapFirstPointToChannel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER,
				ElementToChannelConverter.INVERT, DefaultSunSpecModel.S124.W_CHA_MAX);
		this.mapFirstPointToChannel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER,
				ElementToChannelConverter.DIRECT_1_TO_1, DefaultSunSpecModel.S124.W_CHA_MAX);

		this.addCopyListener(//
				this.getSunSpecChannel(DefaultSunSpecModel.S124.CHA_GRI_SET).get(), //
				SymmetricEss.ChannelId.GRID_MODE //
		);

		// Fronius GEN24 uses S160 module 3 and 4 for battery charge and discharge power
		this.mapFirstPointToChannel(SymmetricEss.ChannelId.SOC, ElementToChannelConverter.DIRECT_1_TO_1,
				DefaultSunSpecModel.S124.CHA_STATE);
		this.mapFirstPointToChannel(FroniusGen24Ess.ChannelId.CHARGE_POWER, ElementToChannelConverter.DIRECT_1_TO_1,
				DefaultSunSpecModel.S160.MODULE_3_DCW);
		this.mapFirstPointToChannel(FroniusGen24Ess.ChannelId.DISCHARGE_POWER, ElementToChannelConverter.DIRECT_1_TO_1,
				DefaultSunSpecModel.S160.MODULE_4_DCW);

		try {
			this.addPowerConstraint("SetReactivePowerGreaterOrEquals", Phase.ALL, Pwr.ACTIVE,
					Relationship.GREATER_OR_EQUALS, -10);
			this.addPowerConstraint("SetReactivePowerLessOrEquals", Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS,
					10);
		} catch (OpenemsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void installUpdateActivePower() {

		// calculate active Power from charge power and discharge power
		final Consumer<Value<Integer>> activePowerCallback = (ignore) -> {
			this._setActivePower(TypeUtils.subtract(this.getDischargePowerChannel().value().get(),
					this.getChargePowerChannel().value().get()));
		};
		this.getChargePowerChannel().onSetNextValue(activePowerCallback);
		this.getDischargePowerChannel().onSetNextValue(activePowerCallback);
	}

	/**
	 * Adds a Copy-Listener. It listens on setNextValue() and copies the value to
	 * the target channel.
	 * 
	 * @param <T>             the Channel type
	 * @param sourceChannel   the source Channel
	 * @param targetChannelId the target ChannelId
	 */
	private <T> void addCopyListener(Channel<T> sourceChannel,
			io.openems.edge.common.channel.ChannelId targetChannelId) {
		Consumer<Value<T>> callback = (value) -> {
			Channel<T> targetChannel = this.channel(targetChannelId);
			targetChannel.setNextValue(value);
		};
		sourceChannel.onSetNextValue(callback);
		callback.accept(sourceChannel.getNextValue());
	}

	private static enum FroniusWorkaroundStates {
		AcceptPower, OpeningPowerWindow, ClosingPowerWindow
	}

	private static enum FroniusCase {
		IncreaseCharge, DecreaseCharge, IncreaseDischarge, DecreaseDischarge,
	}

	private FroniusWorkaroundStates reqState = FroniusWorkaroundStates.AcceptPower;
	private FroniusCase workaroundCase;
	private int powerToFollow = 0;

	private int surveillanceCntr = 0;

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {

		if (this.sunSpecInitialized == false) {
			return;
		}

		if (this.config.readOnly()) {
			return;
		}

		this.channel(FroniusGen24Ess.ChannelId.REQUESTED_ACTIVE_POWER).setNextValue(activePower);

		// From Modbus docs:
		// Additional Fronius description: Active hold/discharge/charge storage control
		// mode. Set the charge field to enable charging and the discharge field to
		// enable discharging.
		((BooleanWriteChannel) this.getSunSpecChannelOrError(DefaultSunSpecModel.S124_StorCtl_Mod.CHARGE))
				.setNextWriteValue(true);
		((BooleanWriteChannel) this.getSunSpecChannelOrError(DefaultSunSpecModel.S124_StorCtl_Mod.DISCHARGE))
				.setNextWriteValue(true);

		// 40356
		FloatWriteChannel activeChargePowerPercentageChannel = this
				.getSunSpecChannelOrError(DefaultSunSpecModel.S124.IN_W_RTE);
		// 40357
		FloatWriteChannel activeDischargePowerPercentageChannel = this
				.getSunSpecChannelOrError(DefaultSunSpecModel.S124.OUT_W_RTE);
		FloatReadChannel maxPowerChannel = this.getSunSpecChannelOrError(DefaultSunSpecModel.S124.W_CHA_MAX);

		try {
			Float maxPower = maxPowerChannel.value().getOrError();

			// { //debug
			// Float chargePowerPercentage =
			// activeChargePowerPercentageChannel.value().get();
			// Float dischargePowerPercentage =
			// activeDischargePowerPercentageChannel.value().get();
			// this.logInfo(this.log, "[" + this.reqState + "] percentages charge: " +
			// chargePowerPercentage + " discharge: " + dischargePowerPercentage);
			// }

			/*
			 * Note: -an exception is raised in 40356 when the old activePower was smaller
			 * than the current activePower -Reason: In General: Setting the power of a
			 * battery over two modbus channels includes an excellent race condition because
			 * there is a time window between setting IN_W_RTE and OUT_W_RTE
			 * 
			 * workaround:
			 * 
			 * start situation: percentage from both values is CURRENT end situation:
			 * percentage of both values is NEW
			 * 
			 * ActivePower charging -MaxPower neutral 0 discharging MaxPower
			 * 
			 * Channel t0 t1 t2 CASE
			 * -----------------------------------------------------------------------------
			 * -------------------------------------- 1) Increase Charging Power (CURRENT >
			 * NEW), e.g. CUR=-500 NEW=-1000 IN_W_RTE (charging) CUR NEW NEW
			 * OUT_W_RTE(discharg) CUR CUR NEW
			 * 
			 * 2) Decrease Charging Power (CUR < NEW), e.g. CUR=-500 NEW=-200 IN_W_RTE
			 * (charging) CUR CUR NEW OUT_W_RTE(discharg) CUR NEW NEW
			 * 
			 * 3) Increase Discharging Power (CUR < NEW), e.g. CUR=500 NEW=1000 IN_W_RTE
			 * (charging) CUR CUR NEW OUT_W_RTE(discharg) CUR NEW NEW
			 *
			 * 4) Decrease Discharging Power (CUR > NEW), e.g. CUR=500 NEW=200 IN_W_RTE
			 * (charging) CUR NEW NEW OUT_W_RTE(discharg) CUR CUR NEW
			 * 
			 * t0 <-> AcceptPower t1 <-> OpenPowerWindow t2 <-> ClosePowerWindow
			 */

			// NOTE: this is absolutely weird code!!!
			/*
			 * it can * IncreaseDischarge easily * IncreaseCharge easily * DecreaseDischarge
			 * power by setting the activePower to 0, wait until it is 0 and then it sets
			 * the new value * DecreaseCharge power by setting the activePower to 0, wait
			 * until it is 0 and then it sets the new value Reaction times are between 2s
			 * and 8s
			 */

			switch (this.reqState) {
			case AcceptPower:
				this.surveillanceCntr = 0;
				if (activePower == 0 || activePower == this.powerToFollow) {
					this.powerToFollow = activePower;
					Float activePowerPercentage = (this.powerToFollow / maxPower) * 100;
					activeDischargePowerPercentageChannel.setNextWriteValue(activePowerPercentage);
					activeChargePowerPercentageChannel.setNextWriteValue(-activePowerPercentage);
					break;

				} else if (activePower > this.powerToFollow) {
					this.powerToFollow = activePower;
					Float activePowerPercentage = (this.powerToFollow / maxPower) * 100;
					if (activePower < 0) {
						this.workaroundCase = FroniusCase.DecreaseCharge;
						activeDischargePowerPercentageChannel.setNextWriteValue(0.0f);

					} else {
						this.workaroundCase = FroniusCase.IncreaseDischarge;
						activeDischargePowerPercentageChannel.setNextWriteValue(activePowerPercentage);
					}

				} else {
					this.powerToFollow = activePower;
					Float activePowerPercentage = (this.powerToFollow / maxPower) * 100;
					if (activePower > 0) {
						this.workaroundCase = FroniusCase.DecreaseDischarge;
						activeChargePowerPercentageChannel.setNextWriteValue(0.0f);
						activeDischargePowerPercentageChannel.setNextWriteValue(0.0f);

					} else {
						this.workaroundCase = FroniusCase.IncreaseCharge;
						activeChargePowerPercentageChannel.setNextWriteValue(-activePowerPercentage);
					}

				}
				this.reqState = FroniusWorkaroundStates.OpeningPowerWindow;

				break;
			case OpeningPowerWindow:
				this.surveillanceCntr++;

				switch (this.workaroundCase) {
				case DecreaseCharge: {
					if (activeDischargePowerPercentageChannel.value().isDefined()) {
						Float actDischarge = activeDischargePowerPercentageChannel.value().get();
						Float actCharge = activeChargePowerPercentageChannel.value().get();
						if (actDischarge == 0) {
							if (actCharge != 0) {
								activeChargePowerPercentageChannel.setNextWriteValue(0.0f);
								break;
							}
						}
					}
					break;
				}
				case DecreaseDischarge: {
					if (activeChargePowerPercentageChannel.value().isDefined()) {
						if (activeDischargePowerPercentageChannel.value().isDefined()) {
							Float actCharge = activeChargePowerPercentageChannel.value().get();
							Float actDischarge = activeDischargePowerPercentageChannel.value().get();
							if (actCharge == 0 && actDischarge == 0) {

								if (Math.abs(this.getActivePower().orElse(0)) < 10) {
									this.reqState = FroniusWorkaroundStates.AcceptPower;
									this.powerToFollow = 0;
									return;
								}
							} else {
								activeChargePowerPercentageChannel.setNextWriteValue(0.0f);
								activeDischargePowerPercentageChannel.setNextWriteValue(0.0f);
							}
						}
					}
					break;
				}
				case IncreaseCharge: {
					if (activeChargePowerPercentageChannel.value().isDefined()) {
						Float actCharge = activeChargePowerPercentageChannel.value().get();
						Float activePowerPercentage = (this.powerToFollow / maxPower) * 100;
						if (Math.abs(actCharge.intValue()) == Math.abs(activePowerPercentage.intValue())) {
							activeDischargePowerPercentageChannel
									.setNextWriteValue(-activeChargePowerPercentageChannel.value().get());
							this.reqState = FroniusWorkaroundStates.ClosingPowerWindow;
						}
					}
					break;

				}
				case IncreaseDischarge: {
					if (activeDischargePowerPercentageChannel.value().isDefined()) {
						Float actDischarge = activeDischargePowerPercentageChannel.value().get();
						Float activePowerPercentage = (this.powerToFollow / maxPower) * 100;
						if (Math.abs(actDischarge.intValue()) == Math.abs(activePowerPercentage.intValue())) {
							activeChargePowerPercentageChannel
									.setNextWriteValue(-activeDischargePowerPercentageChannel.value().get());
							this.reqState = FroniusWorkaroundStates.ClosingPowerWindow;
						}
					}
					break;
				}
				} // inner Switch

				break;
			case ClosingPowerWindow:
				this.surveillanceCntr++;

				if (activeChargePowerPercentageChannel.value().isDefined()) {
					if (activeDischargePowerPercentageChannel.value().isDefined()) {
						Float actCharge = activeChargePowerPercentageChannel.value().get();
						Float actDischarge = activeDischargePowerPercentageChannel.value().get();
						Float activePowerPercentage = (this.powerToFollow / maxPower) * 100;
						if (Math.abs(actDischarge) == Math.abs(actCharge)
								&& Math.abs(actDischarge) == Math.abs(activePowerPercentage.intValue())) {
							this.reqState = FroniusWorkaroundStates.AcceptPower;

							// done, now speed up process by immediately applying new power
							this.applyPower(activePower, reactivePower);
							return;
						}
					}
				}
				break;
			}

			if (this.surveillanceCntr > 20) {
				// something has gone wrong, reinitialize statemachine ...
				this.reqState = FroniusWorkaroundStates.AcceptPower;
				this.getStateChannel().setNextValue(Level.WARNING);
				this.applyPower(0, 0);
				return;
			}

			this.getStateChannel().setNextValue(Level.OK);
		} catch (Exception e) {
			this.getStateChannel().setNextValue(Level.WARNING);
		}
	}

	public Channel<Integer> getChargePowerChannel() {
		return this.channel(FroniusGen24Ess.ChannelId.CHARGE_POWER);
	}

	public Channel<Integer> getDischargePowerChannel() {
		return this.channel(FroniusGen24Ess.ChannelId.DISCHARGE_POWER);
	}

	@Override
	public String debugLog() {
		Float actCharge = 99999.9f;
		Float actDischarge = 99999.9f;
		try {
			FloatWriteChannel activeChargePowerPercentageChannel = this
					.getSunSpecChannelOrError(DefaultSunSpecModel.S124.IN_W_RTE);
			FloatWriteChannel activeDischargePowerPercentageChannel = this
					.getSunSpecChannelOrError(DefaultSunSpecModel.S124.OUT_W_RTE);
			actCharge = activeChargePowerPercentageChannel.value().get();
			actDischarge = activeDischargePowerPercentageChannel.value().get();
		} catch (Exception e) {
			;
		}
		;
		return "SoC:" + this.getSoc().asString() //
				+ "|Req:" + this.getDebugSetActivePower().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|IN_W_RTE: " + actCharge + "|OUT_W_RTE: " + actDischarge + "|Allowed:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit() + ";"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asString() //
				+ "|" + this.getGridModeChannel().value().asOptionString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

}
