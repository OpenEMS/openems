package io.openems.edge.batteryinverter.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.function.Supplier;

import org.junit.Test;

import io.openems.common.session.Language;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;

public class DummyManagedSymmetricBatteryInverterTest {

	@Test
	public void testCalculateApparentPowerFromActiveAndReactivePower() {
		final var sut = new DummyManagedSymmetricBatteryInverter("batteryInverter0");
		final Supplier<Integer> apparentPower = () -> sut.getApparentPowerChannel().getNextValue().get();

		SymmetricBatteryInverter.calculateApparentPowerFromActiveAndReactivePower(sut);

		sut.withActivePower(2000);
		assertNull(apparentPower.get());

		sut.withReactivePower(1000);
		assertEquals(2236, apparentPower.get().intValue());

		sut.withActivePower(null);
		assertNull(apparentPower.get());
	}

	@Test
	public void testTranslations() {
		final var sut = new DummyManagedSymmetricBatteryInverter("batteryInverter0");

		var activePowerDoc = sut.getActivePowerChannel().channelDoc();
		assertEquals("Negative Werte für Beladung; Positive für Entladung", activePowerDoc.getText(Language.DE));
		assertEquals("Negative values for Charge; positive for Discharge", activePowerDoc.getText(Language.EN));
	}
}
