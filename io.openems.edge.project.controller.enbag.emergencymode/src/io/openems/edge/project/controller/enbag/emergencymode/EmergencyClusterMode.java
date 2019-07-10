package io.openems.edge.project.controller.enbag.emergencymode;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.EmergencyClusterMode", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class EmergencyClusterMode extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	/**
	 * Wait between two consecutive switches of Q1/Q2 or Q3/Q4.
	 */
	private static final int WAIT_FOR_RELAY_SWITCH_SECONDS = 10;

	private final Logger log = LoggerFactory.getLogger(EmergencyClusterMode.class);
	private final Clock clock;

	private Config config;

	private ChannelAddress q1Ess1SupplyUps;
	private ChannelAddress q2Ess2SupplyUps;
	private ChannelAddress q3PvOffGrid;
	private ChannelAddress q4PvOnGrid;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ConfigurationAdmin cm;

	public EmergencyClusterMode() {
		this(Clock.systemDefaultZone());
	}

	protected EmergencyClusterMode(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ThisChannelId.values() //
		);
		this.clock = clock;
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.q1Ess1SupplyUps = ChannelAddress.fromString(config.q1ChannelAddress());
		this.q2Ess2SupplyUps = ChannelAddress.fromString(config.q2ChannelAddress());
		this.q3PvOffGrid = ChannelAddress.fromString(config.q3ChannelAddress());
		this.q4PvOnGrid = ChannelAddress.fromString(config.q4ChannelAddress());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		switch (this.getGridMode()) {
		case UNDEFINED:
			/*
			 * Grid-Mode is undefined -> wait till we have some clear information
			 */
			break;

		case OFF_GRID:
			/*
			 * Both ESS are Off-Grid
			 */
			this.handleOffGridState();
			break;

		case ON_GRID:
			/*
			 * Both ESS are On-Grid
			 */
			this.handleOnGridState();
			break;
		}
	}

	/**
	 * Both Ess are in Off-Grid Mode.
	 * 
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	private void handleOffGridState() throws IllegalArgumentException, OpenemsNamedException {
		switch (this.getBatteryAndPvState()) {
		case ESS1_FULL__ESS2_NORMAL__PV_SUFFICIENT:
		case ESS1_FULL__ESS2_NORMAL__PV_NOT_SUFFICIENT:
		case ESS1_FULL__ESS2_NORMAL__PV_UNKNOWN:
		case ESS1_FULL__ESS2_LOW__PV_SUFFICIENT:
		case ESS1_FULL__ESS2_EMPTY__PV_SUFFICIENT:
		case ESS1_NORMAL__ESS2_FULL__PV_NOT_SUFFICIENT:
		case ESS1_NORMAL__ESS2_NORMAL__PV_SUFFICIENT:
		case ESS1_NORMAL__ESS2_NORMAL__PV_NOT_SUFFICIENT:
		case ESS1_NORMAL__ESS2_NORMAL__PV_UNKNOWN:
		case ESS1_NORMAL__ESS2_LOW__PV_SUFFICIENT:
		case ESS1_NORMAL__ESS2_EMPTY__PV_SUFFICIENT:
		case ESS1_LOW__ESS2_NORMAL__PV_SUFFICIENT:
		case ESS1_LOW__ESS2_NORMAL__PV_NOT_SUFFICIENT:
		case ESS1_LOW__ESS2_NORMAL__PV_UNKNOWN:
		case ESS1_LOW__ESS2_LOW__PV_SUFFICIENT:
		case ESS1_LOW__ESS2_LOW__PV_NOT_SUFFICIENT:
		case ESS1_LOW__ESS2_LOW__PV_UNKNOWN:
		case ESS1_LOW__ESS2_EMPTY__PV_SUFFICIENT:
		case ESS1_EMPTY__ESS2_NORMAL__PV_SUFFICIENT:
		case ESS1_EMPTY__ESS2_NORMAL__PV_NOT_SUFFICIENT:
		case ESS1_EMPTY__ESS2_NORMAL__PV_UNKNOWN:
		case ESS1_EMPTY__ESS2_LOW__PV_SUFFICIENT:
		case ESS1_EMPTY__ESS2_LOW__PV_NOT_SUFFICIENT:
		case ESS1_EMPTY__ESS2_LOW__PV_UNKNOWN:
		case ESS1_EMPTY__ESS2_EMPTY__PV_SUFFICIENT:
		case ESS1_EMPTY__ESS2_EMPTY__PV_NOT_SUFFICIENT:
		case ESS1_EMPTY__ESS2_EMPTY__PV_UNKNOWN:
			this.useBattery(Battery.ESS2);
			this.offgridPv(PvConnected.YES);
			break;

		case ESS1_FULL__ESS2_FULL__PV_SUFFICIENT:
		case ESS1_FULL__ESS2_FULL__PV_NOT_SUFFICIENT:
		case ESS1_FULL__ESS2_FULL__PV_UNKNOWN:
		case ESS1_NORMAL__ESS2_FULL__PV_UNKNOWN:
		case ESS1_LOW__ESS2_FULL__PV_NOT_SUFFICIENT:
		case ESS1_LOW__ESS2_FULL__PV_UNKNOWN:
		case ESS1_EMPTY__ESS2_FULL__PV_NOT_SUFFICIENT:
		case ESS1_EMPTY__ESS2_FULL__PV_UNKNOWN:
		case UNDEFINED:
			this.useBattery(Battery.ESS2);
			this.offgridPv(PvConnected.NO);
			break;

		case ESS1_FULL__ESS2_LOW__PV_NOT_SUFFICIENT:
		case ESS1_FULL__ESS2_EMPTY__PV_NOT_SUFFICIENT:
		case ESS1_NORMAL__ESS2_FULL__PV_SUFFICIENT:
		case ESS1_NORMAL__ESS2_LOW__PV_NOT_SUFFICIENT:
		case ESS1_NORMAL__ESS2_LOW__PV_UNKNOWN:
		case ESS1_NORMAL__ESS2_EMPTY__PV_NOT_SUFFICIENT:
		case ESS1_NORMAL__ESS2_EMPTY__PV_UNKNOWN:
		case ESS1_LOW__ESS2_FULL__PV_SUFFICIENT:
		case ESS1_LOW__ESS2_EMPTY__PV_NOT_SUFFICIENT:
		case ESS1_LOW__ESS2_EMPTY__PV_UNKNOWN:
		case ESS1_EMPTY__ESS2_FULL__PV_SUFFICIENT:
			this.useBattery(Battery.ESS1);
			this.offgridPv(PvConnected.YES);
			break;

		case ESS1_FULL__ESS2_LOW__PV_UNKNOWN:
		case ESS1_FULL__ESS2_EMPTY__PV_UNKNOWN:
			this.useBattery(Battery.ESS1);
			this.offgridPv(PvConnected.NO);
			break;
		}

		// Limit PV production
		this.limitPvInOffgrid();
	}

	/**
	 * Both Ess are in On-Grid Mode.
	 * 
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	private void handleOnGridState() throws IllegalArgumentException, OpenemsNamedException {
		if (this.isQ4PvOnGridClosed() && !this.isQ3PvOffGridClosed()
				&& (this.isQ1Ess1SupplyUpsClosed() ^ this.isQ2Ess2SupplyUpsClosed())) {
			// Nothing to do. Already switched to On-Grid.
			return;
		}

		// Switch to On-Grid
		this.setOutput(this.q1Ess1SupplyUps, Operation.OPEN);
		this.setOutput(this.q2Ess2SupplyUps, Operation.CLOSE);
		this.setOutput(this.q3PvOffGrid, Operation.OPEN);
		this.setOutput(this.q4PvOnGrid, Operation.CLOSE);
		this.NolimitPvInOngrid();
	}

	private enum PvConnected {
		YES, NO
	}

	private LocalDateTime lastSwitchOffgridPv = LocalDateTime.MIN;

	/**
	 * Sets whether the PV should be connected to ESS in Off-Grid.
	 * 
	 * @param pvConnected Yes or No
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	private void offgridPv(PvConnected pvConnected) throws IllegalArgumentException, OpenemsNamedException {
		// Wait at least 10 seconds between switching outputs
		if (this.lastSwitchOffgridPv
				.isAfter(LocalDateTime.now(this.clock).minusSeconds(WAIT_FOR_RELAY_SWITCH_SECONDS))) {
			return;
		}
		lastSwitchOffgridPv = LocalDateTime.now(this.clock);

		// Always disconnect PV from Grid
		this.setOutput(this.q4PvOnGrid, Operation.OPEN);
		if (this.isQ4PvOnGridClosed()) {
			return;
		}

		if (pvConnected == PvConnected.YES) {
			this.setOutput(this.q3PvOffGrid, Operation.CLOSE);
		} else {
			this.setOutput(this.q3PvOffGrid, Operation.OPEN);
		}
	}

	private LocalDateTime lastSwitchUseBattery = LocalDateTime.MIN;

	/**
	 * Use the given Battery to supply UPS in Off-Grid.
	 * 
	 * @param ess the Battery
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	private void useBattery(Battery ess) throws IllegalArgumentException, OpenemsNamedException {
		// Wait at least 10 seconds between switching outputs
		if (this.lastSwitchUseBattery
				.isAfter(LocalDateTime.now(this.clock).minusSeconds(WAIT_FOR_RELAY_SWITCH_SECONDS))) {
			return;
		}
		lastSwitchUseBattery = LocalDateTime.now(this.clock);

		switch (ess) {
		case ESS1:
			this.setOutput(this.q2Ess2SupplyUps, Operation.OPEN);
			if (!this.isQ2Ess2SupplyUpsClosed()) {
				this.setOutput(this.q1Ess1SupplyUps, Operation.CLOSE);
			}
			break;
		case ESS2:
			this.setOutput(this.q1Ess1SupplyUps, Operation.OPEN);
			if (!this.isQ1Ess1SupplyUpsClosed()) {
				this.setOutput(this.q2Ess2SupplyUps, Operation.CLOSE);
			}
			break;
		case UNDEFINED:
			break;
		}
	}

	/**
	 * Don't Limit PV production in On-Grid.
	 * 
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	private void NolimitPvInOngrid() throws IllegalArgumentException, OpenemsNamedException {
		ManagedSymmetricPvInverter pvInverter;
		try {
			pvInverter = this.componentManager.getComponent(this.config.pvInverter_id());
			// TODO 137500 needs to be read from registers
			pvInverter.getActivePowerLimit().setNextWriteValue(137500);
		} catch (OpenemsNamedException e) {
			// ignore
		}
	}

	/**
	 * Limit PV production in Off-Grid to 35.000 W.
	 * 
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	private void limitPvInOffgrid() throws IllegalArgumentException, OpenemsNamedException {
		ManagedSymmetricPvInverter pvInverter;
		int power = Integer.MAX_VALUE;
		try {
			pvInverter = this.componentManager.getComponent(this.config.pvInverter_id());
			pvInverter.getActivePowerLimit().setNextWriteValue(this.config.OFFGRID_PV_LIMIT());
			power = pvInverter.getActivePower().value().orElse(Integer.MAX_VALUE);
		} catch (OpenemsNamedException e) {
			// ignore
		}
		if (power > this.config.OFFGRID_PV_LIMIT_FAULT()) {
			// Limit did not work: disconnect PV
			this.setOutput(q3PvOffGrid, Operation.OPEN);
		}
	}

	/**
	 * Gets the Grid-Mode of both ESS.
	 * 
	 * @return the Grid-Mode
	 */
	private GridMode getGridMode() {
		SymmetricEss ess1;
		SymmetricEss ess2;
		try {
			ess1 = this.componentManager.getComponent(this.config.ess1_id());
			ess2 = this.componentManager.getComponent(this.config.ess2_id());
		} catch (OpenemsNamedException e) {
			// ignore
			return GridMode.UNDEFINED;
		}

		GridMode ess1GridMode = ess1.getGridMode().value().asEnum();
		GridMode ess2GridMode = ess2.getGridMode().value().asEnum();
		if (
		// At least Ess1 is On-Grid
		(ess1GridMode == GridMode.ON_GRID && ess2GridMode != GridMode.OFF_GRID) //
				// At least Ess2 is On-Grid
				|| (ess2GridMode == GridMode.ON_GRID && ess1GridMode != GridMode.OFF_GRID)) {
			return GridMode.ON_GRID;
		} else if (
		// At least Ess1 is Off-Grid
		(ess1GridMode == GridMode.OFF_GRID && ess2GridMode != GridMode.ON_GRID) //
				// At least Ess2 is Off-Grid
				|| (ess2GridMode == GridMode.OFF_GRID && ess1GridMode != GridMode.ON_GRID)) {
			return GridMode.OFF_GRID;
		} else {
			return GridMode.UNDEFINED;
		}
	}

	private boolean isQ1Ess1SupplyUpsClosed() throws IllegalArgumentException, OpenemsNamedException {
		BooleanWriteChannel q1Ess1SupplyUps = this.componentManager.getChannel(this.q1Ess1SupplyUps);
		return q1Ess1SupplyUps.value().orElse(false);
	}

	private boolean isQ2Ess2SupplyUpsClosed() throws IllegalArgumentException, OpenemsNamedException {
		BooleanWriteChannel q2Ess2SupplyUps = this.componentManager.getChannel(this.q2Ess2SupplyUps);
		return !q2Ess2SupplyUps.value().orElse(true);
	}

	private boolean isQ3PvOffGridClosed() throws IllegalArgumentException, OpenemsNamedException {
		BooleanWriteChannel q3PvOffGrid = this.componentManager.getChannel(this.q3PvOffGrid);
		return q3PvOffGrid.value().orElse(false);
	}

	private boolean isQ4PvOnGridClosed() throws IllegalArgumentException, OpenemsNamedException {
		BooleanWriteChannel q4PvOnGrid = this.componentManager.getChannel(this.q4PvOnGrid);
		return q4PvOnGrid.value().orElse(false);
	}

	/**
	 * Gets the State of Ess1, Ess2 and PV.
	 * 
	 * @return the State
	 */
	private BatteryAndPvState getBatteryAndPvState() {
		OneBatteryState ess1State = this.getOneBatteryState(config.ess1_id());
		OneBatteryState ess2State = this.getOneBatteryState(config.ess2_id());
		PvState pvState = this.getPvState();
		switch (ess1State) {
		case EMPTY:
			switch (ess2State) {
			case EMPTY:
			case UNDEFINED:
				switch (pvState) {
				case NOT_SUFFICIENT:
					return BatteryAndPvState.ESS1_EMPTY__ESS2_EMPTY__PV_NOT_SUFFICIENT;
				case SUFFICIENT:
					return BatteryAndPvState.ESS1_EMPTY__ESS2_EMPTY__PV_SUFFICIENT;
				case UNKNOWN:
					return BatteryAndPvState.ESS1_EMPTY__ESS2_EMPTY__PV_UNKNOWN;
				}
			case LOW:
				switch (pvState) {
				case NOT_SUFFICIENT:
					return BatteryAndPvState.ESS1_EMPTY__ESS2_LOW__PV_NOT_SUFFICIENT;
				case SUFFICIENT:
					return BatteryAndPvState.ESS1_EMPTY__ESS2_LOW__PV_SUFFICIENT;
				case UNKNOWN:
					return BatteryAndPvState.ESS1_EMPTY__ESS2_LOW__PV_UNKNOWN;
				}
			case NORMAL:
				switch (pvState) {
				case NOT_SUFFICIENT:
					return BatteryAndPvState.ESS1_EMPTY__ESS2_NORMAL__PV_NOT_SUFFICIENT;
				case SUFFICIENT:
					return BatteryAndPvState.ESS1_EMPTY__ESS2_NORMAL__PV_SUFFICIENT;
				case UNKNOWN:
					return BatteryAndPvState.ESS1_EMPTY__ESS2_NORMAL__PV_UNKNOWN;
				}
			case FULL:
				switch (pvState) {
				case NOT_SUFFICIENT:
					return BatteryAndPvState.ESS1_EMPTY__ESS2_FULL__PV_NOT_SUFFICIENT;
				case SUFFICIENT:
					return BatteryAndPvState.ESS1_EMPTY__ESS2_FULL__PV_SUFFICIENT;
				case UNKNOWN:
					return BatteryAndPvState.ESS1_EMPTY__ESS2_FULL__PV_UNKNOWN;
				}
			}
			break;
		case LOW:
			switch (ess2State) {
			case EMPTY:
				switch (pvState) {
				case NOT_SUFFICIENT:
					return BatteryAndPvState.ESS1_LOW__ESS2_EMPTY__PV_NOT_SUFFICIENT;
				case SUFFICIENT:
					return BatteryAndPvState.ESS1_LOW__ESS2_EMPTY__PV_SUFFICIENT;
				case UNKNOWN:
					return BatteryAndPvState.ESS1_LOW__ESS2_EMPTY__PV_UNKNOWN;
				}
			case LOW:
			case UNDEFINED:
				switch (pvState) {
				case NOT_SUFFICIENT:
					return BatteryAndPvState.ESS1_LOW__ESS2_LOW__PV_NOT_SUFFICIENT;
				case SUFFICIENT:
					return BatteryAndPvState.ESS1_LOW__ESS2_LOW__PV_SUFFICIENT;
				case UNKNOWN:
					return BatteryAndPvState.ESS1_LOW__ESS2_LOW__PV_UNKNOWN;
				}
			case NORMAL:
				switch (pvState) {
				case NOT_SUFFICIENT:
					return BatteryAndPvState.ESS1_LOW__ESS2_NORMAL__PV_NOT_SUFFICIENT;
				case SUFFICIENT:
					return BatteryAndPvState.ESS1_LOW__ESS2_NORMAL__PV_SUFFICIENT;
				case UNKNOWN:
					return BatteryAndPvState.ESS1_LOW__ESS2_NORMAL__PV_UNKNOWN;
				}
			case FULL:
				switch (pvState) {
				case NOT_SUFFICIENT:
					return BatteryAndPvState.ESS1_LOW__ESS2_FULL__PV_NOT_SUFFICIENT;
				case SUFFICIENT:
					return BatteryAndPvState.ESS1_LOW__ESS2_FULL__PV_SUFFICIENT;
				case UNKNOWN:
					return BatteryAndPvState.ESS1_LOW__ESS2_FULL__PV_UNKNOWN;
				}
			}
			break;
		case NORMAL:
			switch (ess2State) {
			case EMPTY:
				switch (pvState) {
				case NOT_SUFFICIENT:
					return BatteryAndPvState.ESS1_NORMAL__ESS2_EMPTY__PV_NOT_SUFFICIENT;
				case SUFFICIENT:
					return BatteryAndPvState.ESS1_NORMAL__ESS2_EMPTY__PV_SUFFICIENT;
				case UNKNOWN:
					return BatteryAndPvState.ESS1_NORMAL__ESS2_EMPTY__PV_UNKNOWN;
				}
			case LOW:
			case UNDEFINED:
				switch (pvState) {
				case NOT_SUFFICIENT:
					return BatteryAndPvState.ESS1_NORMAL__ESS2_LOW__PV_NOT_SUFFICIENT;
				case SUFFICIENT:
					return BatteryAndPvState.ESS1_NORMAL__ESS2_LOW__PV_SUFFICIENT;
				case UNKNOWN:
					return BatteryAndPvState.ESS1_NORMAL__ESS2_LOW__PV_UNKNOWN;
				}
			case NORMAL:
				switch (pvState) {
				case NOT_SUFFICIENT:
					return BatteryAndPvState.ESS1_NORMAL__ESS2_NORMAL__PV_NOT_SUFFICIENT;
				case SUFFICIENT:
					return BatteryAndPvState.ESS1_NORMAL__ESS2_NORMAL__PV_SUFFICIENT;
				case UNKNOWN:
					return BatteryAndPvState.ESS1_NORMAL__ESS2_NORMAL__PV_UNKNOWN;
				}
			case FULL:
				switch (pvState) {
				case NOT_SUFFICIENT:
					return BatteryAndPvState.ESS1_NORMAL__ESS2_FULL__PV_NOT_SUFFICIENT;
				case SUFFICIENT:
					return BatteryAndPvState.ESS1_NORMAL__ESS2_FULL__PV_SUFFICIENT;
				case UNKNOWN:
					return BatteryAndPvState.ESS1_NORMAL__ESS2_FULL__PV_UNKNOWN;
				}
			}
			break;
		case FULL:
			switch (ess2State) {
			case EMPTY:
				switch (pvState) {
				case NOT_SUFFICIENT:
					return BatteryAndPvState.ESS1_FULL__ESS2_EMPTY__PV_NOT_SUFFICIENT;
				case SUFFICIENT:
					return BatteryAndPvState.ESS1_FULL__ESS2_EMPTY__PV_SUFFICIENT;
				case UNKNOWN:
					return BatteryAndPvState.ESS1_FULL__ESS2_EMPTY__PV_UNKNOWN;
				}
			case LOW:
				switch (pvState) {
				case NOT_SUFFICIENT:
					return BatteryAndPvState.ESS1_FULL__ESS2_LOW__PV_NOT_SUFFICIENT;
				case SUFFICIENT:
					return BatteryAndPvState.ESS1_FULL__ESS2_LOW__PV_SUFFICIENT;
				case UNKNOWN:
					return BatteryAndPvState.ESS1_FULL__ESS2_LOW__PV_UNKNOWN;
				}
			case NORMAL:
				switch (pvState) {
				case NOT_SUFFICIENT:
					return BatteryAndPvState.ESS1_FULL__ESS2_NORMAL__PV_NOT_SUFFICIENT;
				case SUFFICIENT:
					return BatteryAndPvState.ESS1_FULL__ESS2_NORMAL__PV_SUFFICIENT;
				case UNKNOWN:
					return BatteryAndPvState.ESS1_FULL__ESS2_NORMAL__PV_UNKNOWN;
				}
			case FULL:
			case UNDEFINED:
				switch (pvState) {
				case NOT_SUFFICIENT:
					return BatteryAndPvState.ESS1_FULL__ESS2_FULL__PV_NOT_SUFFICIENT;
				case SUFFICIENT:
					return BatteryAndPvState.ESS1_FULL__ESS2_FULL__PV_SUFFICIENT;
				case UNKNOWN:
					return BatteryAndPvState.ESS1_FULL__ESS2_FULL__PV_UNKNOWN;
				}
			}
			break;
		case UNDEFINED:
			switch (ess2State) {
			case EMPTY:
				switch (pvState) {
				case NOT_SUFFICIENT:
					return BatteryAndPvState.ESS1_EMPTY__ESS2_EMPTY__PV_NOT_SUFFICIENT;
				case SUFFICIENT:
					return BatteryAndPvState.ESS1_EMPTY__ESS2_EMPTY__PV_SUFFICIENT;
				case UNKNOWN:
					return BatteryAndPvState.ESS1_EMPTY__ESS2_EMPTY__PV_UNKNOWN;
				}
			case LOW:
				switch (pvState) {
				case NOT_SUFFICIENT:
					return BatteryAndPvState.ESS1_LOW__ESS2_LOW__PV_NOT_SUFFICIENT;
				case SUFFICIENT:
					return BatteryAndPvState.ESS1_LOW__ESS2_LOW__PV_SUFFICIENT;
				case UNKNOWN:
					return BatteryAndPvState.ESS1_LOW__ESS2_LOW__PV_UNKNOWN;
				}
			case NORMAL:
				switch (pvState) {
				case NOT_SUFFICIENT:
					return BatteryAndPvState.ESS1_NORMAL__ESS2_NORMAL__PV_NOT_SUFFICIENT;
				case SUFFICIENT:
					return BatteryAndPvState.ESS1_NORMAL__ESS2_NORMAL__PV_SUFFICIENT;
				case UNKNOWN:
					return BatteryAndPvState.ESS1_NORMAL__ESS2_NORMAL__PV_UNKNOWN;
				}
			case FULL:
				switch (pvState) {
				case NOT_SUFFICIENT:
					return BatteryAndPvState.ESS1_FULL__ESS2_FULL__PV_NOT_SUFFICIENT;
				case SUFFICIENT:
					return BatteryAndPvState.ESS1_FULL__ESS2_FULL__PV_SUFFICIENT;
				case UNKNOWN:
					return BatteryAndPvState.ESS1_FULL__ESS2_FULL__PV_UNKNOWN;
				}
			case UNDEFINED:
				return BatteryAndPvState.UNDEFINED;
			}
			break;
		}
		return BatteryAndPvState.UNDEFINED;
	}

	private enum OneBatteryState {
		EMPTY, LOW, NORMAL, FULL, UNDEFINED;
	}

	/**
	 * Gets the Battery-State of one Ess.
	 * 
	 * @param essId the ID of the Ess
	 * @return the Battery-State
	 */
	private OneBatteryState getOneBatteryState(String essId) {
		ManagedSymmetricEss ess;
		try {
			ess = this.componentManager.getComponent(essId);
		} catch (OpenemsNamedException e) {
			return OneBatteryState.UNDEFINED;
		}

		Optional<Integer> allowedChargePower = ess.getAllowedCharge().value().asOptional();
		Optional<Integer> allowedDischargePower = ess.getAllowedDischarge().value().asOptional();
		Optional<Integer> soc = ess.getSoc().value().asOptional();

		if (allowedChargePower.isPresent() && allowedChargePower.get() == 0) {
			return OneBatteryState.FULL;
		} else if (allowedDischargePower.isPresent() && allowedDischargePower.get() == 0) {
			return OneBatteryState.EMPTY;
		} else if (soc.isPresent()) {
			if (soc.get() <= 5) {
				return OneBatteryState.LOW;
			} else {
				return OneBatteryState.NORMAL;
			}
		} else {
			return OneBatteryState.UNDEFINED;
		}
	}

	/**
	 * Gets the current State of the PV.
	 * 
	 * @return the PvState
	 */
	private PvState getPvState() {
		ManagedSymmetricPvInverter pvInverter;
		try {
			pvInverter = this.componentManager.getComponent(this.config.pvInverter_id());
		} catch (OpenemsNamedException e) {
			// ignore
			return PvState.UNKNOWN;
		}

		Channel<Integer> pvInverterChannel = pvInverter.getActivePower();
		Optional<Integer> powerOpt = pvInverterChannel.value().asOptional();

		if (!powerOpt.isPresent()) {
			return PvState.UNKNOWN;
		}

		int power = powerOpt.get();
		if (power <= 0) {
			return PvState.NOT_SUFFICIENT;
		} else if (power >= this.config.pvSufficientPower()) {
			return PvState.SUFFICIENT;
		} else {
			return PvState.UNKNOWN;
		}
	}

	/**
	 * Set Switch to Close or Open Operation.
	 * 
	 * @param channelAddress the Address of the BooleanWriteChannel
	 * @param operation      Close --> Make line connection; <br/>
	 *                       Open --> Make line disconnection
	 * @return true if the output was actually switched; false if it had already
	 *         been in the desired state
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	private boolean setOutput(ChannelAddress channelAddress, Operation operation)
			throws IllegalArgumentException, OpenemsNamedException {
		boolean switchedOutput = false;
		BooleanWriteChannel channel = this.componentManager.getChannel(channelAddress);
		switch (operation) {
		case CLOSE:
			if (channel.address().equals(this.q2Ess2SupplyUps)) {
				switchedOutput = this.setOutput(channel, false);
			} else {
				switchedOutput = this.setOutput(channel, true);
			}
			if (switchedOutput) {
				log.info("Set output [" + channel.address() + "] CLOSE.");
			}
			break;

		case OPEN:
			if (channel.address().equals(this.q2Ess2SupplyUps)) {
				switchedOutput = this.setOutput(channel, true);
			} else {
				switchedOutput = this.setOutput(channel, false);
			}
			if (switchedOutput) {
				log.info("Set output [" + channel.address() + "] OPEN.");
			}
			break;
		case UNDEFINED:
			break;
		}
		return switchedOutput;
	}

	/**
	 * Sets the Output.
	 * 
	 * @param channel the BooleanWriteChannel
	 * @param value   true to set the output, false to unset it
	 * @return true if the output was actually switched; false if it had already
	 *         been in the desired state
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	private boolean setOutput(BooleanWriteChannel channel, boolean value)
			throws IllegalArgumentException, OpenemsNamedException {
		if (channel.value().asOptional().equals(Optional.of(value))) {
			// it is already in the desired state
			return false;
		} else {
			channel.setNextWriteValue(value);
			return true;
		}
	}
}
