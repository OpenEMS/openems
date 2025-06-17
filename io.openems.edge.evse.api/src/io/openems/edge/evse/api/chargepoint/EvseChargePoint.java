package io.openems.edge.evse.api.chargepoint;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import com.google.gson.JsonNull;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.Limit;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
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

	// TODO consider replace with ChargePointAbilities
	public record ChargeParams(boolean isReadyForCharging, Limit limit, ChargePointAbilities abilities) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link ChargeParams}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<ChargeParams> serializer() {
			return jsonObjectSerializer(ChargeParams.class, json -> {
				return new ChargeParams(//
						json.getBoolean("isReadyForCharging"), //
						json.getObject("limit", Limit.serializer()), //
						// TODO json.getImmutableList("abilities", ChargePointAbilities.serializer())
						ChargePointAbilities.create().build());
			}, obj -> {
				return obj == null //
						? JsonNull.INSTANCE //
						: buildJsonObject() //
								.addProperty("isReadyForCharging", obj.isReadyForCharging) //
								.add("limit", Limit.serializer().serialize(obj.limit)) //
								// TODO .add("profiles", ...) //
								.build();
			});
		}
	}

	/**
	 * Gets the {@link ChargeParams}.
	 * 
	 * @return list of {@link ChargeParams}s
	 */
	public ChargeParams getChargeParams();

	/**
	 * Apply {@link ChargePointActions}.
	 * 
	 * @param actions the {@link ChargePointActions}
	 */
	public void apply(ChargePointActions actions);

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
