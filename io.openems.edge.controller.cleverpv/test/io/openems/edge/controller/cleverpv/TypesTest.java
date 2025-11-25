package io.openems.edge.controller.cleverpv;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.channel.Level;
import io.openems.edge.controller.cleverpv.Types.SendData;
import io.openems.edge.controller.cleverpv.Types.SendData.ActiveControlModes;
import io.openems.edge.controller.cleverpv.Types.SendData.AvailableControlModes;
import io.openems.edge.controller.cleverpv.Types.SendData.CurrentData;
import io.openems.edge.controller.cleverpv.Types.SendData.Ess;

public class TypesTest {

	@Test
	public void testNull() {
		var sut = new SendData(null, null, null, PowerStorageState.IDLE.getValue(), null, //
				new CurrentData(null, null, null, null), //
				new AvailableControlModes(ImmutableList.of()), //
				new ActiveControlModes(new Ess(RemoteControlMode.OFF)), //
				"edge0", Level.OK.getValue()); //
		var json = SendData.serializer().serialize(sut);

		assertEquals("""
				{
					  "edgeId": "edge0",
					  "state": 0,
					  "watt": null,
					  "producingWatt": null,
					  "soc": null,
					  "powerStorageState": 0,
					  "chargingPower": null,
					  "currentData": {
						"sumGridActivePower": null,
						"productionActivePower": null,
						"sumEssSoc": null,
						"sumEssDischargePower": null
					  },
					  "availableControlModes": {
						"ess": []
					  },
					  "activeControlModes": {
						"ess" : {
						  "mode": "OFF"
						}
					  }
					}""".replaceAll("\\s+", ""), json.toString().replaceAll("\\s+", ""));

		assertEquals(sut, SendData.serializer().deserialize(json));
	}

	@Test
	public void testValues() {
		var sut = new SendData(1, 1, 1, PowerStorageState.IDLE.getValue(), 1, //
				new CurrentData(1, 1, 1, 1), //
				new AvailableControlModes(ImmutableList.of()), //
				new ActiveControlModes(new Ess(RemoteControlMode.NO_DISCHARGE)), //
				"edge0", Level.OK.getValue()); //
		var json = SendData.serializer().serialize(sut);
		assertEquals("""
				{
					  "edgeId": "edge0",
					  "state": 0,
					  "watt": 1,
					  "producingWatt": 1,
					  "soc": 1,
					  "powerStorageState": 0,
					  "chargingPower": 1,
					  "currentData": {
						"sumGridActivePower": 1,
						"productionActivePower": 1,
						"sumEssSoc": 1,
						"sumEssDischargePower": 1
					  },
					  "availableControlModes": {
						"ess": []
					  },
					  "activeControlModes": {
						"ess" : {
						  "mode": "NO_DISCHARGE"
						}
					  }
					}""".replaceAll("\\s+", ""), json.toString().replaceAll("\\s+", ""));

		assertEquals(sut, SendData.serializer().deserialize(json));
	}

}
