import { TestBed } from "@angular/core/testing";
import { FormlyFieldConfig, FORMLY_CONFIG } from "@ngx-formly/core";
import { TranslateLoader, TranslateModule, TranslateService } from "@ngx-translate/core";
import { EdgeConfig } from "src/app/shared/shared";
import { registerTranslateExtension } from "src/app/shared/translate.extension";
import { MyTranslateLoader } from "src/app/shared/type/language";
import { Role } from "src/app/shared/type/role";
import { ModalComponent } from "../live/common/grid/modal/modal";

export const fems1ToBeDeleted: EdgeConfig = {
  "components": {
    "meter0": {
      "alias": "Netzzähler",
      "factoryId": "Meter.Socomec.Threephase",
      "id": "meter0",
      "isEnabled": "true",
      "properties": {
        "alias": "Netzzähler",
        "enabled": "true",
        "invert": "false",
        "modbus.id": "modbus10",
        "modbusUnitId": 5,
        "type": "GRID"
      },
      "channels": {}
    },
  }
} as unknown as EdgeConfig

export namespace fems1 {

  export const key = "fems1";

  export namespace Grid {

    export class Modal {
      public static admin: FormlyFieldConfig[] = [{
        "key": "fems1",
        "type": "input",
        "templateOptions": {
          "attributes": {
            "title": "Netz"
          },
          "required": true,
          "options": [
            {
              "lines": [
                {
                  "type": "line",
                  "name": "Keine Netzverbindung!",
                  "channel": "_sum/GridMode",
                  "channelCondition": 2
                },
                {
                  "type": "line",
                  "name": "Bezug",
                  "channel": "_sum/GridActivePower"
                },
                {
                  "type": "line",
                  "name": "Einspeisung",
                  "channel": "_sum/GridActivePower"
                },
                {
                  "type": "line",
                  "name": "Phase L1 ",
                  "indentation": "5%",
                  "channel": "_sum/GridActivePowerL1",
                  "children": [
                    {
                      "type": "line-item",
                      "channel": "meter0/VoltageL1",
                      "indentation": "5%"
                    },
                    {
                      "type": "line-item",
                      "channel": "meter0/CurrentL1",
                      "indentation": "5%"
                    },
                    {
                      "type": "line-item",
                      "channel": "meter0/ActivePowerL1",
                      "indentation": "5%"
                    }
                  ]
                },
                {
                  "type": "line",
                  "name": "Phase L2 ",
                  "indentation": "5%",
                  "channel": "_sum/GridActivePowerL2",
                  "children": [
                    {
                      "type": "line-item",
                      "channel": "meter0/VoltageL2",
                      "indentation": "5%"
                    },
                    {
                      "type": "line-item",
                      "channel": "meter0/CurrentL2",
                      "indentation": "5%"
                    },
                    {
                      "type": "line-item",
                      "channel": "meter0/ActivePowerL2",
                      "indentation": "5%"
                    }
                  ]
                },
                {
                  "type": "line",
                  "name": "Phase L3 ",
                  "indentation": "5%",
                  "channel": "_sum/GridActivePowerL3",
                  "children": [
                    {
                      "type": "line-item",
                      "channel": "meter0/VoltageL3",
                      "indentation": "5%"
                    },
                    {
                      "type": "line-item",
                      "channel": "meter0/CurrentL3",
                      "indentation": "5%"
                    },
                    {
                      "type": "line-item",
                      "channel": "meter0/ActivePowerL3",
                      "indentation": "5%"
                    }
                  ]
                },
                {
                  "type": "line-info",
                  "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
                }
              ]
            }
          ]
        },
        "wrappers": [
          "formly-field-modal"
        ]
      }]
    }
  }



}

