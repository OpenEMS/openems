//package io.openems.edge.energy.api;
//
//import org.junit.Test;
//
//import io.openems.common.exceptions.OpenemsException;
//import io.openems.edge.common.component.OpenemsComponent;
//import io.openems.edge.controller.api.Controller;
//import io.openems.edge.energy.api.controller.DummyBalancingController;
//import io.openems.edge.energy.api.controller.DummyFixActivePowerController;
//import io.openems.edge.energy.api.device.MyDummyEss;
//
//public class EnergyApiTest {
//
//	@Test
//	public void test() throws OpenemsException {
//		var forecast = ForecastTest.autumn24();
//		var ess0 = new MyDummyEss("ess0");
//		var ctrlEss0 = new DummyFixActivePowerController("ctrlEss0", ess0);
//		var ctrlEss1 = new DummyBalancingController("ctrlEss1", ess0);
//		// var evcs0 = new MyDummyEvcs("evcs0");
//		// var ctrlEvcs0 = new DummyEvcsController("ctrlEvcs0", evcs0);
//		// var components = new OpenemsComponent[] { ess0, ctrlEss0, ctrlEss1, evcs0,
//		// ctrlEvcs0 };
//		//	var scheduler = new Controller[] { ctrlEvcs0, ctrlEss0, ctrlEss1 }; // Dummy-Simulator
//		var components = new OpenemsComponent[] { ess0, ctrlEss0, ctrlEss1 };
//		var scheduler = new Controller[] { ctrlEss0, ctrlEss1 }; // Dummy-Simulator
//
//		var ep = Utils.getBestExecutionPlan(forecast, components, scheduler);
//		ep.print();
//		ep.plot();
//	}
//}
