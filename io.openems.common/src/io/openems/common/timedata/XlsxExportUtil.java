package io.openems.common.timedata;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.XlsxExportDetailData.XlsxExportCategory;
import io.openems.common.timedata.XlsxExportDetailData.XlsxExportDataEntry;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.CurrencyConfig;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.MeterType;
import io.openems.common.utils.JsonUtils;

public class XlsxExportUtil {

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
				case Natures.METER -> {
					final var props = component.getProperties();
					var meterType = JsonUtils.<MeterType>getAsOptionalEnum(MeterType.class, props.get("type"))
							.orElse(null);
					if (meterType != null) {
						var list = switch (meterType) {
						case CONSUMPTION_METERED, CONSUMPTION_NOT_METERED, MANAGED_CONSUMPTION_METERED -> consumption;
						case PRODUCTION -> production;
						case GRID, PRODUCTION_AND_CONSUMPTION -> null;
						};
						if (list != null) {
							list.add(new XlsxExportDataEntry(component.getAlias(),
									new ChannelAddress(component.getId(), "ActivePower"),
									XlsxExportDataEntry.HistoricTimedataSaveType.POWER));
						}
						continue;
					}

					final var activePowerType = getActivePowerType(component.getFactoryId());
					if (activePowerType == null) {
						continue;
					}
					enumMap.get(activePowerType)
							.add(new XlsxExportDataEntry(component.getAlias(),
									new ChannelAddress(component.getId(), "ActivePower"),
									XlsxExportDataEntry.HistoricTimedataSaveType.POWER));
				}
				case Natures.TIME_OF_USE_TARIFF -> {
					tou.add(new XlsxExportDataEntry(component.getAlias(), new ChannelAddress("_sum", "GridBuyPrice"),
							XlsxExportDataEntry.HistoricTimedataSaveType.POWER));
				}
				}
			}
		}
		return new XlsxExportDetailData(enumMap, XlsxExportUtil.getCurrency(edgeConfig));
	}

	private static XlsxExportCategory getActivePowerType(String factoryId) {
		if (Natures.PRODUCTION_NATURES.contains(factoryId)) {
			return XlsxExportCategory.PRODUCTION;
		} else if (Natures.CONSUMPTION_NATURES.contains(factoryId)) {
			return XlsxExportCategory.CONSUMPTION;
		}
		return null;
	}

	private static final class Natures {
		public static final String METER = "io.openems.edge.meter.api.ElectricityMeter";
		public static final String TIME_OF_USE_TARIFF = "io.openems.edge.timeofusetariff.api.TimeOfUseTariff";
		public static final Set<String> PRODUCTION_NATURES = Set.of("Simulator.PvInverter", "Fenecon.Dess.PvMeter",
				"Fenecon.Mini.PvMeter", "Kaco.BlueplanetHybrid10.PvInverter", "PvInverter.Cluster",
				"PV-Inverter.Fronius", "PV-Inverter.KACO.blueplanet", "PV-Inverter.SMA.SunnyTripower",
				"PV-Inverter.Kostal", "PV-Inverter.Solarlog", "Simulator.ProductionMeter.Acting",
				"SolarEdge.PV-Inverter");

		public static final Set<String> CONSUMPTION_NATURES = Set.of("GoodWe.EmergencyPowerMeter",
				"Simulator.NRCMeter.Acting", "Evcs.AlpitronicHypercharger", "Evcs.Dezony", "Evcs.Goe.ChargerHome",
				"Evcs.HardyBarth", "Evcs.Keba.KeContact", "Evcs.Ocpp.Abl", "Evcs.Ocpp.IesKeywattSingle",
				"Evcs.Spelsberg.SMART", "Evcs.Webasto.Next", "Evcs.Webasto.Unite");
	}

}
