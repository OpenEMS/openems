package io.openems.edge.ess.mr.gridcon;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.mr.gridcon.enums.InverterCount;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.mr.gridcon.writeutils.CcuControlParameters;
import io.openems.edge.ess.mr.gridcon.writeutils.CommandControlRegisters;
import io.openems.edge.meter.api.SymmetricMeter;

public class OffgridHandler {

	private final Logger log = LoggerFactory.getLogger(OffgridHandler.class);
	private final StateMachine parent;

	public OffgridHandler(StateMachine parent) {
		this.parent = parent;
	}

	public void initialize() {
	}

	protected StateMachine.State run() throws IllegalArgumentException, OpenemsNamedException {
		// Are we still Off-Grid?
		GridMode gridMode = this.parent.parent.getGridMode().value().asEnum();
		switch (gridMode) {
		case OFF_GRID:
		case UNDEFINED:
			break;
		case ON_GRID:
			// new state is On-Grid -> switch to GOING_ONGRID
			return StateMachine.State.GOING_ONGRID;
		}

		// Always set OutputSyncDeviceBridge ON in Off-Grid state
		this.parent.parent.setOutputSyncDeviceBridge(true);

		// Measured by Grid-Meter, grid Values
		SymmetricMeter gridMeter = this.parent.parent.componentManager.getComponent(this.parent.parent.config.meter());

		Optional<Integer> gridFreqOpt = gridMeter.getFrequency().value().asOptional();
		Optional<Integer> gridVoltOpt = gridMeter.getVoltage().value().asOptional();

		this.log.info("OffgridHandler.run() GridFreq: " + gridFreqOpt + ", GridVolt: " + gridVoltOpt);

		if (!gridFreqOpt.isPresent() || !gridVoltOpt.isPresent()) {
			// TODO used to do doBlackStartGoingOnGrid() - correct?
			return StateMachine.State.OFFGRID;
		}

		int gridFreq = gridFreqOpt.get();
		int gridVolt = gridVoltOpt.get();

		if (gridFreq == 0 || gridFreq < 49_700 || gridFreq > 50_300 || //
				gridVolt == 0 || gridVolt < 215_000 || gridVolt > 245_000) {
			/*
			 * Off-Grid
			 */
			this.doNormalBlackStartMode();

		} else {
			/*
			 * Going On-Grid
			 */
			BooleanReadChannel inputNAProtection1 = parent.parent.componentManager
					.getChannel(ChannelAddress.fromString(parent.parent.config.inputNAProtection1()));
			BooleanReadChannel inputNAProtection2 = parent.parent.componentManager
					.getChannel(ChannelAddress.fromString(parent.parent.config.inputNAProtection2()));
			
			Optional<Boolean> isInputNAProtection1 = inputNAProtection1.value().asOptional();
			Optional<Boolean> isInputNAProtection2 = inputNAProtection2.value().asOptional();
			
			if (isInputNAProtection1.isPresent() && isInputNAProtection1.get()) {
				
				if (isInputNAProtection2.isPresent() && isInputNAProtection2.get()) {
					// We are on grid MR has to be switched off and restarted
					System.out.println("!!!! Grid is back --> set state to undefined!!");
//					this.state = StateMachine.UNDEFINED;
					
				} else {
					// going on grid
					System.out.println("Grid is back, M1C1 is set, going on grid!");
					doBlackStartGoingOnGrid(gridFreq, gridVolt);					
				}
				
			} else {
				System.out.println("Grid is back, M1C1 is not set, do normal mode!");
				doNormalBlackStartMode();
			}
		}

		return StateMachine.State.OFFGRID;
	}

	/**
	 * Handle Normal BlackStartMode.
	 * 
	 * @throws IllegalArgumentException
	 * @throws OpenemsNamedException
	 */
	private void doNormalBlackStartMode() throws IllegalArgumentException, OpenemsNamedException {
		log.info("OffgridHandler.doNormalBlackStartMode() Write channels for blackstart mode");
		InverterCount inverterCount = this.parent.parent.config.inverterCount();
		new CommandControlRegisters() //
				.play(true) //
				.syncApproval(false) //
				.blackstartApproval(true) //
				.modeSelection(CommandControlRegisters.Mode.VOLTAGE_CONTROL) //
				.enableIpus(inverterCount) //
				.parameterF0(GridconPCS.OFF_GRID_FREQUENCY_FACTOR) //
				.parameterU0(GridconPCS.OFF_GRID_VOLTAGE_FACTOR) //
				.writeToChannels(this.parent.parent);
		new CcuControlParameters() //
				.pControlMode(PControlMode.DISABLED) //
				.qLimit(1f) //
				.writeToChannels(this.parent.parent);
		this.parent.parent.setIpuControlSettings();
	}

	/**
	 * Handle BlackStart GoingOnGrid.
	 * 
	 * @param gridFreq
	 * @param gridVolt
	 * @throws IllegalArgumentException
	 * @throws OpenemsNamedException
	 */
	private void doBlackStartGoingOnGrid(int gridFreq, int gridVolt)
			throws IllegalArgumentException, OpenemsNamedException {
		int invSetFreq = gridFreq + this.parent.parent.config.overFrequency(); // add default 200 mHz
		int invSetVolt = gridVolt + this.parent.parent.config.overVoltage(); // add default 2 V
		float invSetFreqNormalized = invSetFreq / 50_000f;
		float invSetVoltNormalized = invSetVolt / 230_000f;

		log.info("OffgridHandler.doBlackStartGoingOnGrid() Going On-Grid -> F/U " + invSetFreq + ", " + invSetVolt
				+ ", " + invSetFreqNormalized + ", " + invSetVoltNormalized);

		InverterCount inverterCount = this.parent.parent.config.inverterCount();
		new CommandControlRegisters() //
				.play(true) //
				.ready(false) //
				.acknowledge(false) //
				.stop(false) //
				.syncApproval(false) //
				.blackstartApproval(true) //
				.shortCircuitHandling(false) //
				.modeSelection(CommandControlRegisters.Mode.VOLTAGE_CONTROL) //
				.enableIpus(inverterCount) //
				.parameterU0(invSetVoltNormalized) //
				.parameterF0(invSetFreqNormalized) //
				.writeToChannels(this.parent.parent);
		new CcuControlParameters() //
				.pControlMode(PControlMode.DISABLED) //
				.qLimit(1f) //
				.writeToChannels(this.parent.parent);
		this.parent.parent.setIpuControlSettings();
	}
}
