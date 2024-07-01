package io.openems.backend.timedata.aggregatedinflux;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.influxdb.client.write.Point;

public final class AllowedChannels {

	public static final Map<String, DataType> ALLOWED_AVERAGE_CHANNELS;
	public static final Map<String, DataType> ALLOWED_CUMULATED_CHANNELS;

	private AllowedChannels() {
	}

	static {
		ALLOWED_AVERAGE_CHANNELS = ImmutableMap.<String, DataType>builder() //
				.put("_sum/EssSoc", DataType.LONG) //
				.put("_sum/EssActivePower", DataType.LONG) //
				.put("_sum/EssActivePowerL1", DataType.LONG) //
				.put("_sum/EssActivePowerL2", DataType.LONG) //
				.put("_sum/EssActivePowerL3", DataType.LONG) //
				.put("_sum/GridActivePower", DataType.LONG) //
				.put("_sum/GridActivePowerL1", DataType.LONG) //
				.put("_sum/GridActivePowerL2", DataType.LONG) //
				.put("_sum/GridActivePowerL3", DataType.LONG) //
				.put("_sum/ProductionActivePower", DataType.LONG) //
				.put("_sum/ProductionAcActivePower", DataType.LONG) //
				.put("_sum/ProductionAcActivePowerL1", DataType.LONG) //
				.put("_sum/ProductionAcActivePowerL2", DataType.LONG) //
				.put("_sum/ProductionAcActivePowerL3", DataType.LONG) //
				.put("_sum/ProductionDcActualPower", DataType.LONG) //
				.put("_sum/ConsumptionActivePower", DataType.LONG) //
				.put("_sum/ConsumptionActivePowerL1", DataType.LONG) //
				.put("_sum/ConsumptionActivePowerL2", DataType.LONG) //
				.put("_sum/ConsumptionActivePowerL3", DataType.LONG) //
				.put("_sum/UnmanagedConsumptionActivePower", DataType.LONG) //
				.putAll(multiChannels("io", 0, 10, "Relay", 1, 9, DataType.LONG)) //
				.put("ctrlIoHeatPump0/Status", DataType.LONG) //
				.putAll(multiChannels("ess", 0, 17, "Soc", DataType.LONG)) //
				.putAll(multiChannels("ess", 0, 17, "ActivePower", DataType.LONG)) //
				.putAll(multiChannels("ess", 0, 17, "ReactivePower", DataType.LONG)) //
				.put("ctrlIoHeatingElement0/Level", DataType.LONG) //
				.put("ctrlGridOptimizedCharge0/DelayChargeMaximumChargeLimit", DataType.LONG) //
				.putAll(multiChannels("charger", 0, 20, "ActualPower", DataType.LONG)) //
				.put("ctrlEmergencyCapacityReserve0/ActualReserveSoc", DataType.LONG) //
				.put("ctrlGridOptimizedCharge0/_PropertyMaximumSellToGridPower", DataType.LONG) //
				.putAll(multiChannels("meter", 0, 10, "ActivePower", DataType.LONG)) //
				.putAll(multiChannels("meter", 0, 10, "ActivePowerL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels("pvInverter", 0, 10, "ActivePower", DataType.LONG)) //
				.putAll(multiChannels("pvInverter", 0, 10, "ActivePowerL", 1, 4, DataType.LONG)) //
				.put("_sum/EssDischargePower", DataType.LONG) // used for xlsx export
				.put("ctrlGridOptimizedCharge0/SellToGridLimitMinimumChargeLimit", DataType.LONG) //
				.put("ctrlEssTimeOfUseTariff0/QuarterlyPrices", DataType.DOUBLE) //
				.put("ctrlEssTimeOfUseTariff0/StateMachine", DataType.LONG) //
				.putAll(multiChannels("evcs", 0, 10, "ChargePower", DataType.LONG)) //
				.putAll(multiChannels("ctrlTimeslotPeakshaving", 0, 10, "StateMachine", DataType.LONG)) //
				.putAll(multiChannels("ctrlTimeslotPeakshaving", 0, 10, "_PropertyRechargePower", DataType.LONG)) //
				.putAll(multiChannels("ctrlTimeslotPeakshaving", 0, 10, "_PropertyPeakShavingPower", DataType.LONG)) //
				// both for symmetric and asymmetric
				.put("ctrlPeakShaving0/_PropertyPeakShavingPower", DataType.LONG) //
				.put("ctrlPeakShaving0/_PropertyRechargePower", DataType.LONG) //
				.putAll(multiChannels("ctrlApiModbusTcp", 0, 3, "Ess0SetActivePowerEquals", DataType.LONG)) //
				.putAll(multiChannels("ctrlApiModbusTcp", 0, 3, "Ess0SetReactivePowerEquals", DataType.LONG)) //
				.putAll(multiChannels("ctrlApiModbusTcp", 0, 3, "Ess0SetActivePowerLessOrEquals", DataType.LONG)) //
				.putAll(multiChannels("ctrlApiModbusTcp", 0, 3, "Ess0SetReactivePowerLessOrEquals", DataType.LONG)) //
				.putAll(multiChannels("ctrlApiModbusTcp", 0, 3, "Ess0SetReactivePowerGreaterOrEquals", DataType.LONG)) //
				.putAll(multiChannels("ctrlApiModbusTcp", 0, 3, "Ess0SetActivePowerGreaterOrEquals", DataType.LONG)) //
				.build();

		ALLOWED_CUMULATED_CHANNELS = ImmutableMap.<String, DataType>builder() //
				.put("_sum/EssDcChargeEnergy", DataType.LONG) //
				.put("_sum/EssDcDischargeEnergy", DataType.LONG) //
				.put("_sum/GridSellActiveEnergy", DataType.LONG) //
				.put("_sum/ProductionActiveEnergy", DataType.LONG) //
				.put("_sum/ProductionAcActiveEnergy", DataType.LONG) //
				.put("_sum/ProductionDcActiveEnergy", DataType.LONG) //
				.put("_sum/ConsumptionActiveEnergy", DataType.LONG) //
				.put("_sum/GridBuyActiveEnergy", DataType.LONG) //
				.put("_sum/EssActiveChargeEnergy", DataType.LONG) //
				.put("_sum/EssActiveDischargeEnergy", DataType.LONG) //
				.put("ctrlEssTimeOfUseTariffDischarge0/DelayedTime", DataType.LONG) //
				.put("ctrlEssTimeOfUseTariff0/DelayedTime", DataType.LONG) //
				.put("ctrlEssTimeOfUseTariff0/ChargedTime", DataType.LONG) //
				.putAll(multiChannels("evcs", 0, 10, "ActiveConsumptionEnergy", DataType.LONG)) //
				.putAll(multiChannels("meter", 0, 10, "ActiveProductionEnergy", DataType.LONG)) //
				.putAll(multiChannels("meter", 0, 10, "ActiveProductionEnergyL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels("meter", 0, 10, "ActiveConsumptionEnergy", DataType.LONG)) //
				.putAll(multiChannels("meter", 0, 10, "ActiveConsumptionEnergyL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels("io", 0, 10, "ActiveProductionEnergy", DataType.LONG)) //
				.putAll(multiChannels("pvInverter", 0, 10, "ActiveProductionEnergy", DataType.LONG)) //
				.putAll(multiChannels("pvInverter", 0, 10, "ActiveProductionEnergyL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels("charger", 0, 20, "ActualEnergy", DataType.LONG)) //
				.put("ctrlGridOptimizedCharge0/AvoidLowChargingTime", DataType.LONG) //
				.put("ctrlGridOptimizedCharge0/NoLimitationTime", DataType.LONG) //
				.put("ctrlGridOptimizedCharge0/SellToGridLimitTime", DataType.LONG) //
				.put("ctrlGridOptimizedCharge0/DelayChargeTime", DataType.LONG) //
				.put("ctrlIoHeatingElement0/Level1CumulatedTime", DataType.LONG) //
				.put("ctrlIoHeatingElement0/Level2CumulatedTime", DataType.LONG) //
				.put("ctrlIoHeatingElement0/Level3CumulatedTime", DataType.LONG) //
				.put("ctrlChpSoc0/CumulatedActiveTime", DataType.LONG) //
				.put("ctrlFixActivePower0/CumulatedActiveTime", DataType.LONG) //
				.putAll(multiChannels("ctrlChannelThreshold", 0, 5, "CumulatedActiveTime", DataType.LONG)) //
				.putAll(multiChannels("ctrlIoChannelSingleThreshold", 0, 5, "CumulatedActiveTime", DataType.LONG)) //
				.putAll(multiChannels("ctrlIoFixDigitalOutput", 0, 5, "CumulatedActiveTime", DataType.LONG)) //
				.put("ctrlIoHeatPump0/RegularStateTime", DataType.LONG) //
				.put("ctrlIoHeatPump0/RecommendationStateTime", DataType.LONG) //
				.put("ctrlIoHeatPump0/ForceOnStateTime", DataType.LONG) //
				.put("ctrlIoHeatPump0/LockStateTime", DataType.LONG) //
				.putAll(multiChannels("ess", 0, 17, "ActiveChargeEnergy", DataType.LONG)) //
				.putAll(multiChannels("ess", 0, 17, "ActiveDischargeEnergy", DataType.LONG)) //
				.putAll(multiChannels("ctrlApiModbusTcp", 0, 3, "CumulatedActiveTime", DataType.LONG)) //
				.putAll(multiChannels("ctrlApiModbusTcp", 0, 3, "CumulatedInactiveTime", DataType.LONG)) //
				.build();
	}

	public static enum ChannelType {
		AVG, //
		MAX, //
		UNDEFINED, //
		;
	}

	/**
	 * Checks if the given channel is a allowed channel.
	 * 
	 * @param channel the to check
	 * @return true if defined otherwise false
	 */
	public static boolean isChannelDefined(String channel) {
		return getChannelType(channel) != ChannelType.UNDEFINED;
	}

	/**
	 * Gets the type of the given channel.
	 * 
	 * @param channel the Channel-Address
	 * @return the {@link ChannelType}
	 */
	public static ChannelType getChannelType(String channel) {
		if (ALLOWED_AVERAGE_CHANNELS.containsKey(channel)) {
			return ChannelType.AVG;
		}
		if (ALLOWED_CUMULATED_CHANNELS.containsKey(channel)) {
			return ChannelType.MAX;
		}
		return ChannelType.UNDEFINED;
	}

	/**
	 * Adds the given value to the builder at the specified field parsed to the
	 * predefined type of the channel.
	 * 
	 * @param builder a {@link Point} builder
	 * @param field   the field name
	 * @param value   the {@link JsonElement} value
	 * @return true on success
	 */
	public static boolean addWithSpecificChannelType(Point builder, String field, JsonElement value) {
		if (value == null) {
			return false;
		}
		if (!value.isJsonPrimitive()) {
			return false;
		}
		if (!value.getAsJsonPrimitive().isNumber()) {
			return false;
		}
		final var type = typeOf(field);
		if (type == null) {
			return false;
		}

		var number = value.getAsNumber();

		if (number.getClass().getName().equals("com.google.gson.internal.LazilyParsedNumber")) {
			number = number.doubleValue();
		}

		switch (type) {
		case DOUBLE -> builder.addField(field, number.doubleValue());
		case LONG -> builder.addField(field, number.longValue());
		}
		return true;
	}

	protected static enum DataType {
		LONG, //
		DOUBLE, //
		;
	}

	private static DataType typeOf(String channel) {
		var type = ALLOWED_AVERAGE_CHANNELS.get(channel);
		if (type != null) {
			return type;
		}
		type = ALLOWED_CUMULATED_CHANNELS.get(channel);
		if (type != null) {
			return type;
		}
		return null;
	}

	protected static Iterable<Entry<String, DataType>> multiChannels(//
			final String component, //
			final int from, //
			final int to, //
			final String channelOfComponent, //
			final DataType type //
	) {
		return IntStream.range(from, to) //
				.mapToObj(componentNumber -> {
					return component + componentNumber + "/" + channelOfComponent;
				}).collect(Collectors.toMap(t -> t, t -> type)).entrySet();
	}

	protected static Iterable<Entry<String, DataType>> multiChannels(//
			final String component, //
			final int from, //
			final int to, //
			final String channelOfComponent, //
			final int fromChannel, //
			final int toChannel, //
			final DataType type //
	) {
		return IntStream.range(from, to) //
				.mapToObj(componentNumber -> {
					return IntStream.range(fromChannel, toChannel) //
							.mapToObj(channelNumber -> {
								return component + componentNumber + "/" + channelOfComponent + channelNumber;
							});
				}).flatMap(t -> t).collect(Collectors.toMap(t -> t, t -> type)).entrySet();
	}

}
