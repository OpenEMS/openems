package io.openems.edge.evse.api.chargepoint;

import java.util.function.Consumer;

import io.openems.common.channel.PersistencePriority;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.Limit;
import io.openems.edge.meter.api.ElectricityMeter;

public interface EvseChargePoint extends ElectricityMeter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Status.
		 *
		 * <p>
		 * The Status of the EVSE Charge-Point.
		 *
		 * <ul>
		 * <li>Interface: Evse.ChargePoint
		 * <li>Readable
		 * <li>Type: Status
		 * </ul>
		 */
		STATUS(Doc.of(Status.values()) //
				.persistencePriority(PersistencePriority.HIGH)), //
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

	public sealed interface ApplyCharge {
		public record SetCurrent(int current) implements ApplyCharge {
		}

		public static final Zero ZERO = new Zero();

		public record Zero() implements ApplyCharge {
		}
	}

	public record ChargeParams(Limit limit, Consumer<ApplyCharge> applyChargeCallback) {
	}

	/**
	 * Gets the {@link ChargeParams}.
	 * 
	 * @return list of {@link ChargeParams}s
	 */
	public ChargeParams getChargeParams();

	/**
	 * Gets the Channel for {@link ChannelId#STATUS}.
	 *
	 * @return the Channel
	 */
	public default Channel<Status> getStatusChannel() {
		return this.channel(ChannelId.STATUS);
	}

	/**
	 * Gets the Status of the EVCS charging station. See {@link ChannelId#STATUS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Status getStatus() {
		return this.getStatusChannel().value().asEnum();
	}
}
