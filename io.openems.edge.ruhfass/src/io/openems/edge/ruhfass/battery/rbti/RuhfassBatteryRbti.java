package io.openems.edge.ruhfass.battery.rbti;

import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.ruhfass.battery.rbti.enums.BatteryCellType;
import io.openems.edge.ruhfass.battery.rbti.statemachine.StateMachine.State;

public interface RuhfassBatteryRbti extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Stop battery timeout passed.
		 * 
		 * <p>
		 * This fault forces the EssGeneric into the GO_STOPPED state.
		 */
		TIMEOUT_STOP_BATTERY(Doc.of(Level.FAULT)//
				.text("Stop battery timeout passed!")), //
		/**
		 * Start battery timeout passed.
		 * 
		 * <p>
		 * This fault forces the EssGeneric into the GO_STOPPED state.
		 */
		TIMEOUT_START_BATTERY(Doc.of(Level.FAULT)//
				.text("Start battery timeout passed!")), //

		STATE_MACHINE(Doc.of(State.values())//
				.text("Current State of State-Machine")),

		RUN_FAILED(Doc.of(Level.WARNING)//
				.text("Running the Logic failed")),

		/*
		 * Write Registers
		 */
		// 0 = Restbussimulation aus; 1 = Restbussimulation an
		SET_EV_RESIDUAL_CAN_SIMULATION(Doc.of(INTEGER)//
				.text("Fahrzeug-Restbussimulation (an/aus)")//
				.accessMode(AccessMode.WRITE_ONLY)), //

		// 0 = HV Schütze öffnen;
		// 1 = HV Schütze schließen
		// 2 = Lade-Modus angefordern
		SET_HV_BATTERY_ACTIVATION_REQUEST(Doc.of(INTEGER)//
				.text("Anforderung HV-Batterie an/aus (HV on / HV off)")//
				.accessMode(AccessMode.WRITE_ONLY)), //

		/*
		 * Read Registers
		 */
		BATTERY_CELL_TYPE(Doc.of(BatteryCellType.values())), //
		TOTAL_NUMBER_OF_CELLS(Doc.of(INTEGER)),

		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the target Start/Stop mode from config or StartStop-Channel.
	 *
	 * @return {@link StartStop}
	 */
	public StartStop getStartStopTarget();
}
