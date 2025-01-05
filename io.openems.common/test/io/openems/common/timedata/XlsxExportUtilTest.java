package io.openems.common.timedata;

import static io.openems.common.utils.JsonUtils.toJson;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.XlsxExportDetailData.XlsxExportCategory;
import io.openems.common.timedata.XlsxExportDetailData.XlsxExportDataEntry.HistoricTimedataSaveType;
import io.openems.common.types.EdgeConfig.ActualEdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.types.EdgeConfig.Factory;
import io.openems.common.types.EdgeConfig.Factory.Property;

public class XlsxExportUtilTest {

	@Test
	public void testGetDetailData() throws OpenemsNamedException {
		var edgeConfig = ActualEdgeConfig.create() //
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
				.addComponent("meter3",
						new Component("meter3", "My MANAGED_CONSUMPTION_METERED Meter", "Meter.Socomec.Threephase",
								// Properties
								ImmutableSortedMap.of("type", toJson("MANAGED_CONSUMPTION_METERED")),
								// Channels
								ImmutableSortedMap.of())) //

				.addFactory("Meter.Socomec.Threephase",
						new Factory("Meter.Socomec.Threephase", "My Name", "My Description", //
								new Property[] {}, //
								// Natures
								new String[] { "io.openems.edge.meter.api.ElectricityMeter" })) //
				.buildEdgeConfig();

		final var result = XlsxExportUtil.getDetailData(edgeConfig);

		var consumptions = result.data().get(XlsxExportCategory.CONSUMPTION);
		assertEquals(3, consumptions.size());

		{
			var meter = consumptions.get(0);
			assertEquals("My CONSUMPTION_METERED Meter", meter.alias());
			assertEquals("meter0/ActivePower", meter.channel().toString());
			assertEquals(HistoricTimedataSaveType.POWER, meter.type());
		}
		{
			var meter = consumptions.get(1);
			assertEquals("My CONSUMPTION_NOT_METERED Meter", meter.alias());
			assertEquals("meter1/ActivePower", meter.channel().toString());
			assertEquals(HistoricTimedataSaveType.POWER, meter.type());
		}
		{
			var meter = consumptions.get(2);
			assertEquals("My MANAGED_CONSUMPTION_METERED Meter", meter.alias());
			assertEquals("meter3/ActivePower", meter.channel().toString());
			assertEquals(HistoricTimedataSaveType.POWER, meter.type());
		}

		var productions = result.data().get(XlsxExportCategory.PRODUCTION);
		assertEquals(1, productions.size());

		{
			var meter = productions.get(0);
			assertEquals("My PRODUCTION Meter", meter.alias());
			assertEquals("meter2/ActivePower", meter.channel().toString());
			assertEquals(HistoricTimedataSaveType.POWER, meter.type());
		}

		var touts = result.data().get(XlsxExportCategory.TIME_OF_USE_TARIFF);
		assertEquals(0, touts.size());
	}

}
