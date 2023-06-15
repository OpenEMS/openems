import { GridMode } from "src/app/shared/shared";
import { TextIndentation } from "../../../../../shared/genericComponents/modal/modal-line/modal-line";
import { OeFormlyViewTester } from "../../../../../shared/genericComponents/shared/tester";

export namespace Constants {

  export const defaultContext: OeFormlyViewTester.Context = {
    "_sum/GridMode": GridMode.ON_GRID,
    "meter0/ActivePower": -1000,
    "meter0/VoltageL1": 230000,
    "meter0/CurrentL1": 2170,
    "meter0/ActivePowerL1": -500,
    "meter0/ActivePowerL2": 1500
  };

  export const EMS1_ADMIN_AND_INSTALLER_SINGLE_METER: OeFormlyViewTester.ViewContext = {
    context: defaultContext,
    view: {
      title: "Netz",
      lines: [
        {
          type: "line",
          name: "Bezug",
          value: "0 W"
        }, {
          type: "line",
          name: "Einspeisung",
          value: "1.000 W"
        }, {
          type: "line-with-children",
          name: "Phase L1 Einspeisung",
          indentation: TextIndentation.SINGLE,
          children: [{
            type: "line-item",
            value: "230 V"
          }, {
            type: "line-item",
            value: "2,2 A"
          }, {
            type: "line-item",
            value: "500 W"
          }]
        }, {
          type: "line-with-children",
          name: "Phase L2 Bezug",
          indentation: TextIndentation.SINGLE,
          children: [{
            type: "line-item",
            value: "-"
          }, {
            type: "line-item",
            value: "-"
          }, {
            type: "line-item",
            value: "1.500 W"
          }]
        }, {
          type: "line-with-children",
          name: "Phase L3",
          indentation: TextIndentation.SINGLE,
          children: [{
            type: "line-item",
            value: "-"
          }, {
            type: "line-item",
            value: "-"
          }, {
            type: "line-item",
            value: "0 W"
          }]
        }, {
          type: "line-horizontal"
        }, {
          type: "line-info",
          name: "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
        }
      ]
    }
  }


  export const EMS30093_ADMIN_AND_INSTALLER_TWO_METERS: OeFormlyViewTester.ViewContext = {
    context: defaultContext,
    view: {
      title: "Netz",
      lines: [
        {
          type: "line",
          name: "Bezug",
          value: "0 W"
        },
        {
          type: "line",
          name: "Einspeisung",
          value: "0 W"
        },
        {
          type: "line-horizontal"
        },
        {
          type: "line",
          name: "meter10",
          value: "0 W"
        },
        {
          type: "line-with-children",
          name: "Phase L1",
          indentation: TextIndentation.SINGLE,
          children: [
            {
              type: "line-item",
              value: "-"
            },
            {
              type: "line-item",
              value: "-"
            },
            {
              type: "line-item",
              value: "0 W"
            }
          ]
        },
        {
          type: "line-with-children",
          name: "Phase L2",
          indentation: TextIndentation.SINGLE,
          children: [
            {
              type: "line-item",
              value: "-"
            },
            {
              type: "line-item",
              value: "-"
            },
            {
              type: "line-item",
              value: "0 W"
            }
          ]
        },
        {
          type: "line-with-children",
          name: "Phase L3",
          indentation: TextIndentation.SINGLE,
          children: [
            {
              type: "line-item",
              value: "-"
            },
            {
              type: "line-item",
              value: "-"
            },
            {
              type: "line-item",
              value: "0 W"
            }
          ]
        },
        {
          type: "line-horizontal"
        },
        {
          type: "line",
          name: "meter11",
          value: "0 W"
        },
        {
          type: "line-with-children",
          name: "Phase L1",
          indentation: TextIndentation.SINGLE,
          children: [
            {
              type: "line-item",
              value: "-"
            },
            {
              type: "line-item",
              value: "-"
            },
            {
              type: "line-item",
              value: "0 W"
            }
          ]
        },
        {
          type: "line-with-children",
          name: "Phase L2",
          indentation: TextIndentation.SINGLE,
          children: [
            {
              type: "line-item",
              value: "-"
            },
            {
              type: "line-item",
              value: "-"
            },
            {
              type: "line-item",
              value: "0 W"
            }
          ]
        },
        {
          type: "line-with-children",
          name: "Phase L3",
          indentation: TextIndentation.SINGLE,
          children: [
            {
              type: "line-item",
              value: "-"
            },
            {
              type: "line-item",
              value: "-"
            },
            {
              type: "line-item",
              value: "0 W"
            }
          ]
        },
        {
          type: "line-horizontal"
        },
        {
          type: "line-info",
          name: "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
        }
      ]
    }
  }

