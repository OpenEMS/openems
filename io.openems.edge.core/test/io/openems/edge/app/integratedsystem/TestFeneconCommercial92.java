package io.openems.edge.app.integratedsystem;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercial92;
import io.openems.edge.app.meter.SocomecMeter;
import io.openems.edge.common.test.DummyUser;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;

public class TestFeneconCommercial92 {

	private AppManagerTestBundle appManagerTestBundle;

	private SocomecMeter meterApp;

	@Before
	public void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return List.of(//
					Apps.feneconCommercial92(t), //
					Apps.gridOptimizedCharge(t), //
					Apps.selfConsumptionOptimization(t), //
					Apps.prepareBatteryExtension(t), //
					Apps.kdkMeter(t), //
					this.meterApp = Apps.socomecMeter(t) //
			);
		}, null, new AppManagerTestBundle.PseudoComponentManagerFactory());

		final var componentTask = this.appManagerTestBundle.addComponentAggregateTask();
		this.appManagerTestBundle.addSchedulerByCentralOrderAggregateTask(componentTask);
	}

	@Test
	public void testGetMeterDefaultModbusIdValue() throws Exception {
		this.createFullCommercial92();

		final var modbusIdProperty = Arrays.stream(this.meterApp.getProperties()) //
				.filter(t -> t.name.equals(SocomecMeter.Property.MODBUS_ID.name())) //
				.findFirst().orElseThrow();

		assertEquals("modbus3", modbusIdProperty.getDefaultValue(Language.DEFAULT) //
				.map(JsonElement::getAsString) //
				.orElseThrow());
	}

	private void createFullCommercial92() throws Exception {
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DummyUser.DUMMY_ADMIN,
				new AddAppInstance.Request("App.FENECON.Commercial.92", "key", "alias", fullSettings()));
	}

	/**
	 * Gets a {@link JsonObject} with the full settings for a
	 * {@link FeneconCommercial92}.
	 *
	 * @return the settings object
	 */
	public static JsonObject fullSettings() {
		return JsonUtils.buildJsonObject() //
				.addProperty("SAFETY_COUNTRY", "GERMANY") //
				.addProperty("GRID_CODE", "VDE_4105")
				.addProperty("FEED_IN_TYPE", "DYNAMIC_LIMITATION") //
				.addProperty("MAX_FEED_IN_POWER", 1000) //
				.addProperty("HAS_ESS_LIMITER_14A", false) //
				.addProperty("BATTERY_TARGET", "AUTO") //
				.build();
	}

}
