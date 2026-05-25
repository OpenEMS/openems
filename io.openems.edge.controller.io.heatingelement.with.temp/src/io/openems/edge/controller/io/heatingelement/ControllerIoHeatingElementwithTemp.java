package io.openems.edge.controller.io.heatingelement;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.Unit.CUMULATED_SECONDS;
import static io.openems.common.channel.Unit.SECONDS;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;

import io.openems.common.channel.PersistencePriority;
import io.openems.edge.common.channel.Doc;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Status;
import io.openems.edge.meter.api.ElectricityMeter;

public interface ControllerIoHeatingElementwithTemp extends Controller, ElectricityMeter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		LEVEL(Doc.of(Level.values()) //
				.text("Current Level") //
				.persistencePriority(HIGH)),
		AWAITING_HYSTERESIS(Doc.of(INTEGER)), //
		PHASE1_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //
		PHASE2_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //
		PHASE3_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //
		/*
		 * LEVELx_TIME was used for old history view. It is left for the analysis of the
		 * forced duration on a day.
		 */
		WATER_ACTUAL(Doc.of(OpenemsType.INTEGER)), //
		WATER_TARGET(Doc.of(OpenemsType.INTEGER)), //
		
		
		LEVEL1_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //
		LEVEL2_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //
		LEVEL3_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //

		/*
		 * Total active Time of each Level.
		 */
		LEVEL1_CUMULATED_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS) //
				.persistencePriority(HIGH)), //
		LEVEL2_CUMULATED_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS) //
				.persistencePriority(HIGH)), //
		LEVEL3_CUMULATED_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS) //
				.persistencePriority(HIGH)), //
		TOTAL_PHASE_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //
		FORCE_START_AT_SECONDS_OF_DAY(Doc.of(INTEGER)//
				.unit(SECONDS) //
				.persistencePriority(PersistencePriority.HIGH)),
		STATUS(Doc.of(Status.values()) //
				.persistencePriority(PersistencePriority.HIGH)); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	
	public default IntegerReadChannel getWaterActualChannel() {
		return this.channel(ChannelId.WATER_ACTUAL);
	}


	public default Value<Integer> getWaterActual() {
		return this.getWaterActualChannel().value();
	}

	
	public default void _setWaterActual(Integer value) {
		this.getWaterActualChannel().setNextValue(value);
	}

	
	public default IntegerReadChannel getWaterTargetChannel() {
		return this.channel(ChannelId.WATER_TARGET);
	}

	
	public default Value<Integer> getWaterTarget() {
		return this.getWaterTargetChannel().value();
	}


	public default void _setWaterTarget(Integer value) {
		this.getWaterTargetChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#AMBIENT_ACTUAL}.
	 *
	 * @return the Channel
	 */
	
}