export const fems4 = {
  "fems": "fems4",
  "config": {
    "ess0": {
      "alias": "ess0",
      "factoryId": "Ess.Generic.ManagedSymmetric",
      "properties": {
        "_lastChangeAt": "2021-10-26T10:13:32",
        "_lastChangeBy": "ludwig.asen: Ludwig Asen",
        "alias": "",
        "battery.id": "battery0",
        "batteryInverter.id": "batteryInverter0",
        "enabled": true,
        "startStop": "START"
      }
    }
  }
}
export const fems10004 = {
  "fems": "fems10004",
  "config": {
    "ess0": {
      "alias": "Speichersystem",
      "factoryId": "Ess.Generic.ManagedSymmetric",
      "properties": {
        "_lastChangeAt": "2022-08-30T15:29:55",
        "_lastChangeBy": "lukas.rieger: Lukas Rieger",
        "alias": "Speichersystem",
        "battery.id": "battery0",
        "batteryInverter.id": "batteryInverter0",
        "enabled": true,
        "startStop": "START"
      }
    }
  }
}
export const fems12786 = {
  "fems": "fems12786",
  "config": {
    "ess0": {
      "alias": "Speichersystem",
      "factoryId": "Ess.Generic.ManagedSymmetric",
      "properties": {
        "_lastChangeAt": "2022-07-20T21:17:30",
        "_lastChangeBy": "ludwig.asen: Ludwig Asen",
        "alias": "Speichersystem",
        "battery.id": "battery0",
        "batteryInverter.id": "batteryInverter0",
        "enabled": true,
        "startStop": "START"
      }
    }
  }
}
export const fems30012 = {
  "fems": "fems30012",
  "config": {
    "ess0": {
      "alias": "ess0",
      "factoryId": "Ess.Generic.ManagedSymmetric",
      "properties": {
        "_lastChangeAt": "2022-11-05T01:52:19",
        "_lastChangeBy": "nico.ketzer: Nico Ketzer",
        "alias": "",
        "battery.id": "battery0",
        "batteryInverter.id": "batteryInverter0",
        "enabled": true,
        "startStop": "START"
      }
    }
  }
}
export const fems30034 = {
  "fems": "fems30034",
  "config": {
    "ess0": {
      "alias": "ess0",
      "factoryId": "Ess.Generic.ManagedSymmetric",
      "properties": {
        "_lastChangeAt": "2022-07-29T09:54:45",
        "_lastChangeBy": "andrej.peter: Andrej Peter",
        "alias": "",
        "battery.id": "battery0",
        "batteryInverter.id": "batteryInverter0",
        "enabled": true,
        "startStop": "START"
      }
    }
  }
}
export const fems30048 = {
  "fems": "fems30048",
  "config": {
    "ess0": {
      "alias": "Speichersystem Cluster",
      "factoryId": "Ess.Cluster",
      "properties": {
        "_lastChangeAt": "2022-10-19T17:24:13",
        "_lastChangeBy": "andreas.wust: Andreas Wust",
        "alias": "Speichersystem Cluster",
        "enabled": true,
        "ess.ids": [
          "ess1",
          "ess2",
          "ess3"
        ],
        "startStop": "START"
      }
    },
    "ess1": {
      "alias": "Speichersystem 1",
      "factoryId": "Ess.Generic.ManagedSymmetric",
      "properties": {
        "_lastChangeAt": "2022-11-17T13:28:01",
        "_lastChangeBy": "andrej.peter: Andrej Peter",
        "alias": "Speichersystem 1",
        "battery.id": "battery1",
        "batteryInverter.id": "batteryInverter1",
        "enabled": true,
        "startStop": "START"
      }
    },
    "ess2": {
      "alias": "Speichersystem 2",
      "factoryId": "Ess.Generic.ManagedSymmetric",
      "properties": {
        "_lastChangeAt": "2022-10-25T14:44:42",
        "_lastChangeBy": "nico.ketzer: Nico Ketzer",
        "alias": "Speichersystem 2",
        "battery.id": "battery2",
        "batteryInverter.id": "batteryInverter2",
        "enabled": "true",
        "startStop": "START"
      }
    },
    "ess3": {
      "alias": "Speichersystem 3",
      "factoryId": "Ess.Generic.ManagedSymmetric",
      "properties": {
        "_lastChangeAt": "2022-11-17T11:02:23",
        "_lastChangeBy": "andrej.peter: Andrej Peter",
        "alias": "Speichersystem 3",
        "battery.id": "battery3",
        "batteryInverter.id": "batteryInverter3",
        "enabled": true,
        "startStop": "START"
      }
    }
  }
}


describe('ExampleSystemsTest', () => {

  let translate: TranslateService;

  beforeEach((() => {
    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot({ loader: { provide: TranslateLoader, useClass: MyTranslateLoader, }, defaultLanguage: 'de' }),

      ],
      providers: [TranslateService, { provide: FORMLY_CONFIG, multi: true, useFactory: registerTranslateExtension, deps: [TranslateService] },]
    }).compileComponents();
    translate = TestBed.inject(TranslateService);
  }));

  let testSystems = [fems1,
    //fems4, fems10004, fems12786, fems30012, fems30034, fems30048
  ];
  let roles = [Role.ADMIN,// Role.INSTALLER, Role.OWNER, Role.GUEST
  ]

  for (let key in testSystems) {

    for (let role of roles) {
      it(testSystems[key].key + ' StorageModal equals expected modal', () => {
        expect(JSON.stringify(ModalComponent.generateGridModal("fems1", fems1ToBeDeleted, role, translate))).toBe(JSON.stringify(fems1.Grid.Modal.admin))
      });
    }
  }
});