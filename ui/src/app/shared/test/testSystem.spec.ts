import { Edge, EdgeConfig } from "../shared";
import { Role } from "../type/role";
import { AbstractSystem, Widget } from "./types.spec";

export namespace TestSystem {
  export class Ems1 extends AbstractSystem {
    public components: { [id: string]: EdgeConfig.Component; } = {
      meter0: {
        alias: "Netzzähler",
        factoryId: "Meter.Socomec.Threephase",
        id: "meter0",
        isEnabled: true,
        properties: {
          alias: "Netzzähler",
          enabled: true,
          invert: false,
          modbusUnitId: 5,
          type: "GRID"
        },
        channels: {}
      }
    };

    public factories: { [id: string]: EdgeConfig.Factory } = {
      ['Meter.Socomec.Threephase']:
      {
        name: "Meter Socomec Threephase",
        description: "Implements a threephase Socomec meter. Actual type is identified automatically.",
        natureIds: [
          "io.openems.edge.meter.api.ElectricityMeter",
          "io.openems.edge.bridge.modbus.api.ModbusComponent",
          "io.openems.edge.common.modbusslave.ModbusSlave",
          "io.openems.edge.common.component.OpenemsComponent",
          "io.openems.edge.meter.socomec.threephase.SocomecMeterThreephase",
          "io.openems.edge.meter.socomec.SocomecMeter"
        ],
        properties: [],
        id: "Meter.Socomec.Threephase",
        componentIds: [
          "meter0"
        ]
      }
    };

    public override key: string = "ems1";
    public widgets: Widget[];
    constructor() {
      // constructor(public widgets: Widget[]) {
      super();
      super.setEdge(new Edge(this.key, "", "", "2023.3.5", Role.ADMIN, true, new Date()));
      super.setConfig(this.components, this.factories);
    }
  }

  export class Ems4 extends AbstractSystem {
    public override key: string = "ems4";
    public components: { [id: string]: EdgeConfig.Component; } =
      {
        meter1: {
          alias: "Netzanschlusspunkt",
          factoryId: "GoodWe.Grid-Meter",
          properties: {
            alias: "Netzanschlusspunkt",
            enabled: "true",
            modbusUnitId: "247",
            type: "GRID"
          },
          id: "meter1",
          isEnabled: true,
          channels: {}
        }
      };
    public factories: { [id: string]: EdgeConfig.Factory } = {
      ['GoodWe.Grid-Meter']:
      {
        name: "GoodWe Grid-Meter",
        description: "GoodWe Smart Meter.",
        natureIds: [
          "io.openems.edge.goodwe.gridmeter.GoodWeGridMeter",
          "io.openems.edge.meter.api.ElectricityMeter",
          "io.openems.edge.bridge.modbus.api.ModbusComponent",
          "io.openems.edge.common.modbusslave.ModbusSlave",
          "io.openems.edge.common.component.OpenemsComponent",
          "io.openems.edge.timedata.api.TimedataProvider"
        ],
        properties: [],
        id: "GoodWe.Grid-Meter",
        componentIds: [
          "meter1"
        ]
      }
    };

    constructor(public widgets: Widget[]) {
      super();
      super.setEdge(new Edge(this.key, "", "", "2023.3.5", Role.ADMIN, true, new Date()));
      super.setConfig(this.components, this.factories);
    }
  }

  export class Ems10004 extends AbstractSystem {
    public override key: string = "ems10004";
    public components: { [id: string]: EdgeConfig.Component; } =
      {
        meter0: {
          alias: "Netzzähler",
          factoryId: "GoodWe.Grid-Meter",
          properties: {
            alias: "Netzzähler",
            enabled: true,
            modbusUnitId: 247
          },
          id: "meter0",
          isEnabled: true,
          channels: {}
        }
      };
    public factories: { [id: string]: EdgeConfig.Factory } = {
      ['GoodWe.Grid-Meter']:
      {
        name: "GoodWe Grid-Meter",
        description: "GoodWe Smart Meter.",
        natureIds: [
          "io.openems.edge.goodwe.gridmeter.GoodWeGridMeter",
          "io.openems.edge.meter.api.ElectricityMeter",
          "io.openems.edge.bridge.modbus.api.ModbusComponent",
          "io.openems.edge.common.modbusslave.ModbusSlave",
          "io.openems.edge.common.component.OpenemsComponent",
          "io.openems.edge.timedata.api.TimedataProvider"
        ],
        properties: [],
        id: "GoodWe.Grid-Meter",
        componentIds: [
          "meter0"
        ]
      }
    };

