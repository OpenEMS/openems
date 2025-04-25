package io.openems.edge.evse.api.chargepoint;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.Limit;
import io.openems.edge.meter.api.ElectricityMeter;

public interface EvseChargePoint extends ElectricityMeter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Charge-Point is ready for charging.
		 * 
		 * <ul>
		 * <li>Cable is plugged on Charge-Point and Electric-Vehicle
		 * <li>RFID authorization is OK (if required)
		 * <li>There are no known errors
		 * <li>...
		 * </ul>
		 */
		IS_READY_FOR_CHARGING(Doc.of(OpenemsType.BOOLEAN)) //
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
	 * Apply Current in [mA] and optionally {@link Profile.Command}s.
	 * 
	 * @param current         the Current in [mA]
	 * @param profileCommands the {@link Profile.Command}s
	 */
	public void apply(int current, ImmutableList<Profile.Command> profileCommands);

	/**
	 * Is this Charge-Point installed according to standard or rotated wiring?. See
	 * {@link PhaseRotation} for details.
	 *
	 * @return the {@link PhaseRotation}.
	 */
	public PhaseRotation getPhaseRotation();

	/**
	 * Gets the Channel for {@link ChannelId#IS_READY_FOR_CHARGING}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getIsReadyForChargingChannel() {
		return this.channel(ChannelId.IS_READY_FOR_CHARGING);
	}

	/**
	 * Gets the Plug-Status of the Charge Point. See
	 * {@link ChannelId#IS_READY_FOR_CHARGING}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default boolean getIsReadyForCharging() {
		return this.getIsReadyForChargingChannel().value().orElse(false);
	}
}
