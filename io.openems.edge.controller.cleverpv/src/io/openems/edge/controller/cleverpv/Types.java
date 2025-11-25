package io.openems.edge.controller.cleverpv;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.util.function.Function;

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.host.Host;
import io.openems.edge.common.sum.Sum;

public final class Types {

	private Types() {
	}

	public static record SendData(
			// Legacy Push-API
			Integer watt, Integer producingWatt, Integer soc, Integer powerStorageState, Integer chargingPower, //
			// OpenEMS Push/Polling API
			CurrentData currentData, AvailableControlModes availableControlModes, ActiveControlModes activeControlModes,
			String edgeId, Integer state) {

		public SendData(CurrentData currentData, AvailableControlModes availableControlModes,
				ActiveControlModes activeControlModes, Host host, Sum sum) {
			this(currentData.sumGridActivePower, currentData.productionActivePower, currentData.sumEssSoc,
					PowerStorageState.fromPower(currentData.sumEssDischargePower).getValue(),
					currentData.sumEssDischargePower, currentData, availableControlModes, activeControlModes,
					host.getHostname().get(), sum.getState().getValue());
		}

		protected static JsonSerializer<SendData> serializer() {
			return jsonObjectSerializer(json -> {

				return new SendData(//
						json.getOptionalInt("watt").orElse(null), //
						json.getOptionalInt("producingWatt").orElse(null), //
						json.getOptionalInt("soc").orElse(null), //
						json.getOptionalInt("powerStorageState").orElse(null), //
						json.getOptionalInt("chargingPower").orElse(null), //
						json.getObject("currentData", CurrentData.serializer()), //
						json.getObject("availableControlModes", AvailableControlModes.serializer()), //
						json.getObject("activeControlModes", ActiveControlModes.serializer()), //
						json.getOptionalString("edgeId").orElse(null), //
						json.getOptionalInt("state").orElse(null) //
				); //
			}, obj -> {
				return buildJsonObject() //
						.addPropertyIfNotNull("edgeId", obj.edgeId) //
						.addPropertyIfNotNull("state", obj.state) //
						.addProperty("watt", obj.watt) //
						.addProperty("producingWatt", obj.producingWatt) //
						.addProperty("soc", obj.soc) //
						.addProperty("powerStorageState", obj.powerStorageState) //
						.addProperty("chargingPower", obj.chargingPower) //
						.add("currentData", CurrentData.serializer().serialize(obj.currentData)) //
						.add("availableControlModes",
								AvailableControlModes.serializer().serialize(obj.availableControlModes)) //
						.add("activeControlModes", ActiveControlModes.serializer().serialize(obj.activeControlModes)) //
						.build();
			});
		}

		public static record CurrentData(Integer sumGridActivePower, Integer productionActivePower, Integer sumEssSoc,
				Integer sumEssDischargePower) {
			protected static JsonSerializer<CurrentData> serializer() {
				return jsonObjectSerializer(json -> {
					return new CurrentData(//
							json.getOptionalInt("sumGridActivePower").orElse(null), //
							json.getOptionalInt("productionActivePower").orElse(null), //
							json.getOptionalInt("sumEssSoc").orElse(null), //
							json.getOptionalInt("sumEssDischargePower").orElse(null));
				}, obj -> {
					return buildJsonObject() //
							.addProperty("sumGridActivePower", obj.sumGridActivePower) //
							.addProperty("productionActivePower", obj.productionActivePower) //
							.addProperty("sumEssSoc", obj.sumEssSoc) //
							.addProperty("sumEssDischargePower", obj.sumEssDischargePower) //
							.build();
				});
			}

			private static final ChannelAddress SUM_GRID_ACTIVE_POWER = new ChannelAddress(Sum.SINGLETON_COMPONENT_ID, Sum.ChannelId.GRID_ACTIVE_POWER.id());
			private static final ChannelAddress SUM_PRODUCTION_ACTIVE_POWER = new ChannelAddress(Sum.SINGLETON_COMPONENT_ID, Sum.ChannelId.PRODUCTION_ACTIVE_POWER.id());
			private static final ChannelAddress SUM_ESS_SOC = new ChannelAddress(Sum.SINGLETON_COMPONENT_ID, Sum.ChannelId.ESS_SOC.id());
			private static final ChannelAddress SUM_ESS_DISCHARGE_POWER = new ChannelAddress(Sum.SINGLETON_COMPONENT_ID, Sum.ChannelId.ESS_DISCHARGE_POWER.id());

			/**
			 * Creates a {@link CurrentData} instance from the given
			 * {@link ComponentManager}.
			 *
			 * @param componentManager the {@link ComponentManager} used to read the channel
			 *                         values
			 * @return the {@link CurrentData} object created from the available channel
			 *         values
			 * @throws IllegalArgumentException or OpenemsNamedException If a channel cannot
			 *                                  be accessed
			 */
			public static CurrentData fromComponentManager(ComponentManager componentManager) {
				Function<ChannelAddress, Integer> getter = channelAddress -> {
					try {
						return componentManager.<IntegerReadChannel>getChannel(channelAddress).value().get();
					} catch (IllegalArgumentException | OpenemsNamedException e) {
						return null; // ignore
					}
				};

				return new CurrentData(//
						getter.apply(SUM_GRID_ACTIVE_POWER), //
						getter.apply(SUM_PRODUCTION_ACTIVE_POWER), //
						getter.apply(SUM_ESS_SOC), //
						getter.apply(SUM_ESS_DISCHARGE_POWER));
			}
		}

		public static record AvailableControlModes(ImmutableList<Ess> ess) {
			/**
			 * Returns a {@link JsonSerializer} for a {@link AvailableControlModes}.
			 *
			 * @return the created {@link JsonSerializer}
			 */
			protected static JsonSerializer<AvailableControlModes> serializer() {
				return jsonObjectSerializer(json -> {
					return new AvailableControlModes(//
							json.getImmutableList("ess", Ess.serializer()));
				}, obj -> {
					return buildJsonObject() //
							.add("ess", Ess.serializer().toImmutableListSerializer().serialize(obj.ess)) //
							.build();
				});
			}
		}

		public static record ActiveControlModes(Ess ess) {
			/**
			 * Returns a {@link JsonSerializer} for a {@link ActiveControlModes}.
			 *
			 * @return the created {@link JsonSerializer}
			 */
			protected static JsonSerializer<ActiveControlModes> serializer() {
				return jsonObjectSerializer(json -> {
					return new ActiveControlModes(//
							json.getObjectOrNull("ess", Ess.serializer()));
				}, obj -> {
					return buildJsonObject() //
							.onlyIf(obj.ess != null, b -> b.add("ess", Ess.serializer().serialize(obj.ess))) //
							.build();
				});
			}
		}

		public static record ActivateControlModes(Ess ess) {
			/**
			 * Returns a {@link JsonSerializer} for a {@link ActivateControlModes}.
			 *
			 * @return the created {@link JsonSerializer}
			 */
			protected static JsonSerializer<ActivateControlModes> serializer() {
				return jsonObjectSerializer(json -> {
					return new ActivateControlModes(//
							json.getObjectOrNull("ess", Ess.serializer()));
				}, obj -> {
					return buildJsonObject() //
							.onlyIf(obj.ess != null, b -> b.add("ess", Ess.serializer().serialize(obj.ess))) //
							.build();
				});
			}
		}

		public static record Response(ActivateControlModes activateControlModes) {
			protected static JsonSerializer<Response> serializer() {
				return jsonObjectSerializer(json -> {
					return new Response(json.getObject("activateControlModes", ActivateControlModes.serializer()));
				}, obj -> {
					return buildJsonObject().add("activateControlModes",
							ActivateControlModes.serializer().serialize(obj.activateControlModes)).build();
				});
			}
		}

		public static record Ess(RemoteControlMode remoteControlMode) {
			/**
			 * Returns a {@link JsonSerializer} for a {@link Ess}.
			 *
			 * @return the created {@link JsonSerializer}
			 */
			protected static JsonSerializer<Ess> serializer() {
				return jsonObjectSerializer(json -> {
					return new Ess(//
							json.getEnumOrNull("mode", RemoteControlMode.class));
				}, obj -> {
					return buildJsonObject() //
							.addPropertyIfNotNull("mode", obj.remoteControlMode) //
							.build();
				});
			}
		}
	}
}
