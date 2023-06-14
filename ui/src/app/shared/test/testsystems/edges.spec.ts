import { EdgeConfig, Edge } from "../../shared";
import { Role } from "../../type/role";
import { AbstractSystem, Widget, Grid, Modal } from "./types.spec";
import { OeFormlyView } from "../../genericComponents/shared/oe-formly-component";

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

  private readonly adminAndInstallerGridModal: OeFormlyView = { "title": "Netz", "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "meter0/ActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "meter0/ActivePower" }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [{ "type": "line-item", "channel": "meter0/VoltageL1" }, { "type": "line-item", "channel": "meter0/CurrentL1" }, { "type": "line-item", "channel": "meter0/ActivePowerL1" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [{ "type": "line-item", "channel": "meter0/VoltageL2" }, { "type": "line-item", "channel": "meter0/CurrentL2" }, { "type": "line-item", "channel": "meter0/ActivePowerL2" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [{ "type": "line-item", "channel": "meter0/VoltageL3" }, { "type": "line-item", "channel": "meter0/CurrentL3" }, { "type": "line-item", "channel": "meter0/ActivePowerL3" }] }, { "type": "line-horizontal" }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] } as OeFormlyView;
  private readonly ownerAndGuestGridModal: OeFormlyView = { "title": "Netz", "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "meter0/ActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "meter0/ActivePower" }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL1" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL2" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL3" }] }, { "type": "line-horizontal" }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] } as OeFormlyView;

  public widgets: Widget[] = [new Grid(new Modal("GridModal", new Map()
    .set(Role.ADMIN, this.adminAndInstallerGridModal)
    .set(Role.INSTALLER, this.adminAndInstallerGridModal)
    .set(Role.OWNER, this.ownerAndGuestGridModal)
    .set(Role.GUEST, this.ownerAndGuestGridModal)
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
  private readonly ownerAndGuestGridModal: OeFormlyView = { "title": "Netz", "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "meter1/ActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "meter1/ActivePower" }, { "type": "line", "indentation": "5%", "channel": "meter1/ActivePowerL1", "children": [false, false, { "type": "line-item", "channel": "meter1/ActivePowerL1" }] }, { "type": "line", "indentation": "5%", "channel": "meter1/ActivePowerL2", "children": [false, false, { "type": "line-item", "channel": "meter1/ActivePowerL2" }] }, { "type": "line", "indentation": "5%", "channel": "meter1/ActivePowerL3", "children": [false, false, { "type": "line-item", "channel": "meter1/ActivePowerL3" }] }, { "type": "line-horizontal" }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] } as OeFormlyView;
  private readonly adminAndInstallerGridModal: OeFormlyView = { "title": "Netz", "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "meter1/ActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "meter1/ActivePower" }, { "type": "line", "indentation": "5%", "channel": "meter1/ActivePowerL1", "children": [{ "type": "line-item", "channel": "meter1/VoltageL1" }, { "type": "line-item", "channel": "meter1/CurrentL1" }, { "type": "line-item", "channel": "meter1/ActivePowerL1" }] }, { "type": "line", "indentation": "5%", "channel": "meter1/ActivePowerL2", "children": [{ "type": "line-item", "channel": "meter1/VoltageL2" }, { "type": "line-item", "channel": "meter1/CurrentL2" }, { "type": "line-item", "channel": "meter1/ActivePowerL2" }] }, { "type": "line", "indentation": "5%", "channel": "meter1/ActivePowerL3", "children": [{ "type": "line-item", "channel": "meter1/VoltageL3" }, { "type": "line-item", "channel": "meter1/CurrentL3" }, { "type": "line-item", "channel": "meter1/ActivePowerL3" }] }, { "type": "line-horizontal" }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] } as OeFormlyView;

  public widgets: Widget[] = [
    new Grid(
      new Modal("GridModal", new Map()
        .set(Role.ADMIN, this.adminAndInstallerGridModal)
        .set(Role.INSTALLER, this.adminAndInstallerGridModal)
        .set(Role.OWNER, this.ownerAndGuestGridModal)
        .set(Role.GUEST, this.ownerAndGuestGridModal)
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
  private readonly ownerAndGuestGridModal: OeFormlyView = { "title": "Netz", "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "meter0/ActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "meter0/ActivePower" }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL1" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL2" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL3" }] }, { "type": "line-horizontal" }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] } as OeFormlyView;

  private readonly adminAndInstallerGridModal: OeFormlyView = { "title": "Netz", "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "meter0/ActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "meter0/ActivePower" }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [{ "type": "line-item", "channel": "meter0/VoltageL1" }, { "type": "line-item", "channel": "meter0/CurrentL1" }, { "type": "line-item", "channel": "meter0/ActivePowerL1" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [{ "type": "line-item", "channel": "meter0/VoltageL2" }, { "type": "line-item", "channel": "meter0/CurrentL2" }, { "type": "line-item", "channel": "meter0/ActivePowerL2" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [{ "type": "line-item", "channel": "meter0/VoltageL3" }, { "type": "line-item", "channel": "meter0/CurrentL3" }, { "type": "line-item", "channel": "meter0/ActivePowerL3" }] }, { "type": "line-horizontal" }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] } as OeFormlyView;

  public widgets: Widget[] = [
    new Grid(
      new Modal("GridModal", new Map()
        .set(Role.ADMIN, this.adminAndInstallerGridModal)
        .set(Role.INSTALLER, this.adminAndInstallerGridModal)
        .set(Role.OWNER, this.ownerAndGuestGridModal)
        .set(Role.GUEST, this.ownerAndGuestGridModal)
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
  private readonly ownerAndGuestGridModal: OeFormlyView = { "title": "Netz", "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "meter0/ActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "meter0/ActivePower" }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL1" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL2" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL3" }] }, { "type": "line-horizontal" }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] } as OeFormlyView;

  private readonly adminAndInstallerGridModal: OeFormlyView = { "title": "Netz", "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "meter0/ActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "meter0/ActivePower" }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [{ "type": "line-item", "channel": "meter0/VoltageL1" }, { "type": "line-item", "channel": "meter0/CurrentL1" }, { "type": "line-item", "channel": "meter0/ActivePowerL1" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [{ "type": "line-item", "channel": "meter0/VoltageL2" }, { "type": "line-item", "channel": "meter0/CurrentL2" }, { "type": "line-item", "channel": "meter0/ActivePowerL2" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [{ "type": "line-item", "channel": "meter0/VoltageL3" }, { "type": "line-item", "channel": "meter0/CurrentL3" }, { "type": "line-item", "channel": "meter0/ActivePowerL3" }] }, { "type": "line-horizontal" }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] } as OeFormlyView;

  public widgets: Widget[] = [
    new Grid(
      new Modal("GridModal", new Map()
        .set(Role.ADMIN, this.adminAndInstallerGridModal)
        .set(Role.INSTALLER, this.adminAndInstallerGridModal)
        .set(Role.OWNER, this.ownerAndGuestGridModal)
        .set(Role.GUEST, this.ownerAndGuestGridModal)
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
  private readonly ownerAndGuestGridModal: OeFormlyView = { "title": "Netz", "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "meter0/ActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "meter0/ActivePower" }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL1" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL2" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL3" }] }, { "type": "line-horizontal" }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] } as OeFormlyView;

  private readonly adminAndInstallerGridModal: OeFormlyView = { "title": "Netz", "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "meter0/ActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "meter0/ActivePower" }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [{ "type": "line-item", "channel": "meter0/VoltageL1" }, { "type": "line-item", "channel": "meter0/CurrentL1" }, { "type": "line-item", "channel": "meter0/ActivePowerL1" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [{ "type": "line-item", "channel": "meter0/VoltageL2" }, { "type": "line-item", "channel": "meter0/CurrentL2" }, { "type": "line-item", "channel": "meter0/ActivePowerL2" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [{ "type": "line-item", "channel": "meter0/VoltageL3" }, { "type": "line-item", "channel": "meter0/CurrentL3" }, { "type": "line-item", "channel": "meter0/ActivePowerL3" }] }, { "type": "line-horizontal" }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] } as OeFormlyView;

  public widgets: Widget[] = [
    new Grid(
      new Modal("GridModal", new Map()
        .set(Role.ADMIN, this.adminAndInstallerGridModal)
        .set(Role.INSTALLER, this.adminAndInstallerGridModal)
        .set(Role.OWNER, this.ownerAndGuestGridModal)
        .set(Role.GUEST, this.ownerAndGuestGridModal)
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
  private readonly ownerAndGuestGridModal: OeFormlyView = { "title": "Netz", "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "meter0/ActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "meter0/ActivePower" }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL1" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL2" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL3" }] }, { "type": "line-horizontal" }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] } as OeFormlyView;

  private readonly adminAndInstallerGridModal: OeFormlyView = { "title": "Netz", "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "meter0/ActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "meter0/ActivePower" }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [{ "type": "line-item", "channel": "meter0/VoltageL1" }, { "type": "line-item", "channel": "meter0/CurrentL1" }, { "type": "line-item", "channel": "meter0/ActivePowerL1" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [{ "type": "line-item", "channel": "meter0/VoltageL2" }, { "type": "line-item", "channel": "meter0/CurrentL2" }, { "type": "line-item", "channel": "meter0/ActivePowerL2" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [{ "type": "line-item", "channel": "meter0/VoltageL3" }, { "type": "line-item", "channel": "meter0/CurrentL3" }, { "type": "line-item", "channel": "meter0/ActivePowerL3" }] }, { "type": "line-horizontal" }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] } as OeFormlyView;

  public widgets: Widget[] = [
    new Grid(
      new Modal("GridModal", new Map()
        .set(Role.ADMIN, this.adminAndInstallerGridModal)
        .set(Role.INSTALLER, this.adminAndInstallerGridModal)
        .set(Role.OWNER, this.ownerAndGuestGridModal)
        .set(Role.GUEST, this.ownerAndGuestGridModal)
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
  private readonly ownerAndGuestGridModal: OeFormlyView = { "title": "Netz", "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "meter0/ActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "meter0/ActivePower" }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL1" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL2" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL3" }] }, { "type": "line-horizontal" }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] } as OeFormlyView;

  private readonly adminAndInstallerGridModal: OeFormlyView = { "title": "Netz", "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "meter0/ActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "meter0/ActivePower" }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [{ "type": "line-item", "channel": "meter0/VoltageL1" }, { "type": "line-item", "channel": "meter0/CurrentL1" }, { "type": "line-item", "channel": "meter0/ActivePowerL1" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [{ "type": "line-item", "channel": "meter0/VoltageL2" }, { "type": "line-item", "channel": "meter0/CurrentL2" }, { "type": "line-item", "channel": "meter0/ActivePowerL2" }] }, { "type": "line", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [{ "type": "line-item", "channel": "meter0/VoltageL3" }, { "type": "line-item", "channel": "meter0/CurrentL3" }, { "type": "line-item", "channel": "meter0/ActivePowerL3" }] }, { "type": "line-horizontal" }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] } as OeFormlyView;

  public widgets: Widget[] = [
    new Grid(
      new Modal("GridModal", new Map()
        .set(Role.ADMIN, this.adminAndInstallerGridModal)
        .set(Role.INSTALLER, this.adminAndInstallerGridModal)
        .set(Role.OWNER, this.ownerAndGuestGridModal)
        .set(Role.GUEST, this.ownerAndGuestGridModal)
      ))];

  constructor() {
    super();
    super.setEdge(new Edge(this.key, "", "", "2023.3.5", Role.ADMIN, true, new Date()));
    super.setConfig(this.components, this.factories);
  }
}