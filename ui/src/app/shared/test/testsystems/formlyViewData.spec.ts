import { OeFormlyView } from "../../genericComponents/shared/oe-formly-component";

export const ems1adminAndInstallerGridModal: OeFormlyView = {
  "title": "Netz",
  "lines": [
    {
      "type": "line",
      "name": "Keine Netzverbindung!",
      "channel": "_sum/GridMode"
    },
    {
      "type": "line",
      "name": "Bezug",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "name": "Einspeisung",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL1",
      "children": [
        {
          "type": "line-item",
          "channel": "meter0/VoltageL1"
        },
        {
          "type": "line-item",
          "channel": "meter0/CurrentL1"
        },
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL1"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL2",
      "children": [
        {
          "type": "line-item",
          "channel": "meter0/VoltageL2"
        },
        {
          "type": "line-item",
          "channel": "meter0/CurrentL2"
        },
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL2"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL3",
      "children": [
        {
          "type": "line-item",
          "channel": "meter0/VoltageL3"
        },
        {
          "type": "line-item",
          "channel": "meter0/CurrentL3"
        },
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL3"
        }
      ]
    },
    {
      "type": "line-horizontal"
    },
    {
      "type": "line-info",
      "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
    }
  ]
} as OeFormlyView;
export const ems1ownerAndGuestGridModal: OeFormlyView = {
  "title": "Netz",
  "lines": [
    {
      "type": "line",
      "name": "Keine Netzverbindung!",
      "channel": "_sum/GridMode"
    },
    {
      "type": "line",
      "name": "Bezug",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "name": "Einspeisung",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL1",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL1"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL2",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL2"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL3",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL3"
        }
      ]
    },
    {
      "type": "line-horizontal"
    },
    {
      "type": "line-info",
      "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
    }
  ]
} as OeFormlyView;
export const ems4ownerAndGuestGridModal: OeFormlyView = {
  "title": "Netz",
  "lines": [
    {
      "type": "line",
      "name": "Keine Netzverbindung!",
      "channel": "_sum/GridMode"
    },
    {
      "type": "line",
      "name": "Bezug",
      "channel": "meter1/ActivePower"
    },
    {
      "type": "line",
      "name": "Einspeisung",
      "channel": "meter1/ActivePower"
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter1/ActivePowerL1",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter1/ActivePowerL1"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter1/ActivePowerL2",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter1/ActivePowerL2"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter1/ActivePowerL3",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter1/ActivePowerL3"
        }
      ]
    },
    {
      "type": "line-horizontal"
    },
    {
      "type": "line-info",
      "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
    }
  ]
} as OeFormlyView;
export const ems4adminAndInstallerGridModal: OeFormlyView = {
  "title": "Netz",
  "lines": [
    {
      "type": "line",
      "name": "Keine Netzverbindung!",
      "channel": "_sum/GridMode"
    },
    {
      "type": "line",
      "name": "Bezug",
      "channel": "meter1/ActivePower"
    },
    {
      "type": "line",
      "name": "Einspeisung",
      "channel": "meter1/ActivePower"
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter1/ActivePowerL1",
      "children": [
        {
          "type": "line-item",
          "channel": "meter1/VoltageL1"
        },
        {
          "type": "line-item",
          "channel": "meter1/CurrentL1"
        },
        {
          "type": "line-item",
          "channel": "meter1/ActivePowerL1"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter1/ActivePowerL2",
      "children": [
        {
          "type": "line-item",
          "channel": "meter1/VoltageL2"
        },
        {
          "type": "line-item",
          "channel": "meter1/CurrentL2"
        },
        {
          "type": "line-item",
          "channel": "meter1/ActivePowerL2"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter1/ActivePowerL3",
      "children": [
        {
          "type": "line-item",
          "channel": "meter1/VoltageL3"
        },
        {
          "type": "line-item",
          "channel": "meter1/CurrentL3"
        },
        {
          "type": "line-item",
          "channel": "meter1/ActivePowerL3"
        }
      ]
    },
    {
      "type": "line-horizontal"
    },
    {
      "type": "line-info",
      "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
    }
  ]
} as OeFormlyView;
export const ems10004ownerAndGuestGridModal: OeFormlyView = {
  "title": "Netz",
  "lines": [
    {
      "type": "line",
      "name": "Keine Netzverbindung!",
      "channel": "_sum/GridMode"
    },
    {
      "type": "line",
      "name": "Bezug",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "name": "Einspeisung",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL1",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL1"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL2",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL2"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL3",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL3"
        }
      ]
    },
    {
      "type": "line-horizontal"
    },
    {
      "type": "line-info",
      "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
    }
  ]
} as OeFormlyView;
export const ems10004adminAndInstallerGridModal: OeFormlyView = {
  "title": "Netz",
  "lines": [
    {
      "type": "line",
      "name": "Keine Netzverbindung!",
      "channel": "_sum/GridMode"
    },
    {
      "type": "line",
      "name": "Bezug",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "name": "Einspeisung",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL1",
      "children": [
        {
          "type": "line-item",
          "channel": "meter0/VoltageL1"
        },
        {
          "type": "line-item",
          "channel": "meter0/CurrentL1"
        },
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL1"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL2",
      "children": [
        {
          "type": "line-item",
          "channel": "meter0/VoltageL2"
        },
        {
          "type": "line-item",
          "channel": "meter0/CurrentL2"
        },
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL2"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL3",
      "children": [
        {
          "type": "line-item",
          "channel": "meter0/VoltageL3"
        },
        {
          "type": "line-item",
          "channel": "meter0/CurrentL3"
        },
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL3"
        }
      ]
    },
    {
      "type": "line-horizontal"
    },
    {
      "type": "line-info",
      "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
    }
  ]
} as OeFormlyView;
export const ems12786ownerAndGuestGridModal: OeFormlyView = {
  "title": "Netz",
  "lines": [
    {
      "type": "line",
      "name": "Keine Netzverbindung!",
      "channel": "_sum/GridMode"
    },
    {
      "type": "line",
      "name": "Bezug",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "name": "Einspeisung",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL1",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL1"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL2",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL2"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL3",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL3"
        }
      ]
    },
    {
      "type": "line-horizontal"
    },
    {
      "type": "line-info",
      "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
    }
  ]
} as OeFormlyView;
export const ems12786adminAndInstallerGridModal: OeFormlyView = {
  "title": "Netz",
  "lines": [
    {
      "type": "line",
      "name": "Keine Netzverbindung!",
      "channel": "_sum/GridMode"
    },
    {
      "type": "line",
      "name": "Bezug",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "name": "Einspeisung",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL1",
      "children": [
        {
          "type": "line-item",
          "channel": "meter0/VoltageL1"
        },
        {
          "type": "line-item",
          "channel": "meter0/CurrentL1"
        },
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL1"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL2",
      "children": [
        {
          "type": "line-item",
          "channel": "meter0/VoltageL2"
        },
        {
          "type": "line-item",
          "channel": "meter0/CurrentL2"
        },
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL2"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL3",
      "children": [
        {
          "type": "line-item",
          "channel": "meter0/VoltageL3"
        },
        {
          "type": "line-item",
          "channel": "meter0/CurrentL3"
        },
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL3"
        }
      ]
    },
    {
      "type": "line-horizontal"
    },
    {
      "type": "line-info",
      "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
    }
  ]
} as OeFormlyView;
export const ems30012ownerAndGuestGridModal: OeFormlyView = {
  "title": "Netz",
  "lines": [
    {
      "type": "line",
      "name": "Keine Netzverbindung!",
      "channel": "_sum/GridMode"
    },
    {
      "type": "line",
      "name": "Bezug",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "name": "Einspeisung",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL1",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL1"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL2",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL2"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL3",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL3"
        }
      ]
    },
    {
      "type": "line-horizontal"
    },
    {
      "type": "line-info",
      "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
    }
  ]
} as OeFormlyView;
export const ems30012adminAndInstallerGridModal: OeFormlyView = {
  "title": "Netz",
  "lines": [
    {
      "type": "line",
      "name": "Keine Netzverbindung!",
      "channel": "_sum/GridMode"
    },
    {
      "type": "line",
      "name": "Bezug",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "name": "Einspeisung",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL1",
      "children": [
        {
          "type": "line-item",
          "channel": "meter0/VoltageL1"
        },
        {
          "type": "line-item",
          "channel": "meter0/CurrentL1"
        },
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL1"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL2",
      "children": [
        {
          "type": "line-item",
          "channel": "meter0/VoltageL2"
        },
        {
          "type": "line-item",
          "channel": "meter0/CurrentL2"
        },
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL2"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL3",
      "children": [
        {
          "type": "line-item",
          "channel": "meter0/VoltageL3"
        },
        {
          "type": "line-item",
          "channel": "meter0/CurrentL3"
        },
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL3"
        }
      ]
    },
    {
      "type": "line-horizontal"
    },
    {
      "type": "line-info",
      "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
    }
  ]
} as OeFormlyView;
export const ems30034ownerAndGuestGridModal: OeFormlyView = {
  "title": "Netz",
  "lines": [
    {
      "type": "line",
      "name": "Keine Netzverbindung!",
      "channel": "_sum/GridMode"
    },
    {
      "type": "line",
      "name": "Bezug",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "name": "Einspeisung",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL1",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL1"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL2",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL2"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL3",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL3"
        }
      ]
    },
    {
      "type": "line-horizontal"
    },
    {
      "type": "line-info",
      "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
    }
  ]
} as OeFormlyView;
export const ems30034adminAndInstallerGridModal: OeFormlyView = {
  "title": "Netz",
  "lines": [
    {
      "type": "line",
      "name": "Keine Netzverbindung!",
      "channel": "_sum/GridMode"
    },
    {
      "type": "line",
      "name": "Bezug",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "name": "Einspeisung",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL1",
      "children": [
        {
          "type": "line-item",
          "channel": "meter0/VoltageL1"
        },
        {
          "type": "line-item",
          "channel": "meter0/CurrentL1"
        },
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL1"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL2",
      "children": [
        {
          "type": "line-item",
          "channel": "meter0/VoltageL2"
        },
        {
          "type": "line-item",
          "channel": "meter0/CurrentL2"
        },
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL2"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL3",
      "children": [
        {
          "type": "line-item",
          "channel": "meter0/VoltageL3"
        },
        {
          "type": "line-item",
          "channel": "meter0/CurrentL3"
        },
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL3"
        }
      ]
    },
    {
      "type": "line-horizontal"
    },
    {
      "type": "line-info",
      "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
    }
  ]
} as OeFormlyView;
export const ems30048ownerAndGuestGridModal: OeFormlyView = {
  "title": "Netz",
  "lines": [
    {
      "type": "line",
      "name": "Keine Netzverbindung!",
      "channel": "_sum/GridMode"
    },
    {
      "type": "line",
      "name": "Bezug",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "name": "Einspeisung",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL1",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL1"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL2",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL2"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL3",
      "children": [
        false,
        false,
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL3"
        }
      ]
    },
    {
      "type": "line-horizontal"
    },
    {
      "type": "line-info",
      "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
    }
  ]
} as OeFormlyView;
export const ems30048adminAndInstallerGridModal: OeFormlyView = {
  "title": "Netz",
  "lines": [
    {
      "type": "line",
      "name": "Keine Netzverbindung!",
      "channel": "_sum/GridMode"
    },
    {
      "type": "line",
      "name": "Bezug",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "name": "Einspeisung",
      "channel": "meter0/ActivePower"
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL1",
      "children": [
        {
          "type": "line-item",
          "channel": "meter0/VoltageL1"
        },
        {
          "type": "line-item",
          "channel": "meter0/CurrentL1"
        },
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL1"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL2",
      "children": [
        {
          "type": "line-item",
          "channel": "meter0/VoltageL2"
        },
        {
          "type": "line-item",
          "channel": "meter0/CurrentL2"
        },
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL2"
        }
      ]
    },
    {
      "type": "line",
      "indentation": "5%",
      "channel": "meter0/ActivePowerL3",
      "children": [
        {
          "type": "line-item",
          "channel": "meter0/VoltageL3"
        },
        {
          "type": "line-item",
          "channel": "meter0/CurrentL3"
        },
        {
          "type": "line-item",
          "channel": "meter0/ActivePowerL3"
        }
      ]
    },
    {
      "type": "line-horizontal"
    },
    {
      "type": "line-info",
      "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
    }
  ]
} as OeFormlyView;