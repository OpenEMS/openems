package io.openems.edge.evse.api.chargepoint;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
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
				.persistencePriority(HIGH)), //
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

	public record ChargeParams(boolean isReadyForCharging, Limit limit, ImmutableList<Profile> profiles) {

		/**
		 * Serialize.
		 * 
		 * @param cp the {@link ChargeParams}, possibly null
		 * @return the {@link JsonElement}
		 */
		public static JsonElement toJson(ChargeParams cp) {
			if (cp == null) {
				return JsonNull.INSTANCE;
			}
			return buildJsonObject() //
					.addProperty("isReadyForCharging", cp.isReadyForCharging) //
					.add("limit", Limit.toJson(cp.limit)) //
					.add("profiles", new JsonArray() /* TODO */) //
					.build();
		}

		/**
		 * Deserialize.
		 * 
		 * @param j a {@link JsonObject}
		 * @return the {@link ChargeParams}
		 * @throws OpenemsNamedException on error
		 */
		public static ChargeParams fromJson(JsonObject j) throws OpenemsNamedException {
			return new ChargeParams(//
					getAsBoolean(j, "isReadyForCharging"), //
					Limit.fromJson(getAsJsonObject(j, "limit")), //
					ImmutableList.of() /* TODO */);
		}
	}

	/**
	 * Gets the {@link ChargeParams}.
	 * 
	 * @return list of {@link ChargeParams}s
	 */
	public ChargeParams getChargeParams();

	/**
	 * Apply an {@link ApplyCharge} and optionally {@link Profile.Command}s.
	 * 
	 * @param applyCharge     the {@link ApplyCharge}
	 * @param profileCommands the {@link Profile.Command}s
	 */
	public void apply(ApplyCharge applyCharge, ImmutableList<Profile.Command> profileCommands);

	/**
	 * Is this Charge-Point installed according to standard or rotated wiring?. See
	 * {@link PhaseRotation} for details.
	 *
	 * @return the {@link PhaseRotation}.
	 */
	public PhaseRotation getPhaseRotation();

	/**
	 * Gets the Channel for {@link ChannelId#STATUS}.
	 *
	 * @return the Channel
	 */
	public default Channel<Status> getStatusChannel() {
		return this.channel(ChannelId.STATUS);
	}

	/**
	 * Gets the Status of the Charge Point. See {@link ChannelId#STATUS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Status getStatus() {
		return this.getStatusChannel().value().asEnum();
	}
}
