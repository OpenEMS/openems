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
		final var io = new Namespace("io", 0, 10);
		final var ess = new Namespace("ess", 0, 17);
		final var evcs = new Namespace("evcs", 0, 10);
		final var meter = new Namespace("meter", 0, 20);
		final var ctrlEvseSingle = new Namespace("ctrlEvseSingle", 0, 10);
		final var evseChargePoint = new Namespace("evseChargePoint", 0, 10);
		final var pvInverter = new Namespace("pvInverter", 0, 10);
		final var charger = new Namespace("charger", 0, 20);
		final var ctrlIoHeatPump = new Namespace("ctrlIoHeatPump", 0, 5);
		final var ctrlIoHeatingElement = new Namespace("ctrlIoHeatingElement", 0, 5);
		final var ctrlTimeslotPeakshaving = new Namespace("ctrlTimeslotPeakshaving", 0, 10);
		final var ctrlApiModbusTcp = new Namespace("ctrlApiModbusTcp", 0, 3);
		final var ctrlIoChannelSingleThreshold = new Namespace("ctrlIoChannelSingleThreshold", 0, 13);
		final var ctrlChannelSingleThreshold = new Namespace("ctrlChannelSingleThreshold",
				ctrlIoChannelSingleThreshold.from(), ctrlIoChannelSingleThreshold.to());
		final var ctrlEssRippleControlReceiver = new Namespace("ctrlEssRippleControlReceiver", 0, 3);
		final var heat = new Namespace("heat", 0, 5);

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
				.put("_sum/GridMode", DataType.LONG) //
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
				.put("_sum/GridBuyPrice", DataType.DOUBLE) //
				.put("_sum/UnmanagedConsumptionActivePower", DataType.LONG) //
				.putAll(multiChannels(io, "Relay", 1, 9, DataType.LONG)) //
				.putAll(multiChannels(ctrlIoHeatPump, "Status", DataType.LONG)) //
				.putAll(multiChannels(ess, "Soc", DataType.LONG)) //
				.putAll(multiChannels(ess, "ActivePower", DataType.LONG)) //
				.putAll(multiChannels(ess, "ReactivePower", DataType.LONG)) //
				.putAll(multiChannels(ctrlIoHeatingElement, "Level", DataType.LONG)) //
				.put("ctrlGridOptimizedCharge0/DelayChargeMaximumChargeLimit", DataType.LONG) //
				.putAll(multiChannels(charger, "ActualPower", DataType.LONG)) //
				.putAll(multiChannels(charger, "Current", DataType.LONG)) //
				.putAll(multiChannels(charger, "Voltage", DataType.LONG)) //
				.put("ctrlEmergencyCapacityReserve0/ActualReserveSoc", DataType.LONG) //
				.put("ctrlGridOptimizedCharge0/_PropertyMaximumSellToGridPower", DataType.LONG) //
				.putAll(multiChannels(meter, "ActivePower", DataType.LONG)) //
				.putAll(multiChannels(meter, "ActivePowerL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels(meter, "Current", DataType.LONG)) //
				.putAll(multiChannels(meter, "Voltage", DataType.LONG)) //
				.putAll(multiChannels(meter, "CurrentL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels(meter, "VoltageL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels(pvInverter, "ActivePower", DataType.LONG)) //
				.putAll(multiChannels(pvInverter, "ActivePowerL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels(pvInverter, "Current", DataType.LONG)) //
				.putAll(multiChannels(pvInverter, "CurrentL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels(pvInverter, "Voltage", DataType.LONG)) //
				.putAll(multiChannels(pvInverter, "VoltageL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels(evseChargePoint, "ActivePower", DataType.LONG)) //
				.putAll(multiChannels(evseChargePoint, "ActivePowerL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels(evseChargePoint, "Current", DataType.LONG)) //
				.putAll(multiChannels(evseChargePoint, "Voltage", DataType.LONG)) //
				.putAll(multiChannels(evseChargePoint, "CurrentL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels(evseChargePoint, "VoltageL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels(ctrlEvseSingle, "ActualMode", DataType.LONG)) //
				.put("_sum/EssDischargePower", DataType.LONG) // used for xlsx export
				.put("ctrlGridOptimizedCharge0/SellToGridLimitMinimumChargeLimit", DataType.LONG) //
				.put("ctrlEssTimeOfUseTariff0/QuarterlyPrices", DataType.DOUBLE) //
				.put("ctrlEssTimeOfUseTariff0/StateMachine", DataType.LONG) //
				.putAll(multiChannels(evcs, "ChargePower", DataType.LONG)) //
				.putAll(multiChannels(evcs, "ActivePower", DataType.LONG)) //
				.putAll(multiChannels(evcs, "ActivePowerL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels(evcs, "Current", DataType.LONG)) //
				.putAll(multiChannels(evcs, "Voltage", DataType.LONG)) //
				.putAll(multiChannels(evcs, "CurrentL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels(evcs, "VoltageL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels(ctrlTimeslotPeakshaving, "StateMachine", DataType.LONG)) //
				.putAll(multiChannels(ctrlTimeslotPeakshaving, "_PropertyRechargePower", DataType.LONG)) //
				.putAll(multiChannels(ctrlTimeslotPeakshaving, "_PropertyPeakShavingPower", DataType.LONG)) //
				// both for symmetric and asymmetric
				.put("ctrlPeakShaving0/_PropertyPeakShavingPower", DataType.LONG) //
				.put("ctrlPeakShaving0/_PropertyRechargePower", DataType.LONG) //
				.putAll(multiChannels(ctrlApiModbusTcp, "Ess0SetActivePowerEquals", DataType.LONG)) //
				.putAll(multiChannels(ctrlApiModbusTcp, "Ess0SetReactivePowerEquals", DataType.LONG)) //
				.putAll(multiChannels(ctrlApiModbusTcp, "Ess0SetActivePowerLessOrEquals", DataType.LONG)) //
				.putAll(multiChannels(ctrlApiModbusTcp, "Ess0SetReactivePowerLessOrEquals", DataType.LONG)) //
				.putAll(multiChannels(ctrlApiModbusTcp, "Ess0SetReactivePowerGreaterOrEquals", DataType.LONG)) //
				.putAll(multiChannels(ctrlApiModbusTcp, "Ess0SetActivePowerGreaterOrEquals", DataType.LONG)) //
				.put("ctrlEssLimiter14a0/RestrictionMode", DataType.LONG) //
				.putAll(multiChannels(ctrlEssRippleControlReceiver, "RestrictionMode", DataType.LONG)) //
				.putAll(multiChannels(heat, "Temperature", DataType.LONG)) //
				.putAll(multiChannels(heat, "ActivePower", DataType.LONG)) //
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
				.put("_sum/GridModeOffGridTime", DataType.LONG) //
				.put("_sum/EssActiveChargeEnergy", DataType.LONG) //
				.put("_sum/EssActiveDischargeEnergy", DataType.LONG) //
				.put("ctrlEssTimeOfUseTariffDischarge0/DelayedTime", DataType.LONG) //
				.put("ctrlEssTimeOfUseTariff0/DelayedTime", DataType.LONG) //
				.put("ctrlEssTimeOfUseTariff0/ChargedTime", DataType.LONG) //
				.putAll(multiChannels(evcs, "ActiveConsumptionEnergy", DataType.LONG)) //
				.putAll(multiChannels(evcs, "ActiveProductionEnergy", DataType.LONG)) //
				.putAll(multiChannels(evcs, "ActiveProductionEnergyL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels(evseChargePoint, "ActiveProductionEnergy", DataType.LONG)) //
				.putAll(multiChannels(evseChargePoint, "ActiveProductionEnergyL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels(meter, "ActiveProductionEnergy", DataType.LONG)) //
				.putAll(multiChannels(meter, "ActiveProductionEnergyL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels(meter, "ActiveConsumptionEnergy", DataType.LONG)) //
				.putAll(multiChannels(meter, "ActiveConsumptionEnergyL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels(io, "ActiveProductionEnergy", DataType.LONG)) //
				.putAll(multiChannels(pvInverter, "ActiveProductionEnergy", DataType.LONG)) //
				.putAll(multiChannels(pvInverter, "ActiveProductionEnergyL", 1, 4, DataType.LONG)) //
				.putAll(multiChannels(charger, "ActualEnergy", DataType.LONG)) //
				.put("ctrlGridOptimizedCharge0/AvoidLowChargingTime", DataType.LONG) //
				.put("ctrlGridOptimizedCharge0/NoLimitationTime", DataType.LONG) //
				.put("ctrlGridOptimizedCharge0/SellToGridLimitTime", DataType.LONG) //
				.put("ctrlGridOptimizedCharge0/DelayChargeTime", DataType.LONG) //
				.putAll(multiChannels(ctrlIoHeatingElement, "Level1CumulatedTime", DataType.LONG)) //
				.putAll(multiChannels(ctrlIoHeatingElement, "Level2CumulatedTime", DataType.LONG)) //
				.putAll(multiChannels(ctrlIoHeatingElement, "Level3CumulatedTime", DataType.LONG)) //
				.put("ctrlChpSoc0/CumulatedActiveTime", DataType.LONG) //
				.put("ctrlFixActivePower0/CumulatedActiveTime", DataType.LONG) //
				.putAll(multiChannels("ctrlChannelThreshold", 0, 5, "CumulatedActiveTime", DataType.LONG)) //
				.putAll(multiChannels(ctrlIoChannelSingleThreshold, "CumulatedActiveTime", DataType.LONG)) //
				.putAll(multiChannels(ctrlChannelSingleThreshold, "CumulatedActiveTime", DataType.LONG)) //
				.putAll(multiChannels("ctrlIoFixDigitalOutput", 0, 5, "CumulatedActiveTime", DataType.LONG)) //
				.putAll(multiChannels(ctrlIoHeatPump, "RegularStateTime", DataType.LONG)) //
				.putAll(multiChannels(ctrlIoHeatPump, "RecommendationStateTime", DataType.LONG)) //
				.putAll(multiChannels(ctrlIoHeatPump, "ForceOnStateTime", DataType.LONG)) //
				.putAll(multiChannels(ctrlIoHeatPump, "LockStateTime", DataType.LONG)) //
				.putAll(multiChannels(ess, "ActiveChargeEnergy", DataType.LONG)) //
				.putAll(multiChannels(ess, "ActiveDischargeEnergy", DataType.LONG)) //
				.putAll(multiChannels(ctrlApiModbusTcp, "CumulatedActiveTime", DataType.LONG)) //
				.putAll(multiChannels(ctrlApiModbusTcp, "CumulatedInactiveTime", DataType.LONG)) //
				.put("ctrlEssLimiter14a0/CumulatedRestrictionTime", DataType.LONG) //
				.putAll(multiChannels(ctrlEssRippleControlReceiver, "CumulatedRestrictionTime", DataType.LONG)) //
				.putAll(multiChannels(heat, "ActiveConsumptionEnergy", DataType.LONG)) // @Deprecated(use=ActiveProductionEnergy)
				.putAll(multiChannels(heat, "ActiveProductionEnergy", DataType.LONG)) //
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

	private record Namespace(//
			String componentId, //
			int from, //
			int to // exclude
	) {

	}

	protected static Iterable<Entry<String, DataType>> multiChannels(//
			final Namespace namespace, //
			final String channelOfComponent, //
			final DataType type //
	) {
		return multiChannels(namespace.componentId(), namespace.from(), namespace.to(), channelOfComponent, type);
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
			final Namespace namespace, //
			final String channelOfComponent, //
			final int fromChannel, //
			final int toChannel, //
			final DataType type //
	) {
		return multiChannels(namespace.componentId(), namespace.from(), namespace.to(), channelOfComponent, fromChannel,
				toChannel, type);
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
		if (from >= to) {
			throw new IllegalArgumentException("to must be greater than from.");
		}
		return IntStream.range(from, to) //
				.mapToObj(componentNumber -> {
					return IntStream.range(fromChannel, toChannel) //
							.mapToObj(channelNumber -> {
								return component + componentNumber + "/" + channelOfComponent + channelNumber;
							});
				}).flatMap(t -> t).collect(Collectors.toMap(t -> t, t -> type)).entrySet();
	}

}