    constructor(public widgets: Widget[]) {
      super();
      super.setEdge(new Edge(this.key, "", "", "2023.3.5", Role.ADMIN, true, new Date()));
      super.setConfig(this.components, this.factories);
    }
  }

  export class Ems12786 extends AbstractSystem {
    public override key: string = "ems12786";
    public components: { [id: string]: EdgeConfig.Component; } =
      {
        meter0: {
          alias: "Netzzähler",
          factoryId: "GoodWe.Grid-Meter",
          properties: {
            alias: "Netzzähler",
            enabled: true,
            modbusUnitId: 247
          },
          id: "meter0",
          isEnabled: true,
          channels: {}
        }
      };
    public factories: { [id: string]: EdgeConfig.Factory } = {
      ['GoodWe.Grid-Meter']:
      {
        name: "GoodWe Grid-Meter",
        description: "GoodWe Smart Meter.",
        natureIds: [
          "io.openems.edge.goodwe.gridmeter.GoodWeGridMeter",
          "io.openems.edge.meter.api.ElectricityMeter",
          "io.openems.edge.bridge.modbus.api.ModbusComponent",
          "io.openems.edge.common.modbusslave.ModbusSlave",
          "io.openems.edge.common.component.OpenemsComponent",
          "io.openems.edge.timedata.api.TimedataProvider"
        ],
        properties: [],
        id: "GoodWe.Grid-Meter",
        componentIds: [
          "meter0"
        ]
      }
    };

    constructor(public widgets: Widget[]) {
      super();
      super.setEdge(new Edge(this.key, "", "", "2023.3.5", Role.ADMIN, true, new Date()));
      super.setConfig(this.components, this.factories);
    }
  }

  export class Ems30012 extends AbstractSystem {
    public override key: string = "ems30012";
    public components: { [id: string]: EdgeConfig.Component; } =
      {
        meter0: {
          alias: "meter0",
          factoryId: "Meter.Socomec.Threephase",
          properties: {
            alias: "",
            enabled: "true",
            invert: "false",
            modbusUnitId: 5,
            type: "GRID"
          },
          id: "meter0",
          isEnabled: true,
          channels: {}
        }
      };
    public factories: { [id: string]: EdgeConfig.Factory } = {
      ['Meter.Socomec.Threephase']:
      {
        name: "Meter Socomec Threephase",
        description: "Implements a threephase Socomec meter. Actual type is identified automatically.",
        natureIds: [
          "io.openems.edge.meter.api.ElectricityMeter",
          "io.openems.edge.bridge.modbus.api.ModbusComponent",
          "io.openems.edge.common.modbusslave.ModbusSlave",
          "io.openems.edge.common.component.OpenemsComponent",
          "io.openems.edge.meter.socomec.threephase.SocomecMeterThreephase",
          "io.openems.edge.meter.socomec.SocomecMeter"
        ],
        properties: [],
        id: "Meter.Socomec.Threephase",
        componentIds: [
          "meter0"
        ]
      }
    };

    constructor(public widgets: Widget[]) {
      super();
      super.setEdge(new Edge(this.key, "", "", "2023.3.5", Role.ADMIN, true, new Date()));
      super.setConfig(this.components, this.factories);
    }
  }

  export class Ems30034 extends AbstractSystem {
    public override key: string = "ems30034";
    public components: { [id: string]: EdgeConfig.Component; } =
      {
        meter0: {
          alias: "meter0",
          factoryId: "Meter.Socomec.Threephase",
          properties: {
            alias: "",
            enabled: "true",
            invert: "false",
            modbusUnitId: 5,
            type: "GRID"
          },
          id: "meter0",
          isEnabled: true,
          channels: {}
        }
      };
    public factories: { [id: string]: EdgeConfig.Factory } = {
      ['Meter.Socomec.Threephase']:
      {
        name: "Meter Socomec Threephase",
        description: "Implements a threephase Socomec meter. Actual type is identified automatically.",
        natureIds: [
          "io.openems.edge.meter.api.ElectricityMeter",
          "io.openems.edge.bridge.modbus.api.ModbusComponent",
          "io.openems.edge.common.modbusslave.ModbusSlave",
          "io.openems.edge.common.component.OpenemsComponent",
          "io.openems.edge.meter.socomec.threephase.SocomecMeterThreephase",
          "io.openems.edge.meter.socomec.SocomecMeter"
        ],
        properties: [],
        id: "Meter.Socomec.Threephase",
        componentIds: [
          "meter0"
        ]
      }
    };

