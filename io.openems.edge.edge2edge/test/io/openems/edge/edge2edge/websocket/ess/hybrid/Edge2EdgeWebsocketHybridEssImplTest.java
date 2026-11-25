package io.openems.edge.edge2edge.websocket.ess.hybrid;

import static io.openems.edge.edge2edge.websocket.ess.hybrid.Edge2EdgeWebsocketHybridEss.ChannelId.PV_PRODUCTION;
import static io.openems.edge.edge2edge.websocket.ess.hybrid.Edge2EdgeWebsocketHybridEss.ChannelId.SURPLUS_POWER_VALUE;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER;

import org.junit.jupiter.api.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.edge2edge.websocket.dccharger.Edge2EdgeWebsocketDcChargerImpl;

class Edge2EdgeWebsocketHybridEssImplTest {

	@Test
	void testDcChargerReferences() throws Exception {
		var ess = new Edge2EdgeWebsocketHybridEssImpl();
		var firstCharger = new Edge2EdgeWebsocketDcChargerImpl();
		var secondCharger = new Edge2EdgeWebsocketDcChargerImpl();
		ess.addDcCharger(firstCharger);
		ess.addDcCharger(secondCharger);

		firstCharger._setActualPower(200);
		firstCharger.getActualPowerChannel().nextProcessImage();
		secondCharger._setActualPower(300);
		secondCharger.getActualPowerChannel().nextProcessImage();

		var test = new ComponentTest(ess) //
				.activate(MyConfig.create() //
						.setDcChargerIds("charger0", "charger1") //
						.build()) //
				.next(new TestCase() //
						.input(ALLOWED_CHARGE_POWER, -100) //
						.output(PV_PRODUCTION, 500) //
						.output(SURPLUS_POWER_VALUE, 400)); //

		secondCharger._setActualPower(null);
		secondCharger.getActualPowerChannel().nextProcessImage();
		test.next(new TestCase() //
				.output(PV_PRODUCTION, 200) //
				.output(SURPLUS_POWER_VALUE, 100)); //

		firstCharger._setActualPower(null);
		firstCharger.getActualPowerChannel().nextProcessImage();
		test.next(new TestCase() //
				.output(PV_PRODUCTION, null) //
				.output(SURPLUS_POWER_VALUE, null)); //

		firstCharger._setActualPower(200);
		firstCharger.getActualPowerChannel().nextProcessImage();
		secondCharger._setActualPower(300);
		secondCharger.getActualPowerChannel().nextProcessImage();
		ess.removeDcCharger(firstCharger);
		test.next(new TestCase() //
				.output(PV_PRODUCTION, 300) //
				.output(SURPLUS_POWER_VALUE, 200)); //

		ess.addDcCharger(firstCharger);
		test.next(new TestCase() //
				.input(ALLOWED_CHARGE_POWER, -500) //
				.output(PV_PRODUCTION, 500) //
				.output(SURPLUS_POWER_VALUE, 0)) //
				.deactivate();
	}

}
