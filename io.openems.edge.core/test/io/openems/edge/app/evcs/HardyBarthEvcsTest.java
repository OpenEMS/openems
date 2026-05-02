package io.openems.edge.app.evcs;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.MyConfig;

public class HardyBarthEvcsTest {

	@Test
	public void testPropertyToComponentMapping() throws Exception {
		final var testBundle = new AppManagerTestBundle(JsonUtils.parseToJsonObject("""
				{
				    "ctrlEvseCluster0": {
				        "alias": "Ladepunkt Cluster",
				        "factoryId": "Evse.Controller.Cluster",
				        "properties": {
				            "_lastChangeAt": "2026-02-17T18:04:27",
				            "_lastChangeBy": "UNDEFINED",
				            "alias": "Ladepunkt Cluster",
				            "ctrl.ids": [
				                "ctrlEvseSingle0",
				                "ctrlEvseSingle1"
				            ],
				            "distributionStrategy": "EQUAL_POWER",
				            "enabled": "true",
				            "logVerbosity": "NONE"
				        }
				    },
				    "ctrlEvseSingle0": {
				        "alias": "eCharge Hardy Barth Ladestation - Rechts",
				        "factoryId": "Evse.Controller.Single",
				        "properties": {
				            "_lastChangeAt": "2026-02-20T12:16:08",
				            "_lastChangeBy": "UNDEFINED",
				            "alias": "eCharge Hardy Barth Ladestation - Rechts",
				            "chargePoint.id": "evcs1",
				            "electricVehicle.id": "evseElectricVehicle0",
				            "enabled": "true",
				            "jsCalendar": [
				            ],
				            "logVerbosity": "DEBUG_LOG",
				            "manualEnergySessionLimit": "0",
				            "mode": "FORCE",
				            "oneShot": "",
				            "phaseSwitching": "FORCE_THREE_PHASE"
				        }
				    },
				    "ctrlEvseSingle1": {
				        "alias": "eCharge Hardy Barth Ladestation - Links",
				        "factoryId": "Evse.Controller.Single",
				        "properties": {
				            "_lastChangeAt": "2026-02-21T19:12:19",
				            "_lastChangeBy": "UNDEFINED",
				            "alias": "eCharge Hardy Barth Ladestation - Links",
				            "chargePoint.id": "evcs0",
				            "electricVehicle.id": "evseElectricVehicle1",
				            "enabled": "true",
				            "jsCalendar": "[]",
				            "logVerbosity": "DEBUG_LOG",
				            "manualEnergySessionLimit": "0",
				            "mode": "FORCE",
				            "oneShot": "",
				            "phaseSwitching": "FORCE_THREE_PHASE"
				        }
				    },
				    "evseElectricVehicle0": {
				        "alias": "Generisches Fahrzeug",
				        "factoryId": "Evse.ElectricVehicle.Generic",
				        "properties": {
				            "_lastChangeAt": "2026-02-17T18:04:16",
				            "_lastChangeBy": "UNDEFINED",
				            "alias": "Generisches Fahrzeug",
				            "capacity": 50000,
				            "canInterrupt": "true",
				            "enabled": "true",
				            "maxPowerSinglePhase": "7360",
				            "maxPowerThreePhase": "11040",
				            "minPowerSinglePhase": "1380",
				            "minPowerThreePhase": "4140"
				        }
				    },
				    "evseElectricVehicle1": {
				        "alias": "Generisches Fahrzeug",
				        "factoryId": "Evse.ElectricVehicle.Generic",
				        "properties": {
				            "_lastChangeAt": "2026-02-17T18:04:18",
				            "_lastChangeBy": "UNDEFINED",
				            "alias": "Generisches Fahrzeug",
				            "canInterrupt": "true",
				            "enabled": "true",
				            "capacity": 50000,
				            "maxPowerSinglePhase": "7360",
				            "maxPowerThreePhase": "11040",
				            "minPowerSinglePhase": "1380",
				            "minPowerThreePhase": "4140"
				        }
				    },
				    "scheduler0": {
				        "alias": "scheduler0",
				        "factoryId": "Scheduler.AllAlphabetically",
				        "properties": {
				            "_lastChangeAt": "2026-02-21T19:07:42",
				            "_lastChangeBy": "UNDEFINED",
				            "alias": "",
				            "controllers.ids": [
				            ],
				            "enabled": true
				        }
				    },
				    "evcs0": {
				        "alias": "eCharge Hardy Barth Ladestation - Links",
				        "factoryId": "Evse.ChargePoint.HardyBarth",
				        "properties": {
				            "_lastChangeAt": "2026-02-21T19:12:18",
				            "_lastChangeBy": "UNDEFINED",
				            "alias": "eCharge Hardy Barth Ladestation - Links",
				            "enabled": "true",
				            "ip": "192.168.7.77",
				            "logVerbosity": "NONE",
				            "phaseRotation": "L1_L2_L3",
				            "readOnly": false
				        }
				    },
				    "evcs1": {
				        "alias": "eCharge Hardy Barth Ladestation - Rechts",
				        "factoryId": "Evse.ChargePoint.HardyBarth",
				        "properties": {
				            "_lastChangeAt": "2026-02-17T18:03:59",
				            "_lastChangeBy": "UNDEFINED",
				            "alias": "eCharge Hardy Barth Ladestation - Rechts",
				            "enabled": "true",
				            "ip": "192.168.7.78",
				            "logVerbosity": "NONE",
				            "phaseRotation": "L1_L2_L3",
				            "readOnly": false
				        }
				    }
				}
				""".stripIndent()), MyConfig.create().setApps("""
				[
				    {
				        "appId": "App.Evcs.HardyBarth",
				        "alias": "eCharge Hardy Barth Ladestation - Rechts",
				        "instanceId": "77e0ac49-e12e-46e2-83cb-8ac25e9c6579",
				        "properties": {
				            "EVCS_ID": "evcs1",
				            "IP": "192.168.7.78",
				            "CTRL_SINGLE_ID": "ctrlEvseSingle0",
				            "ELECTRIC_VEHICLE_ID": "07a74c08-806f-457c-8efe-bc32b8db672b",
				            "EVCS_ID_CP_2": "evcs0",
				            "CTRL_SINGLE_ID_CP_2": "ctrlEvseSingle1",
				            "IP_CP_2": "192.168.7.77",
				            "ELECTRIC_VEHICLE_ID_CP_2": "174a516a-96bd-461f-a836-be559354e46e",
				            "ALIAS_CP_2": "eCharge Hardy Barth Ladestation - Links",
				            "NUMBER_OF_CHARGING_STATIONS": 2,
				            "ARCHITECTURE_TYPE": "EVSE",
				            "PHASE_ROTATION": "L1_L2_L3",
				            "READ_ONLY": false
				        },
				        "dependencies": [
				            {
				                "key": "VEHICLE",
				                "instanceId": "07a74c08-806f-457c-8efe-bc32b8db672b"
				            },
				            {
				                "key": "VEHICLE_CP_2",
				                "instanceId": "174a516a-96bd-461f-a836-be559354e46e"
				            },
				            {
				                "key": "CLUSTER",
				                "instanceId": "0bc94f94-efdd-4837-b6a2-bd40595e2f3e"
				            }
				        ]
				    },
				    {
				        "appId": "App.Evse.ElectricVehicle.Generic",
				        "alias": "Generisches Fahrzeug",
				        "instanceId": "07a74c08-806f-457c-8efe-bc32b8db672b",
				        "properties": {
				            "VEHICLE_ID": "evseElectricVehicle0",
				            "MIN_POWER_SINGLE_PHASE": 1380,
				            "MAX_POWER_SINGLE_PHASE": 7360,
				            "MIN_POWER_THREE_PHASE": 4140,
				            "MAX_POWER_THREE_PHASE": 11040,
				            "CAPACITY": 50000,
				            "CAN_INTERRUPT": true
				        }
				    },
				    {
				        "appId": "App.Evse.ElectricVehicle.Generic",
				        "alias": "Generisches Fahrzeug",
				        "instanceId": "174a516a-96bd-461f-a836-be559354e46e",
				        "properties": {
				            "VEHICLE_ID": "evseElectricVehicle1",
				            "MIN_POWER_SINGLE_PHASE": 1380,
				            "MAX_POWER_SINGLE_PHASE": 7360,
				            "MIN_POWER_THREE_PHASE": 4140,
				            "MAX_POWER_THREE_PHASE": 11040,
				            "CAPACITY": 50000,
				            "CAN_INTERRUPT": true
				        }
				    },
				    {
				        "appId": "App.Evse.Controller.Cluster",
				        "alias": "Ladepunkt Cluster",
				        "instanceId": "0bc94f94-efdd-4837-b6a2-bd40595e2f3e",
				        "properties": {
				            "EVSE_CLUSTER_ID": "ctrlEvseCluster0",
				            "EVSE_IDS": [
				                "ctrlEvseSingle0",
				                "ctrlEvseSingle1"
				            ]
				        }
				    }
				]
				""".stripIndent()).build(), t -> {
			return ImmutableList.of(//
					Apps.hardyBarthEvcs(t), //
					Apps.genericVehicle(t), //
					Apps.clusterEvse(t) //
			);
		});

		testBundle.addComponentAggregateTask();
		testBundle.addEvseAggregateTask();

		testBundle.assertNoValidationErrors();
	}

}