  export const EMS1_OWNER_AND_GUEST_SINGLE_METER: OeFormlyViewTester.ViewContext = {
    context: defaultContext,
    view: {
      title: "Netz",
      lines: [
        {
          type: "line",
          name: "Bezug",
          value: "0 W"
        },
        {
          type: "line",
          name: "Einspeisung",
          value: "1.000 W"
        },
        {
          type: "line-with-children",
          name: "Phase L1 Einspeisung",
          indentation: TextIndentation.SINGLE,
          children: [
            {
              type: "line-item",
              value: "500 W"
            }
          ]
        },
        {
          type: "line-with-children",
          name: "Phase L2 Bezug",
          indentation: TextIndentation.SINGLE,
          children: [
            {
              type: "line-item",
              value: "1.500 W"
            }
          ]
        },
        {
          type: "line-with-children",
          name: "Phase L3",
          indentation: TextIndentation.SINGLE,
          children: [
            {
              type: "line-item",
              value: "0 W"
            }
          ]
        },
        {
          type: "line-horizontal"
        },
        {
          type: "line-info",
          name: "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
        }
      ]
    }
  }
  export const EMS30093_OWNER_AND_GUEST_TWO_METERS: OeFormlyViewTester.ViewContext = {
    context: defaultContext,
    view: {
      title: "Netz",
      lines: [
        {
          type: "line",
          name: "Bezug",
          value: "0 W"
        },
        {
          type: "line",
          name: "Einspeisung",
          value: "0 W"
        },
        {
          type: "line-horizontal"
        },
        {
          type: "line",
          name: "meter10",
          value: "0 W"
        },
        {
          type: "line-with-children",
          name: "Phase L1",
          indentation: TextIndentation.SINGLE,
          children: [
            {
              type: "line-item",
              value: "0 W"
            }
          ]
        },
        {
          type: "line-with-children",
          name: "Phase L2",
          indentation: TextIndentation.SINGLE,
          children: [
            {
              type: "line-item",
              value: "0 W"
            }
          ]
        },
        {
          type: "line-with-children",
          name: "Phase L3",
          indentation: TextIndentation.SINGLE,
          children: [
            {
              type: "line-item",
              value: "0 W"
            }
          ]
        },
        {
          type: "line-horizontal"
        },
        {
          type: "line",
          name: "meter11",
          value: "0 W"
        },
        {
          type: "line-with-children",
          name: "Phase L1",
          indentation: TextIndentation.SINGLE,
          children: [
            {
              type: "line-item",
              value: "0 W"
            }
          ]
        },
        {
          type: "line-with-children",
          name: "Phase L2",
          indentation: TextIndentation.SINGLE,
          children: [
            {
              type: "line-item",
              value: "0 W"
            }
          ]
        },
        {
          type: "line-with-children",
          name: "Phase L3",
          indentation: TextIndentation.SINGLE,
          children: [
            {
              type: "line-item",
              value: "0 W"
            }
          ]
        },
        {
          type: "line-horizontal"
        },
        {
          type: "line-info",
          name: "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
        }
      ]
    }
  }
  export const EMS1_OFF_GRID: OeFormlyViewTester.ViewContext = {
    context: Object.assign({}, defaultContext, { '_sum/GridMode': GridMode.OFF_GRID }),
    view: {
      title: "Netz",
      lines: [
        {
          type: "line",
          name: "Keine Netzverbindung!",
          value: "2"
        },
        {
          type: "line",
          name: "Bezug",
          value: "0 W"
        },
        {
          type: "line",
          name: "Einspeisung",
          value: "1.000 W"
        },
        {
          type: "line-with-children",
          name: "Phase L1 Einspeisung",
          indentation: TextIndentation.SINGLE,
          children: [
            {
              type: "line-item",
              value: "230 V"
            },
            {
              type: "line-item",
              value: "2,2 A"
            },
            {
              type: "line-item",
              value: "500 W"
            }
          ]
        },
        {
          type: "line-with-children",
          name: "Phase L2 Bezug",
          indentation: TextIndentation.SINGLE,
          children: [
            {
              type: "line-item",
              value: "-"
            },
            {
              type: "line-item",
              value: "-"
            },
            {
              type: "line-item",
              value: "1.500 W"
            }
          ]
        },
        {
          type: "line-with-children",
          name: "Phase L3",
          indentation: TextIndentation.SINGLE,
          children: [
            {
              type: "line-item",
              value: "-"
            },
            {
              type: "line-item",
              value: "-"
            },
            {
              type: "line-item",
              value: "0 W"
            }
          ]
        },
        {
          type: "line-horizontal"
        },
        {
          type: "line-info",
          name: "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
        }
      ]
    }
  }
}