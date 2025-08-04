package io.openems.edge.evse.chargepoint.keba.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ProductTypeAndFeaturesTest {

	@Test
	public void testFrom() {
		{
			var ptaf = ProductTypeAndFeatures.from(4212311L);
			assertEquals(ProductTypeAndFeatures.ProductFamily.KC_P40, ptaf.productFamily());
			assertEquals(ProductTypeAndFeatures.DeviceCurrent.A32_32, ptaf.deviceCurrent());
			assertEquals(ProductTypeAndFeatures.Connector.CABLE, ptaf.connector());
			assertEquals(ProductTypeAndFeatures.Phases.THREE_PHASE, ptaf.phases());
			assertEquals(ProductTypeAndFeatures.Metering.LEGAL, ptaf.metering());
			assertEquals(ProductTypeAndFeatures.Rfid.WITH_RFID, ptaf.rfid());
			assertEquals(ProductTypeAndFeatures.Button.WITH_BUTTON, ptaf.button());
		}
	}
}
