package io.openems.edge.evcs.hardybarth;

import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;
import static io.openems.edge.evcs.api.Phases.THREE_PHASE;
import static io.openems.edge.evcs.api.Status.CHARGING;
import static io.openems.edge.evse.chargepoint.hardybarth.common.Constants.API_RESPONSE;
import static io.openems.edge.evse.chargepoint.hardybarth.common.Constants.EMPTY_API_RESPONSE;
import static io.openems.edge.meter.api.PhaseRotation.L2_L3_L1;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import io.openems.common.oem.DummyOpenemsEdgeOem;
import org.junit.Test;
import org.osgi.service.event.Event;

import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.channel.Level;
import io.openems.common.utils.ReflectionUtils;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.bridge.http.cycle.dummy.DummyCycleSubscriber;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evcs.api.ChargeMode;
import io.openems.edge.evcs.api.ChargeState;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.DeprecatedEvcs;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evse.chargepoint.hardybarth.common.HardyBarth;
import io.openems.edge.evse.chargepoint.hardybarth.common.LogVerbosity;
import io.openems.edge.meter.api.ElectricityMeter;

public class EvcsHardyBarthImplTest {

	@Test
	public void test() throws Exception {
		final var phaseRotation = L2_L3_L1;
		var sut = new EvcsHardyBarthImpl();
		var test = new ComponentTest(sut) //
                .addReference("oem", new DummyOpenemsEdgeOem()) //
				.addReference("httpBridgeFactory",
						ofBridgeImpl(DummyBridgeHttpFactory::dummyEndpointFetcher,
								DummyBridgeHttpFactory::dummyBridgeHttpExecutor)) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber()))
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setIp("192.168.8.101") //
						.setMaxHwCurrent(32_000) //
						.setMinHwCurrent(6_000) //
						.setPhaseRotation(phaseRotation) //
						.setLogVerbosity(LogVerbosity.NONE) //
						.build());
		var rh = ReflectionUtils.<EvcsHandler>getValueViaReflection(sut, "handler");
		test //
				.next(new TestCase() //
						.activateStrictMode() //
						.onBeforeProcessImage(
								() -> rh.handleGetApiCallResponse(HttpResponse.ok(API_RESPONSE), phaseRotation)) //

						.output(DeprecatedEvcs.ChannelId.CHARGE_POWER, 3192) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 4658050L) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 3192) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1044) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1075) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1073) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 4658050L) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, null) //
						.output(ElectricityMeter.ChannelId.CURRENT, 14_770) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 4_770) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 5_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 5_000) //
						.output(ElectricityMeter.ChannelId.FREQUENCY, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 216_156) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 218_868) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 215_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 214_600) //

						.output(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED, false) //
						.output(Evcs.ChannelId.CHARGING_TYPE, ChargingType.AC) //
						.output(Evcs.ChannelId.ENERGY_SESSION, 3460) //
						.output(Evcs.ChannelId.FIXED_MAXIMUM_HARDWARE_POWER, 22080) //
						.output(Evcs.ChannelId.FIXED_MINIMUM_HARDWARE_POWER, 4140) //
						.output(Evcs.ChannelId.MAXIMUM_HARDWARE_POWER, 22080) //
						.output(Evcs.ChannelId.MAXIMUM_POWER, null) //
						.output(Evcs.ChannelId.MINIMUM_HARDWARE_POWER, 4140) //
						.output(Evcs.ChannelId.MINIMUM_POWER, null) //
						.output(Evcs.ChannelId.PHASES, THREE_PHASE) //
						.output(Evcs.ChannelId.STATUS, CHARGING) //

						.output(HardyBarth.ChannelId.METER_NOT_AVAILABLE, false) //
						.output(HardyBarth.ChannelId.RAW_ACTIVE_ENERGY_EXPORT, 0.0) //
						.output(HardyBarth.ChannelId.RAW_ACTIVE_ENERGY_TOTAL, 4658050.0) //
						.output(HardyBarth.ChannelId.RAW_CABLE_CURRENT_LIMIT, "-1") //
						.output(HardyBarth.ChannelId.RAW_CHARGE_STATUS_CHARGEPOINT, "C") //
						.output(HardyBarth.ChannelId.RAW_CHARGE_STATUS_CONTACTOR, "closed") //
						.output(HardyBarth.ChannelId.RAW_CHARGE_STATUS_PLUG, "locked") //
						.output(HardyBarth.ChannelId.RAW_CHARGE_STATUS_PWM, "10.00") //
						.output(HardyBarth.ChannelId.RAW_CHARGING, "1") //
						.output(HardyBarth.ChannelId.RAW_CONTACTOR_ACTUAL, "1") //
						.output(HardyBarth.ChannelId.RAW_CONTACTOR_ERROR, "0") //
						.output(HardyBarth.ChannelId.RAW_CONTACTOR_HLC_TARGET, "0") //
						.output(HardyBarth.ChannelId.RAW_CONTACTOR_TARGET, "1") //
						.output(HardyBarth.ChannelId.RAW_CP_STATE, "C") //
						.output(HardyBarth.ChannelId.RAW_DEVICE_HARDWARE_VERSION, "1.0") //
						.output(HardyBarth.ChannelId.RAW_DEVICE_HOSTNAME, "salia") //
						.output(HardyBarth.ChannelId.RAW_DEVICE_MAC_ADDRESS, "00:01:87:13:12:34") //
						.output(HardyBarth.ChannelId.RAW_DEVICE_MODELNAME, "Salia PLCC Slave") //
						.output(HardyBarth.ChannelId.RAW_DEVICE_PRODUCT, "2310007") //
						.output(HardyBarth.ChannelId.RAW_DEVICE_SERIAL, 101249323L) //
						.output(HardyBarth.ChannelId.RAW_DEVICE_SOFTWARE_VERSION, "1.50.0") //
						.output(HardyBarth.ChannelId.RAW_DEVICE_UUID, "5491ad62-022a-4356-a32c-00018713102x") //
						.output(HardyBarth.ChannelId.RAW_DEVICE_VCS_VERSION, "V0R5e") //
						.output(HardyBarth.ChannelId.RAW_DIODE_PRESENT, "1") //
						.output(HardyBarth.ChannelId.RAW_EMERGENCY_SHUTDOWN, "0") //
						.output(HardyBarth.ChannelId.RAW_EVSE_GRID_CURRENT_LIMIT, 16) //
						.output(HardyBarth.ChannelId.RAW_EV_PRESENT, "1") //
						.output(HardyBarth.ChannelId.RAW_GRID_CURRENT_LIMIT, "6") //
						.output(HardyBarth.ChannelId.RAW_METER_AVAILABLE, true) //
						.output(HardyBarth.ChannelId.RAW_METER_SERIALNUMBER, "21031835") //
						.output(HardyBarth.ChannelId.RAW_METER_TYPE, "klefr") //
						.output(HardyBarth.ChannelId.RAW_PHASE_COUNT, 3) //
						.output(HardyBarth.ChannelId.RAW_PLUG_LOCK_ERROR, "0") //
						.output(HardyBarth.ChannelId.RAW_PLUG_LOCK_STATE_ACTUAL, "1") //
						.output(HardyBarth.ChannelId.RAW_PLUG_LOCK_STATE_TARGET, "1") //
						.output(HardyBarth.ChannelId.RAW_RCD_AVAILABLE, false) //
						.output(HardyBarth.ChannelId.RAW_RFID_AUTHORIZEREQ, "") //
						.output(HardyBarth.ChannelId.RAW_RFID_AVAILABLE, false) //
						.output(HardyBarth.ChannelId.RAW_SALIA_AUTHMODE, "free") //
						.output(HardyBarth.ChannelId.RAW_SALIA_CHANGE_METER, null) //
						.output(HardyBarth.ChannelId.RAW_SALIA_CHARGE_MODE, "power") //
						.output(HardyBarth.ChannelId.RAW_SALIA_CHARGE_PAUSE, 0) //
						.output(HardyBarth.ChannelId.RAW_SALIA_FIRMWAREPROGRESS, "0") //
						.output(HardyBarth.ChannelId.RAW_SALIA_FIRMWARESTATE, "idle") //
						.output(HardyBarth.ChannelId.RAW_SALIA_PUBLISH, null) //
						.output(HardyBarth.ChannelId.RAW_SESSION_AUTHORIZATION_METHOD, null) //
						.output(HardyBarth.ChannelId.RAW_SESSION_SLAC_STARTED, null) //
						.output(HardyBarth.ChannelId.RAW_SESSION_STATUS_AUTHORIZATION, "") //
						.output(HardyBarth.ChannelId.RAW_SLAC_ERROR, null) //
						.output(HardyBarth.ChannelId.RAW_VENTILATION_AVAILABLE, false) //
						.output(HardyBarth.ChannelId.RAW_VENTILATION_STATE_ACTUAL, "0") //
						.output(HardyBarth.ChannelId.RAW_VENTILATION_STATE_TARGET, null) //

						.output(ManagedEvcs.ChannelId.CHARGE_MODE, ChargeMode.FORCE_CHARGE) //
						.output(ManagedEvcs.ChannelId.CHARGE_STATE, ChargeState.CHARGING) //
						.output(ManagedEvcs.ChannelId.IS_CLUSTERED, null) //
						.output(ManagedEvcs.ChannelId.POWER_PRECISION, 230.0) //
						.output(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT, null) //
						.output(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT_WITH_FILTER, null) //
						.output(ManagedEvcs.ChannelId.SET_CHARGE_POWER_REQUEST, null) //
						.output(ManagedEvcs.ChannelId.SET_DISPLAY_TEXT, null) //
						.output(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT, null) //

						.output(OpenemsComponent.ChannelId.STATE, Level.OK) //
				);
	}

	@Test
	public void testSetManualMode() throws Exception {
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var phaseRotation = L2_L3_L1;
		final var cycleSub = new DummyCycleSubscriber();
		var sut = new EvcsHardyBarthImpl();
		var test = new ComponentTest(sut) //
                .addReference("oem", new DummyOpenemsEdgeOem()) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("httpBridgeCycleServiceDefinition", new HttpBridgeCycleServiceDefinition(cycleSub))
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setIp("192.168.8.101") //
						.setMaxHwCurrent(32_000) //
						.setMinHwCurrent(6_000) //
						.setPhaseRotation(phaseRotation) //
						.setLogVerbosity(LogVerbosity.NONE) //
						.build());
		var rh = ReflectionUtils.<EvcsHandler>getValueViaReflection(sut, "handler");
		var expect = httpTestBundle.expect("http://192.168.8.101/api")//
				.toBeCalled();

		test.next(new TestCase() //
				.onBeforeProcessImage(() -> {
					rh.handleGetApiCallResponse(HttpResponse.ok(API_RESPONSE), phaseRotation);
					httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("ok"));
				}));
		cycleSub.handleEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, Collections.emptyMap()));
		assertTrue(expect.get());
		expect = httpTestBundle.expect(t -> {
			// is chargemode set to manual
			return t.body().equals("{\"salia/chargemode\":\"manual\"}");
		}).toBeCalled();

		test.next(new TestCase() //
				.onBeforeProcessImage(() -> {
					rh.handleGetApiCallResponse(HttpResponse.ok(API_RESPONSE), phaseRotation);
					httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("ok"));
				})).next(new TestCase()); //
		assertTrue(expect.get());

	}

	@Test
	public void testHandleUndefinedCheck() throws Exception {
		final var phaseRotation = L2_L3_L1;
		var sut = new EvcsHardyBarthImpl();
		var test = new ComponentTest(sut) //
                .addReference("oem", new DummyOpenemsEdgeOem()) //
				.addReference("httpBridgeFactory",
						ofBridgeImpl(DummyBridgeHttpFactory::dummyEndpointFetcher,
								DummyBridgeHttpFactory::dummyBridgeHttpExecutor)) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber()))
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setIp("192.168.8.101") //
						.setMaxHwCurrent(32_000) //
						.setMinHwCurrent(6_000) //
						.setPhaseRotation(phaseRotation) //
						.setLogVerbosity(LogVerbosity.NONE) //
						.build());
		var rh = ReflectionUtils.<EvcsHandler>getValueViaReflection(sut, "handler");
		test //
				.next(new TestCase() //
						.onBeforeProcessImage(
								() -> rh.handleGetApiCallResponse(HttpResponse.ok(API_RESPONSE), phaseRotation)) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 3192) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1044) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1075) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1073) //
						.output(ElectricityMeter.ChannelId.CURRENT, 14_770) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 4_770) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 5_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 5_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 216_156) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 218_868) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 215_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 214_600) //
				)
				// Values are not overwritten when empty/null response from api
				.next(new TestCase() //
						.onBeforeProcessImage(
								() -> rh.handleGetApiCallResponse(HttpResponse.ok(EMPTY_API_RESPONSE), phaseRotation)) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 3192) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1044) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1075) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1073) //
						.output(ElectricityMeter.ChannelId.CURRENT, 14_770) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 4_770) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 5_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 5_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 216_156) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 218_868) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 215_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 214_600) //
				);
	}
}
