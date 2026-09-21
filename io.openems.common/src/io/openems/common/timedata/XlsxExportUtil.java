package io.openems.common.timedata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.XlsxExportDetailData.XlsxExportCategory;
import io.openems.common.timedata.XlsxExportDetailData.XlsxExportDataEntry;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.CurrencyConfig;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.MeterType;
import io.openems.common.utils.JsonUtils;

public final class XlsxExportUtil {

	private XlsxExportUtil() {
		// utlity class
	}

	/**
	 * Gathers the detail data for excel export.
	 *
	 * @param edge the edge
	 * @return the currency represented as a CurrencyConfig
	 * @throws OpenemsNamedException if component isnt found
	 */
	private static CurrencyConfig getCurrency(EdgeConfig edge) throws OpenemsNamedException {
		return edge.getComponent("_meta") //
				.flatMap(t -> t.getProperty("currency")) //
				.flatMap(t -> JsonUtils.getAsOptionalEnum(CurrencyConfig.class, t)) //
				.orElse(CurrencyConfig.EUR);
	}

	/**
	 * Gathers the detail data for excel export.
	 *
	 * @param edgeConfig the {@link EdgeConfig}
	 * @return the {@link XlsxExportDetailData}
	 * @throws OpenemsNamedException if component is not found
	 */
	public static XlsxExportDetailData getDetailData(EdgeConfig edgeConfig) throws OpenemsNamedException {
		final var enumMap = new EnumMap<XlsxExportCategory, List<XlsxExportDataEntry>>(XlsxExportCategory.class);
		final var consumption = new ArrayList<XlsxExportDetailData.XlsxExportDataEntry>();
		final var production = new ArrayList<XlsxExportDetailData.XlsxExportDataEntry>();
		final var tou = new ArrayList<XlsxExportDetailData.XlsxExportDataEntry>();

		enumMap.put(XlsxExportCategory.PRODUCTION, production);
		enumMap.put(XlsxExportCategory.CONSUMPTION, consumption);
		enumMap.put(XlsxExportCategory.TIME_OF_USE_TARIFF, tou);

		for (var component : edgeConfig.getComponents().values()) {
			final var factory = edgeConfig.getFactories().get(component.getFactoryId());
			if (factory == null) {
				continue;
			}
			for (var nature : factory.getNatureIds()) {
				// Electricity meter
				switch (nature) {
				case Natures.DC_CHARGER -> {
					production.add(new XlsxExportDataEntry(component.getAlias(), //
							new ChannelAddress(component.getId(), "ActualPower"), //
							XlsxExportDataEntry.HistoricTimedataSaveType.POWER));
				}
				case Natures.METER -> {
					final var channelId = Stream.of(factory.getNatureIds())
							.anyMatch(t -> t.equals(Natures.DEPRECATED_EVCS)) ? "ChargePower" : "ActivePower";
					final var props = component.getProperties();
					var meterType = JsonUtils.<MeterType>getAsOptionalEnum(MeterType.class, props.get("type"))
							.orElse(null);
					if (meterType != null) {
						var list = switch (meterType) {
						case CONSUMPTION_METERED, CONSUMPTION_NOT_METERED, MANAGED_CONSUMPTION_METERED -> consumption;
						case PRODUCTION -> production;
						case GRID, GRID_GENSET, PRODUCTION_AND_CONSUMPTION -> null;
						};
						if (list != null) {
							list.add(new XlsxExportDataEntry(component.getAlias(),
									new ChannelAddress(component.getId(), channelId),
									XlsxExportDataEntry.HistoricTimedataSaveType.POWER));
						}
						continue;
					}

					final var activePowerType = getActivePowerType(factory.getNatureIds());
					if (activePowerType == null) {
						continue;
					}
					enumMap.get(activePowerType)
							.add(new XlsxExportDataEntry(component.getAlias(),
									new ChannelAddress(component.getId(), channelId),
									XlsxExportDataEntry.HistoricTimedataSaveType.POWER));
				}
				case Natures.TIME_OF_USE_TARIFF -> {
					final var controllerIds = edgeConfig.getComponentIdsByFactory("Controller.Ess.Time-Of-Use-Tariff");

					tou.add(new XlsxExportDataEntry(component.getAlias(),
							new ChannelAddress(controllerIds.getFirst(), "QuarterlyPrices"),
							XlsxExportDataEntry.HistoricTimedataSaveType.POWER));
				}
				}
			}
		}
		return new XlsxExportDetailData(enumMap, XlsxExportUtil.getCurrency(edgeConfig));
	}

	private static XlsxExportCategory getActivePowerType(String[] natureIds) {
		if (natureIds == null || natureIds.length == 0) {
			return null;
		}

		Set<String> natureSet = Set.of(natureIds);

		if (!Collections.disjoint(natureSet, Natures.EXCLUDED_NATURES)) {
			return null;
		}

		if (!Collections.disjoint(natureSet, Natures.CONSUMPTION_NATURES)) {
			return XlsxExportCategory.CONSUMPTION;
		}

		if (!Collections.disjoint(natureSet, Natures.PRODUCTION_NATURES)) {
			return XlsxExportCategory.PRODUCTION;
		}

		return null;
	}

	private static final class Natures {
		public static final String METER = "io.openems.edge.meter.api.ElectricityMeter";
		public static final String TIME_OF_USE_TARIFF = "io.openems.edge.timeofusetariff.api.TimeOfUseTariff";
		public static final String DC_CHARGER = "io.openems.edge.ess.dccharger.api.EssDcCharger";
		public static final String DEPRECATED_EVCS = "io.openems.edge.evcs.api.DeprecatedEvcs";
		public static final Set<String> EXCLUDED_NATURES = Set.of(//
				"io.openems.edge.evcs.api.MetaEvcs", //
				"io.openems.edge.pvinverter.cluster.PvInverterCluster"//
		);
		public static final Set<String> CONSUMPTION_NATURES = Set.of(//
				"io.openems.edge.goodwe.emergencypowermeter.GoodWeEmergencyPowerMeter", //
				"io.openems.edge.evse.api.chargepoint.EvseChargePoint", //
				"io.openems.edge.evcs.api.Evcs", //
				"io.openems.edge.heat.api.Heat", //
				"io.openems.edge.simulator.meter.nrc.acting.SimulatorNrcMeterActing" //
		);

		public static final Set<String> PRODUCTION_NATURES = Set.of(//
				"io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter", //
				"io.openems.edge.fenecon.dess.pvmeter.FeneconDessPvMeter", //
				"io.openems.edge.simulator.meter.production.acting.SimulatorProductionMeterActing" //
		);
	}

}
