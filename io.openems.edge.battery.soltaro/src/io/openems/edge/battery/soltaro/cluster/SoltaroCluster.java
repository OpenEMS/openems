package io.openems.edge.battery.soltaro.cluster;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.soltaro.cluster.enums.ClusterStartStop;
import io.openems.edge.battery.soltaro.cluster.enums.RackUsage;
import io.openems.edge.battery.soltaro.cluster.enums.RunningState;
import io.openems.edge.battery.soltaro.common.enums.ChargeIndication;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;

public interface SoltaroCluster extends Battery, OpenemsComponent, EventHandler, ModbusSlave {

	/**
	 * Gets the Channel for {@link ChannelId#CLUSTER_START_STOP}.
	 *
	 * @return the Channel
	 */
	public default WriteChannel<ClusterStartStop> getClusterStartStopChannel() {
		return this.channel(ChannelId.CLUSTER_START_STOP);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#CLUSTER_START_STOP}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default ClusterStartStop getClusterStartStop() {
		return this.getClusterStartStopChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CLUSTER_START_STOP} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setClusterStartStop(ClusterStartStop value) {
		this.getClusterStartStopChannel().setNextValue(value);
	}

	/**
	 * Writes the value to the {@link ChannelId#CLUSTER_START_STOP} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setClusterStartStop(ClusterStartStop value) throws OpenemsNamedException {
		this.getClusterStartStopChannel().setNextWriteValue(value);
	}

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/*
		 * EnumWriteChannels
		 */
		CLUSTER_START_STOP(Doc.of(ClusterStartStop.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		RACK_1_USAGE(Doc.of(RackUsage.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		RACK_2_USAGE(Doc.of(RackUsage.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		RACK_3_USAGE(Doc.of(RackUsage.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		RACK_4_USAGE(Doc.of(RackUsage.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		RACK_5_USAGE(Doc.of(RackUsage.values()) //
				.accessMode(AccessMode.READ_WRITE)), //

		/*
		 * EnumReadChannels
		 */
		SYSTEM_RUNNING_STATE(Doc.of(RunningState.values())), //

		/*
		 * IntegerReadChannels
		 */
		CHARGE_INDICATION(Doc.of(ChargeIndication.values())), //
		SYSTEM_INSULATION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.OHM)), //

		/*
		 * StateChannels
		 */
		// Sub Master Communication Failure Registers
		SUB_MASTER_1_COMMUNICATION_FAILURE(Doc.of(Level.WARNING) //
				.text("Communication Failure to Sub Master 1")),
		SUB_MASTER_2_COMMUNICATION_FAILURE(Doc.of(Level.WARNING) //
				.text("Communication Failure to Sub Master 2")),
		SUB_MASTER_3_COMMUNICATION_FAILURE(Doc.of(Level.WARNING) //
				.text("Communication Failure to Sub Master 3")),
		SUB_MASTER_4_COMMUNICATION_FAILURE(Doc.of(Level.WARNING) //
				.text("Communication Failure to Sub Master 4")),
		SUB_MASTER_5_COMMUNICATION_FAILURE(Doc.of(Level.WARNING) //
				.text("Communication Failure to Sub Master 5")),

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

}