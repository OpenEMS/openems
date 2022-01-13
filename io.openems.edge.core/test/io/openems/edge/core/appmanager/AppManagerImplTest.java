package io.openems.edge.core.appmanager;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;

public class AppManagerImplTest {

	@Test
	public void test() throws OpenemsException, Exception {
//		DummyManagedSymmetricEss ess0 = new DummyManagedSymmetricEss("ess0");
//		final DummyConfigurationAdmin cm = new DummyConfigurationAdmin();
//		cm.getOrCreateEmptyConfiguration(AppManager.SINGLETON_SERVICE_PID);
//
//		final DummyComponentManager componentManger = new DummyComponentManager();
//		componentManger.setConfigJson(JsonUtils.buildJsonObject() //
//				.add("components", JsonUtils.buildJsonObject() //
//						.add("ess0", JsonUtils.buildJsonObject() //
//								.addProperty("factoryId", "Ess.Generic.ManagedSymmetric") //
//								.add("properties", JsonUtils.buildJsonObject() //
//										.addProperty("enabled", true) //
//										.addProperty("startStop", "START") //
//										.addProperty("batteryInverter.id", "batteryInverter0") //
//										.addProperty("battery.id", "battery0") //
//										.build()) //
//								.build()) //
//						.add("batteryInverter0", JsonUtils.buildJsonObject() //
//								.addProperty("factoryId", "GoodWe.BatteryInverter") //
//								.add("properties", JsonUtils.buildJsonObject() //
//										.addProperty("enabled", true) //
//										.addProperty("modbus.id", "modbus1") //
//										.addProperty("modbusUnitId", 247) //
//										.addProperty("safetyCountry", "AUSTRIA") //
//										.addProperty("backupEnable", "ENABLE") //
//										.addProperty("feedPowerEnable", "ENABLE") //
//										.addProperty("feedPowerPara", 10000) //
//										.addProperty("setfeedInPowerSettings", "LAGGING_0_95") //
//										.build()) //
//								.build()) //
//						.build()) //
//				.add("factories", JsonUtils.buildJsonObject() //
//						.build()) //
//				.build() //
//		);
//
//		final AppManagerImpl sut = new AppManagerImpl();
//		new ComponentTest(sut) //
//				.addReference("cm", cm) //
//				.addReference("componentManager", componentManger) //
//				.addReference("availableApps", ImmutableList.of(new FeneconHome(componentManger))) //
//				.addComponent(ess0) //
//				.activate(MyConfig.create() //
//						.setApps(JsonUtils.buildJsonArray() //
//								.add(JsonUtils.buildJsonObject() //
//										.addProperty("class", "io.openems.edge.app.FeneconHome") //
////										.addProperty("registrationKey", "") //
//										.add("properties", JsonUtils.buildJsonObject() //
//												.addProperty("ESS_ID", "ess0") //
//												.addProperty("SAFETY_COUNTRY", "AUSTRIA") //
//												.addProperty("BACKUP_ENABLE", "ENABLE") //
//												.addProperty("MAX_FEED_IN_POWER", 10000) //
//												.addProperty("FEED_IN_SETTING", "LAGGING_0_95") //
//												.build()) //
//										.build()) //
//								.build().toString()) //
//						.build());
//
//		AppValidateWorker worker = new AppValidateWorker(sut);
//		worker.validateApps();
//
//		// should not have found defective Apps
//		for (Entry<String, String> entry : worker.defectiveApps.entrySet()) {
//			throw new Exception(entry.getKey() + ": " + entry.getValue());
//		}
	}

}