    constructor(public widgets: Widget[]) {
      super();
      super.setEdge(new Edge(this.key, "", "", "2023.3.5", Role.ADMIN, true, new Date()));
      super.setConfig(this.components, this.factories);
    }
  }

  export class Ems30048 extends AbstractSystem {
    public override key: string = "ems30048";
    public components: { [id: string]: EdgeConfig.Component; } =
      {
        meter0: {
          alias: "meter0",
          factoryId: "Meter.Socomec.Threephase",
          properties: {
            alias: "",
            enabled: "true",
            invert: "false",
            modbusUnitId: 5,
            type: "GRID"
          },
          id: "meter0",
          isEnabled: true,
          channels: {}
        }
      };
    public factories: { [id: string]: EdgeConfig.Factory } = {
      ['Meter.Socomec.Threephase']:
      {
        name: "Meter Socomec Threephase",
        description: "Implements a threephase Socomec meter. Actual type is identified automatically.",
        natureIds: [
          "io.openems.edge.meter.api.ElectricityMeter",
          "io.openems.edge.bridge.modbus.api.ModbusComponent",
          "io.openems.edge.common.modbusslave.ModbusSlave",
          "io.openems.edge.common.component.OpenemsComponent",
          "io.openems.edge.meter.socomec.threephase.SocomecMeterThreephase",
          "io.openems.edge.meter.socomec.SocomecMeter"
        ],
        properties: [],
        id: "Meter.Socomec.Threephase",
        componentIds: [
          "meter0"
        ]
      }
    };

    constructor(public widgets: Widget[]) {
      super();
      super.setEdge(new Edge(this.key, "", "", "2023.3.5", Role.ADMIN, true, new Date()));
      super.setConfig(this.components, this.factories);
    }
  }

  export class Ems30093 extends AbstractSystem {
    public widgets: Widget[];
    public override key: string = "ems30093";
    public components: { [id: string]: EdgeConfig.Component; } =
      {
        meter10: {
          alias: "meter10",
          factoryId: "Meter.Socomec.Threephase",
          id: "meter10",
          isEnabled: true,
          properties: {
            alias: "",
            enabled: true,
            invert: false,
            modbusUnitId: "7",
            type: "GRID"
          },
          channels: {}
        },
        meter11: {
          alias: "meter11",
          factoryId: "Meter.Socomec.Threephase",
          id: "meter11",
          isEnabled: true,
          properties: {
            alias: "",
            enabled: true,
            invert: false,
            modbusUnitId: "8",
            type: "GRID"
          },
          channels: {}
        },

      };
    public factories: { [id: string]: EdgeConfig.Factory } = {
      ['Meter.Socomec.Threephase']:
      {
        name: "Meter Socomec Threephase",
        description: "Implements a threephase Socomec meter. Actual type is identified automatically.",
        natureIds: [
          "io.openems.edge.meter.api.ElectricityMeter",
          "io.openems.edge.bridge.modbus.api.ModbusComponent",
          "io.openems.edge.common.modbusslave.ModbusSlave",
          "io.openems.edge.common.component.OpenemsComponent",
          "io.openems.edge.meter.socomec.threephase.SocomecMeterThreephase",
          "io.openems.edge.meter.socomec.SocomecMeter"
        ],
        properties: [],
        id: "Meter.Socomec.Threephase",
        componentIds: [
          "meter10",
          "meter11"
        ]
      }
    };

    constructor() {
      super();
      super.setEdge(new Edge(this.key, "", "", "2023.3.5", Role.ADMIN, true, new Date()));
      super.setConfig(this.components, this.factories);
    }
  }
}