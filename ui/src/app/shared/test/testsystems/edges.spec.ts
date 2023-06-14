import { OeFormlyView } from "../../genericComponents/shared/oe-formly-component";
import { Edge, EdgeConfig } from "../../shared";
import { Role } from "../../type/role";
import * as viewData from "./formlyViewData.spec";
import { AbstractSystem, Grid, Modal, Widget } from "./types.spec";

export class ems1 extends AbstractSystem {
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

  constructor() {
    super();
    super.setEdge(new Edge(this.key, "", "", "2023.3.5", Role.ADMIN, true, new Date()));
    super.setConfig(this.components, this.factories);
  }

  public widgets: Widget[] = [new Grid(new Modal("GridModal", new Map()
    .set(Role.ADMIN, viewData.ems1adminAndInstallerGridModal)
    .set(Role.INSTALLER, viewData.ems1adminAndInstallerGridModal)
    .set(Role.OWNER, viewData.ems1ownerAndGuestGridModal)
    .set(Role.GUEST, viewData.ems1ownerAndGuestGridModal)
  ))];
}

export class ems4 extends AbstractSystem {
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
  public widgets: Widget[] = [
    new Grid(
      new Modal("GridModal", new Map()
        .set(Role.ADMIN, viewData.ems4adminAndInstallerGridModal)
        .set(Role.INSTALLER, viewData.ems4adminAndInstallerGridModal)
        .set(Role.OWNER, viewData.ems4ownerAndGuestGridModal)
        .set(Role.GUEST, viewData.ems4ownerAndGuestGridModal)
      ))];

  constructor() {
    super();
    super.setEdge(new Edge(this.key, "", "", "2023.3.5", Role.ADMIN, true, new Date()));
    super.setConfig(this.components, this.factories);
  }
}

export class ems10004 extends AbstractSystem {
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

  public widgets: Widget[] = [
    new Grid(
      new Modal("GridModal", new Map()
        .set(Role.ADMIN, viewData.ems10004adminAndInstallerGridModal)
        .set(Role.INSTALLER, viewData.ems10004adminAndInstallerGridModal)
        .set(Role.OWNER, viewData.ems10004ownerAndGuestGridModal)
        .set(Role.GUEST, viewData.ems10004ownerAndGuestGridModal)
      ))];

  constructor() {
    super();
    super.setEdge(new Edge(this.key, "", "", "2023.3.5", Role.ADMIN, true, new Date()));
    super.setConfig(this.components, this.factories);
  }
}

export class ems12786 extends AbstractSystem {
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

  public widgets: Widget[] = [
    new Grid(
      new Modal("GridModal", new Map()
        .set(Role.ADMIN, viewData.ems12786adminAndInstallerGridModal)
        .set(Role.INSTALLER, viewData.ems12786adminAndInstallerGridModal)
        .set(Role.OWNER, viewData.ems12786ownerAndGuestGridModal)
        .set(Role.GUEST, viewData.ems12786ownerAndGuestGridModal)
      ))];

  constructor() {
    super();
    super.setEdge(new Edge(this.key, "", "", "2023.3.5", Role.ADMIN, true, new Date()));
    super.setConfig(this.components, this.factories);
  }
}

export class ems30012 extends AbstractSystem {
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

  public widgets: Widget[] = [
    new Grid(
      new Modal("GridModal", new Map()
        .set(Role.ADMIN, viewData.ems30012adminAndInstallerGridModal)
        .set(Role.INSTALLER, viewData.ems30012adminAndInstallerGridModal)
        .set(Role.OWNER, viewData.ems30012ownerAndGuestGridModal)
        .set(Role.GUEST, viewData.ems30012ownerAndGuestGridModal)
      ))];

  constructor() {
    super();
    super.setEdge(new Edge(this.key, "", "", "2023.3.5", Role.ADMIN, true, new Date()));
    super.setConfig(this.components, this.factories);
  }
}

export class ems30034 extends AbstractSystem {
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

  public widgets: Widget[] = [
    new Grid(
      new Modal("GridModal", new Map()
        .set(Role.ADMIN, viewData.ems30034adminAndInstallerGridModal)
        .set(Role.INSTALLER, viewData.ems30034adminAndInstallerGridModal)
        .set(Role.OWNER, viewData.ems30034ownerAndGuestGridModal)
        .set(Role.GUEST, viewData.ems30034ownerAndGuestGridModal)
      ))];

  constructor() {
    super();
    super.setEdge(new Edge(this.key, "", "", "2023.3.5", Role.ADMIN, true, new Date()));
    super.setConfig(this.components, this.factories);
  }
}

export class ems30048 extends AbstractSystem {
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

  public widgets: Widget[] = [
    new Grid(
      new Modal("GridModal", new Map()
        .set(Role.ADMIN, viewData.ems30048adminAndInstallerGridModal)
        .set(Role.INSTALLER, viewData.ems30048adminAndInstallerGridModal)
        .set(Role.OWNER, viewData.ems30048ownerAndGuestGridModal)
        .set(Role.GUEST, viewData.ems30048ownerAndGuestGridModal)
      ))];

  constructor() {
    super();
    super.setEdge(new Edge(this.key, "", "", "2023.3.5", Role.ADMIN, true, new Date()));
    super.setConfig(this.components, this.factories);
  }
}