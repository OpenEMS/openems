package io.openems.common.timedata;

import static io.openems.common.utils.JsonUtils.toJson;
import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.XlsxExportDetailData.XlsxExportCategory;
import io.openems.common.timedata.XlsxExportDetailData.XlsxExportDataEntry.HistoricTimedataSaveType;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.ActualEdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.types.EdgeConfig.Factory;
import io.openems.common.types.EdgeConfig.Factory.Property;

class XlsxExportUtilTest {

	private EdgeConfig edgeConfig;

	@BeforeEach
	void setup() {
		this.edgeConfig = ActualEdgeConfig.create() //
				.addComponent("charger0", //
						new Component("charger0", "My Charger", "Fenecon.Dess.Charger1",
								// Properties
								ImmutableSortedMap.of(),
								// Channels
								ImmutableSortedMap.of())) //
				.addComponent("meter0",
						new Component("meter0", "My CONSUMPTION_METERED Meter", "Meter.Socomec.Threephase",
								// Properties
								ImmutableSortedMap.of("type", toJson("CONSUMPTION_METERED")),
								// Channels
								ImmutableSortedMap.of())) //
				.addComponent("meter1",
						new Component("meter1", "My CONSUMPTION_NOT_METERED Meter", "Meter.Socomec.Threephase",
								// Properties
								ImmutableSortedMap.of("type", toJson("CONSUMPTION_NOT_METERED")),
								// Channels
								ImmutableSortedMap.of())) //
				.addComponent("meter2", new Component("meter2", "My PRODUCTION Meter", "Meter.Socomec.Threephase",
						// Properties
						ImmutableSortedMap.of("type", toJson("PRODUCTION")),
						// Channels
						ImmutableSortedMap.of())) //
				.addComponent("meter4", new Component("meter4", "My GoodWe Meter", "GoodWe.EmergencyPowerMeter",
						// Properties
						ImmutableSortedMap.of(),
						// Channels
						ImmutableSortedMap.of())) //
				.addComponent("doesntExist0", new Component("doesntExist0", "Null", "Not.Found",
						// Properties
						ImmutableSortedMap.of(),
						// Channels
						ImmutableSortedMap.of())) //
				.addComponent("evseChargePoint0", //
						new Component("evseChargePoint0", "My Wallbox", "Evse.ChargePoint.Keba.Modbus",
								// Properties
								ImmutableSortedMap.of(),
								// Channels
								ImmutableSortedMap.of()) //
				).addComponent("timeOfUse0", //
						new Component("timeOfUse0", "My TOU", "TimeOfUse.Tou",
								// Properties
								ImmutableSortedMap.of(),
								// Channels
								ImmutableSortedMap.of()) //
				) //
				.addComponent("ctrlTimeOfUse0", //
						new Component("ctrlTimeOfUse0", "My TOU", "Controller.Ess.Time-Of-Use-Tariff",
								// Properties
								ImmutableSortedMap.of(),
								// Channels
								ImmutableSortedMap.of()) //
				)

				.addFactory("Evse.ChargePoint.Keba.Modbus", //
						new Factory("Evse.ChargePoint.Keba.Modbus", "My Name", "My Description", //
								new Property[] {}, //
								// Natures
								new String[] { "io.openems.edge.meter.api.ElectricityMeter",
										"io.openems.edge.evse.api.chargepoint.EvseChargePoint" })) //
				.addFactory("Meter.Socomec.Threephase",
						new Factory("Meter.Socomec.Threephase", "My Name", "My Description", //
								new Property[] {}, //
								// Natures
								new String[] { "io.openems.edge.meter.api.ElectricityMeter" })) //
				.addFactory("Fenecon.Dess.Charger1", new Factory("Fenecon.Dess.Charger1", "My Name", "My Description", //
						new Property[] {}, //
						// Natures
						new String[] { "io.openems.edge.ess.dccharger.api.EssDcCharger" })) //
				.addFactory("GoodWe.EmergencyPowerMeter",
						new Factory("GoodWe.EmergencyPowerMeter", "My Name", "My Description", // )
								new Property[] {}, //
								// Natures
								new String[] { "io.openems.edge.meter.api.ElectricityMeter",
										"io.openems.edge.goodwe.emergencypowermeter.GoodWeEmergencyPowerMeter" })) //
				.addFactory("TimeOfUse.Tou", new Factory("TimeOfUse.Tou", "My Name", "My Description", //

						new Property[] {}, //
						// Natures
						new String[] { "io.openems.edge.timeofusetariff.api.TimeOfUseTariff", })) //
				.addFactory("Controller.Ess.Time-Of-Use-Tariff",
						new Factory("Controller.Ess.Time-Of-Use-Tariff", "TOU Controller", "TOU Controller",
								new Property[] {}, //
								// Natures
								new String[] { "Controller.Ess.Time-Of-Use-Tariff", })) //
				.buildEdgeConfig();

	}

	@Test
	void testGetConsumptionData() throws OpenemsNamedException {
		final var result = XlsxExportUtil.getDetailData(this.edgeConfig);

		var consumptions = result.data().get(XlsxExportCategory.CONSUMPTION);

		{
			var meter = consumptions.get(1);
			assertEquals("My CONSUMPTION_METERED Meter", meter.alias());
			assertEquals("meter0/ActivePower", meter.channel().toString());
			assertEquals(HistoricTimedataSaveType.POWER, meter.type());
		}
		{
			var meter = consumptions.get(2);
			assertEquals("My CONSUMPTION_NOT_METERED Meter", meter.alias());
			assertEquals("meter1/ActivePower", meter.channel().toString());
			assertEquals(HistoricTimedataSaveType.POWER, meter.type());
		}
		{
			var meter = consumptions.get(3);
			assertEquals("My GoodWe Meter", meter.alias());
			assertEquals("meter4/ActivePower", meter.channel().toString());
			assertEquals(HistoricTimedataSaveType.POWER, meter.type());
		}
		{
			var meter = consumptions.get(0);
			assertEquals("My Wallbox", meter.alias());
			assertEquals("evseChargePoint0/ActivePower", meter.channel().toString());
			assertEquals(HistoricTimedataSaveType.POWER, meter.type());
		}

	}

	@Test
	void testGetProductionData() throws OpenemsNamedException {
		final var result = XlsxExportUtil.getDetailData(this.edgeConfig);

		var productions = result.data().get(XlsxExportCategory.PRODUCTION);
		assertEquals(2, productions.size());

		{
			var meter = productions.get(0);
			assertEquals("My Charger", meter.alias());
			assertEquals("charger0/ActualPower", meter.channel().toString());
			assertEquals(HistoricTimedataSaveType.POWER, meter.type());
		}

		{
			var meter = productions.get(1);
			assertEquals("My PRODUCTION Meter", meter.alias());
			assertEquals("meter2/ActivePower", meter.channel().toString());
			assertEquals(HistoricTimedataSaveType.POWER, meter.type());
		}

	}

	@Test
	void testGetToutsData() throws OpenemsNamedException {
		final var result = XlsxExportUtil.getDetailData(this.edgeConfig);

		var touts = result.data().get(XlsxExportCategory.TIME_OF_USE_TARIFF);

		var tou = touts.get(0);
		assertEquals("My TOU", tou.alias());
		assertEquals("ctrlTimeOfUse0/QuarterlyPrices", tou.channel().toString());
		assertEquals(HistoricTimedataSaveType.POWER, tou.type());

	}
}
