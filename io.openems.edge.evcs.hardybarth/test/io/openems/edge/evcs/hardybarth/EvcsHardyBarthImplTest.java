package io.openems.edge.evcs.hardybarth;

import static io.openems.common.types.HttpStatus.OK;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.ofDummyBridge;
import static io.openems.edge.evcs.api.PhaseRotation.L2_L3_L1;
import static io.openems.edge.evcs.api.Phases.THREE_PHASE;
import static io.openems.edge.evcs.api.Status.CHARGING;

import org.junit.Test;

import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evcs.api.DeprecatedEvcs;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.meter.api.ElectricityMeter;

public class EvcsHardyBarthImplTest {

	@Test
	public void test() throws Exception {
		final var phaseRotation = L2_L3_L1;
		var sut = new EvcsHardyBarthImpl();
		var ru = sut.readUtils;
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", ofDummyBridge()) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setIp("192.168.8.101") //
						.setMaxHwCurrent(32_000) //
						.setMinHwCurrent(6_000) //
						.setPhaseRotation(phaseRotation).build())

				.next(new TestCase() //
						.onBeforeProcessImage(() -> ru
								.handleGetApiCallResponse(new HttpResponse<String>(OK, API_RESPONSE), phaseRotation)) //
						.output(EvcsHardyBarth.ChannelId.RAW_EVSE_GRID_CURRENT_LIMIT, 16) //
						.output(EvcsHardyBarth.ChannelId.RAW_PHASE_COUNT, 3) //
						.output(EvcsHardyBarth.ChannelId.RAW_CHARGE_STATUS_PLUG, "locked") //
						.output(EvcsHardyBarth.ChannelId.RAW_CHARGE_STATUS_CONTACTOR, "closed") //
						.output(EvcsHardyBarth.ChannelId.RAW_CHARGE_STATUS_PWM, "10.00") //
						.output(EvcsHardyBarth.ChannelId.RAW_CHARGE_STATUS_CHARGEPOINT, "C") //
						.output(EvcsHardyBarth.ChannelId.RAW_SALIA_CHARGE_MODE, "manual") //
						.output(EvcsHardyBarth.ChannelId.RAW_SALIA_CHANGE_METER, null) //
						.output(EvcsHardyBarth.ChannelId.RAW_SALIA_AUTHMODE, "free") //
						.output(EvcsHardyBarth.ChannelId.RAW_SALIA_FIRMWARESTATE, "idle") //
						.output(EvcsHardyBarth.ChannelId.RAW_SALIA_FIRMWAREPROGRESS, "0") //
						.output(EvcsHardyBarth.ChannelId.RAW_SALIA_PUBLISH, null) //
						.output(EvcsHardyBarth.ChannelId.RAW_SESSION_STATUS_AUTHORIZATION, "") //
						.output(EvcsHardyBarth.ChannelId.RAW_SESSION_SLAC_STARTED, null) //
						.output(EvcsHardyBarth.ChannelId.RAW_SESSION_AUTHORIZATION_METHOD, null) //
						.output(EvcsHardyBarth.ChannelId.RAW_CONTACTOR_HLC_TARGET, "0") //
						.output(EvcsHardyBarth.ChannelId.RAW_CONTACTOR_ACTUAL, "1") //
						.output(EvcsHardyBarth.ChannelId.RAW_CONTACTOR_TARGET, "1") //
						.output(EvcsHardyBarth.ChannelId.RAW_CONTACTOR_ERROR, "0") //
						.output(EvcsHardyBarth.ChannelId.RAW_METER_SERIALNUMBER, "21031835") //
						.output(EvcsHardyBarth.ChannelId.RAW_METER_TYPE, "klefr") //
						.output(EvcsHardyBarth.ChannelId.RAW_METER_AVAILABLE, true) //
						.output(EvcsHardyBarth.ChannelId.METER_NOT_AVAILABLE, false) //
						.output(EvcsHardyBarth.ChannelId.RAW_ACTIVE_ENERGY_TOTAL, 4658050.0) //
						.output(EvcsHardyBarth.ChannelId.RAW_ACTIVE_ENERGY_EXPORT, 0.0) //
						.output(EvcsHardyBarth.ChannelId.RAW_EMERGENCY_SHUTDOWN, "0") //
						.output(EvcsHardyBarth.ChannelId.RAW_RCD_AVAILABLE, false) //
						.output(EvcsHardyBarth.ChannelId.RAW_PLUG_LOCK_STATE_ACTUAL, "1") //
						.output(EvcsHardyBarth.ChannelId.RAW_PLUG_LOCK_STATE_TARGET, "1") //
						.output(EvcsHardyBarth.ChannelId.RAW_PLUG_LOCK_ERROR, "0") //
						.output(EvcsHardyBarth.ChannelId.RAW_CP_STATE, "C") //
						.output(EvcsHardyBarth.ChannelId.RAW_DIODE_PRESENT, "1") //
						.output(EvcsHardyBarth.ChannelId.RAW_CABLE_CURRENT_LIMIT, "-1") //
						.output(EvcsHardyBarth.ChannelId.RAW_VENTILATION_STATE_ACTUAL, "0") //
						.output(EvcsHardyBarth.ChannelId.RAW_VENTILATION_STATE_TARGET, null) //
						.output(EvcsHardyBarth.ChannelId.RAW_VENTILATION_AVAILABLE, false) //
						.output(EvcsHardyBarth.ChannelId.RAW_EV_PRESENT, "1") //
						.output(EvcsHardyBarth.ChannelId.RAW_CHARGING, "1") //
						.output(EvcsHardyBarth.ChannelId.RAW_RFID_AUTHORIZEREQ, "") //
						.output(EvcsHardyBarth.ChannelId.RAW_RFID_AVAILABLE, false) //
						.output(EvcsHardyBarth.ChannelId.RAW_GRID_CURRENT_LIMIT, "6") //
						.output(EvcsHardyBarth.ChannelId.RAW_SLAC_ERROR, null) //
						.output(EvcsHardyBarth.ChannelId.RAW_DEVICE_PRODUCT, "2310007") //
						.output(EvcsHardyBarth.ChannelId.RAW_DEVICE_MODELNAME, "Salia PLCC Slave") //
						.output(EvcsHardyBarth.ChannelId.RAW_DEVICE_HARDWARE_VERSION, "1.0") //
						.output(EvcsHardyBarth.ChannelId.RAW_DEVICE_SOFTWARE_VERSION, "1.50.0") //
						.output(EvcsHardyBarth.ChannelId.RAW_DEVICE_VCS_VERSION, "V0R5e") //
						.output(EvcsHardyBarth.ChannelId.RAW_DEVICE_HOSTNAME, "salia") //
						.output(EvcsHardyBarth.ChannelId.RAW_DEVICE_MAC_ADDRESS, "00:01:87:13:12:34") //
						.output(EvcsHardyBarth.ChannelId.RAW_DEVICE_SERIAL, 101249323L) //
						.output(EvcsHardyBarth.ChannelId.RAW_DEVICE_UUID, "5491ad62-022a-4356-a32c-00018713102x") //

						.output(Evcs.ChannelId.ENERGY_SESSION, 3460) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 4658050L) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 4658050L) //
						.output(Evcs.ChannelId.PHASES, THREE_PHASE) //
						.output(Evcs.ChannelId.STATUS, CHARGING) //
						.output(DeprecatedEvcs.ChannelId.CHARGE_POWER, 3192) //
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

	private static final String API_RESPONSE = """
			{
			   "device":{
			      "product":"2310007",
			      "modelname":"Salia PLCC Slave",
			      "hardware_version":"1.0",
			      "software_version":"1.50.0",
			      "vcs_version":"V0R5e",
			      "hostname":"salia",
			      "mac_address":"00:01:87:13:12:34",
			      "serial":"101249323",
			      "uuid":"5491ad62-022a-4356-a32c-00018713102x",
			      "internal_id":"412009"
			   },
			   "secc":{
			      "port0":{
			         "ci":{
			            "evse":{
			               "basic":{
			                  "grid_current_limit":{
			                     "actual":"16"
			                  },
			                  "phase_count":"3",
			                  "physical_current_limit":"16",
			                  "offered_current_limit":"6.0"
			               },
			               "phase":{
			                  "actual":"3"
			               }
			            },
			            "charge":{
			               "cp":{
			                  "status":"C"
			               },
			               "plug":{
			                  "status":"locked"
			               },
			               "contactor":{
			                  "status":"closed"
			               },
			               "pwm":{
			                  "status":"10.00"
			               }
			            }
			         },
			         "salia":{
			            "chargemode":"manual",
			            "thermal":"52893",
			            "mem":"392276",
			            "uptime":" 1:04",
			            "load":"0.37",
			            "chargedata":"3813|3192|3.46|",
			            "authmode":"free",
			            "firmwarestate":"idle",
			            "firmwareprogress":"0",
			            "heartbeat":"off",
			            "pausecharging":"0"
			         },
			         "session":{
			            "authorization_status":""
			         },
			         "contactor":{
			            "state":{
			               "hlc_target":"0",
			               "actual":"1",
			               "target":"1"
			            },
			            "error":"0"
			         },
			         "metering":{
			            "meter":{
			               "serialnumber":"21031835",
			               "type":"klefr",
			               "available":"1"
			            },
			            "eichrecht_protocol":"none",
			            "power":{
			               "active":{
			                  "ac":{
			                     "l1":{
			                        "actual":"10750"
			                     },
			                     "l2":{
			                        "actual":"10730"
			                     },
			                     "l3":{
			                        "actual":"10440"
			                     }
			                  }
			               },
			               "active_total":{
			                  "actual":"31920"
			               }
			            },
			            "current":{
			               "ac":{
			                  "l1":{
			                     "actual":"5000"
			                  },
			                  "l2":{
			                     "actual":"5000"
			                  },
			                  "l3":{
			                     "actual":"4770"
			                  }
			               }
			            },
			            "energy":{
			               "active_total":{
			                  "actual":"4658050"
			               },
			               "active_export":{
			                  "actual":"0"
			               },
			               "active_import":{
			                  "actual":"4658050"
			               }
			            }
			         },
			         "emergency_shutdown":"0",
			         "rcd":{
			            "feedback":{
			               "available":"1"
			            },
			            "state":{
			               "actual":"1"
			            },
			            "recloser":{
			               "available":"0"
			            }
			         },
			         "plug_lock":{
			            "state":{
			               "actual":"1",
			               "target":"1"
			            },
			            "error":"0"
			         },
			         "availability":{
			            "actual":"operative"
			         },
			         "cp":{
			            "pwm_state":{
			               "actual":"1"
			            },
			            "state":"C",
			            "duty_cycle":"10.00"
			         },
			         "rfid":{
			            "available":"0",
			            "authorizereq":""
			         },
			         "diode_present":"1",
			         "cable_current_limit":"-1",
			         "ready_for_slac":"0",
			         "ev_present":"1",
			         "ventilation":{
			            "state":{
			               "actual":"0"
			            },
			            "available":"0"
			         },
			         "charging":"1",
			         "grid_current_limit":"6"
			      }
			   }
			}
			""";
}
