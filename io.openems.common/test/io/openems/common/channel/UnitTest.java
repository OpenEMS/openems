package io.openems.common.channel;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UnitTest {

	@Test
	public void testFromSymbolOrElse() {
		assertEquals(Unit.AMPERE, Unit.fromSymbolOrElse("A", Unit.NONE));
		assertEquals(Unit.NONE, Unit.fromSymbolOrElse("FOOBAR", Unit.NONE));
		assertEquals(Unit.DEGREE_CELSIUS, Unit.fromSymbolOrElse("C", Unit.NONE));
		assertEquals(Unit.DEGREE_CELSIUS, Unit.fromSymbolOrElse("°C", Unit.NONE));
	}

}
