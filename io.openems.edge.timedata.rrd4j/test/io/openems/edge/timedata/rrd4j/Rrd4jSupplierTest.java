package io.openems.edge.timedata.rrd4j;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.rrd4j.ConsolFun;

import io.openems.common.channel.Unit;

public class Rrd4jSupplierTest {

	@Test
	public void testGetDsDefForChannel() {
		for (var unit : Unit.values()) {
			final var def = Rrd4jSupplier.getDsDefForChannel(unit);
			if (unit.isCumulated()) {
				assertEquals(def.consolFun(), ConsolFun.MAX);
			} else {
				assertEquals(def.consolFun(), ConsolFun.AVERAGE);
			}
		}
	}

}
