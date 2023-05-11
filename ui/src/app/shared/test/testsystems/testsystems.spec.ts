import { TestBed } from "@angular/core/testing";
import { FORMLY_CONFIG, FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateLoader, TranslateModule, TranslateService } from "@ngx-translate/core";
import { ModalComponent as GridModal } from "src/app/edge/live/common/grid/modal/modal";
import { Edge, EdgeConfig } from "src/app/shared/shared";
import { registerTranslateExtension } from "src/app/shared/translate.extension";
import { Language, MyTranslateLoader } from "src/app/shared/type/language";
import { Role } from "../../type/role";

export abstract class AbstractSystem {
  public abstract key: string;
  public edge: Edge | null = null;
  public abstract components: { [id: string]: EdgeConfig.Component };
  public config: EdgeConfig;
  public abstract widgets: Widget[]

  public createEdgeConfig(edge: Edge, components: { [id: string]: EdgeConfig.Component }, factories: { [id: string]: EdgeConfig.Factory }): EdgeConfig {
    return new EdgeConfig(edge, {
      components: components,
      factories: factories,
    } as EdgeConfig
    )
  }

  setConfig(components: { [id: string]: EdgeConfig.Component }, factories: { [id: string]: EdgeConfig.Factory }) {
    this.config = this.createEdgeConfig(this.edge, components, factories);
  }

  setEdge(edge: Edge) {
    this.edge = edge;
  }
}

export class Modal {
  public name: string;
  public fieldsWithRoles: Map<number, FormlyFieldConfig[]> = new Map();

  constructor(name: string, fieldsWithRoles: Map<number, FormlyFieldConfig[]>) {
    this.name = name;
    this.fieldsWithRoles = fieldsWithRoles;
  }
}

export abstract class Widget {
  public abstract key: string;
  public abstract modal: Modal;
}

export class Grid extends Widget {

  public key: string = "Grid"
  public modal: Modal;
  public config: EdgeConfig;

  constructor(modal: Modal) {
    super();
    this.modal = modal;
  }
}

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
  }

  public factories: { [id: string]: EdgeConfig.Factory } = {
    ['Meter.Socomec.Threephase']:
    {
      name: "Meter Socomec Threephase",
      description: "Implements a threephase Socomec meter. Actual type is identified automatically.",
      natureIds: [
        "io.openems.edge.meter.api.AsymmetricMeter",
        "io.openems.edge.meter.api.SymmetricMeter",
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
  }

  public override key: string = "ems1";

  constructor() {
    super();
    super.setEdge(new Edge(this.key, "", "", "2023.3.5", Role.ADMIN, true, new Date()));
    super.setConfig(this.components, this.factories);
  }

  private readonly ownerAndGuestGridModal: FormlyFieldConfig[] = [{
    "key": "ems1", "type": "input", "templateOptions": { "attributes": { "title": "Netz" }, "required": true, "options": [{ "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "_sum/GridActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "_sum/GridActivePower" }, false, { "type": "line", "name": "Phase L1 ", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL1", "indentation": "5%" }] }, { "type": "line", "name": "Phase L2 ", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL2", "indentation": "5%" }] }, { "type": "line", "name": "Phase L3 ", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL3", "indentation": "5%" }] }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] }] }, "wrappers": ["formly-field-modal"]
  }]
  private readonly adminAndInstallerGridModal: FormlyFieldConfig[] = [{ "key": "ems1", "type": "input", "templateOptions": { "attributes": { "title": "Netz" }, "required": true, "options": [{ "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "_sum/GridActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "_sum/GridActivePower" }, false, { "type": "line", "name": "Phase L1 ", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [{ "type": "line-item", "channel": "meter0/VoltageL1", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/CurrentL1", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/ActivePowerL1", "indentation": "5%" }] }, { "type": "line", "name": "Phase L2 ", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [{ "type": "line-item", "channel": "meter0/VoltageL2", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/CurrentL2", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/ActivePowerL2", "indentation": "5%" }] }, { "type": "line", "name": "Phase L3 ", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [{ "type": "line-item", "channel": "meter0/VoltageL3", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/CurrentL3", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/ActivePowerL3", "indentation": "5%" }] }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] }] }, "wrappers": ["formly-field-modal"] }]

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
      },
    }
  public factories: { [id: string]: EdgeConfig.Factory } = {
    ['GoodWe.Grid-Meter']:
    {
      name: "GoodWe Grid-Meter",
      description: "GoodWe Smart Meter.",
      natureIds: [
        "io.openems.edge.goodwe.gridmeter.GoodWeGridMeter",
        "io.openems.edge.meter.api.AsymmetricMeter",
        "io.openems.edge.meter.api.SymmetricMeter",
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
  }
  private readonly ownerAndGuestGridModal: FormlyFieldConfig[] = [{ "key": "ems4", "type": "input", "templateOptions": { "attributes": { "title": "Netz" }, "required": true, "options": [{ "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "_sum/GridActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "_sum/GridActivePower" }, false, { "type": "line", "name": "Phase L1 ", "indentation": "5%", "channel": "meter1/ActivePowerL1", "children": [false, false, { "type": "line-item", "channel": "meter1/ActivePowerL1", "indentation": "5%" }] }, { "type": "line", "name": "Phase L2 ", "indentation": "5%", "channel": "meter1/ActivePowerL2", "children": [false, false, { "type": "line-item", "channel": "meter1/ActivePowerL2", "indentation": "5%" }] }, { "type": "line", "name": "Phase L3 ", "indentation": "5%", "channel": "meter1/ActivePowerL3", "children": [false, false, { "type": "line-item", "channel": "meter1/ActivePowerL3", "indentation": "5%" }] }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] }] }, "wrappers": ["formly-field-modal"] }]

  private readonly adminAndInstallerGridModal: FormlyFieldConfig[] = [{ "key": "ems4", "type": "input", "templateOptions": { "attributes": { "title": "Netz" }, "required": true, "options": [{ "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "_sum/GridActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "_sum/GridActivePower" }, false, { "type": "line", "name": "Phase L1 ", "indentation": "5%", "channel": "meter1/ActivePowerL1", "children": [{ "type": "line-item", "channel": "meter1/VoltageL1", "indentation": "5%" }, { "type": "line-item", "channel": "meter1/CurrentL1", "indentation": "5%" }, { "type": "line-item", "channel": "meter1/ActivePowerL1", "indentation": "5%" }] }, { "type": "line", "name": "Phase L2 ", "indentation": "5%", "channel": "meter1/ActivePowerL2", "children": [{ "type": "line-item", "channel": "meter1/VoltageL2", "indentation": "5%" }, { "type": "line-item", "channel": "meter1/CurrentL2", "indentation": "5%" }, { "type": "line-item", "channel": "meter1/ActivePowerL2", "indentation": "5%" }] }, { "type": "line", "name": "Phase L3 ", "indentation": "5%", "channel": "meter1/ActivePowerL3", "children": [{ "type": "line-item", "channel": "meter1/VoltageL3", "indentation": "5%" }, { "type": "line-item", "channel": "meter1/CurrentL3", "indentation": "5%" }, { "type": "line-item", "channel": "meter1/ActivePowerL3", "indentation": "5%" }] }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] }] }, "wrappers": ["formly-field-modal"] }]

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
      },
    }
  public factories: { [id: string]: EdgeConfig.Factory } = {
    ['GoodWe.Grid-Meter']:
    {
      name: "GoodWe Grid-Meter",
      description: "GoodWe Smart Meter.",
      natureIds: [
        "io.openems.edge.goodwe.gridmeter.GoodWeGridMeter",
        "io.openems.edge.meter.api.AsymmetricMeter",
        "io.openems.edge.meter.api.SymmetricMeter",
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
  }
  private readonly ownerAndGuestGridModal: FormlyFieldConfig[] = [{ "key": "ems10004", "type": "input", "templateOptions": { "attributes": { "title": "Netz" }, "required": true, "options": [{ "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "_sum/GridActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "_sum/GridActivePower" }, false, { "type": "line", "name": "Phase L1 ", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL1", "indentation": "5%" }] }, { "type": "line", "name": "Phase L2 ", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL2", "indentation": "5%" }] }, { "type": "line", "name": "Phase L3 ", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL3", "indentation": "5%" }] }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] }] }, "wrappers": ["formly-field-modal"] }]

  private readonly adminAndInstallerGridModal: FormlyFieldConfig[] = [{ "key": "ems10004", "type": "input", "templateOptions": { "attributes": { "title": "Netz" }, "required": true, "options": [{ "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "_sum/GridActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "_sum/GridActivePower" }, false, { "type": "line", "name": "Phase L1 ", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [{ "type": "line-item", "channel": "meter0/VoltageL1", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/CurrentL1", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/ActivePowerL1", "indentation": "5%" }] }, { "type": "line", "name": "Phase L2 ", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [{ "type": "line-item", "channel": "meter0/VoltageL2", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/CurrentL2", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/ActivePowerL2", "indentation": "5%" }] }, { "type": "line", "name": "Phase L3 ", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [{ "type": "line-item", "channel": "meter0/VoltageL3", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/CurrentL3", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/ActivePowerL3", "indentation": "5%" }] }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] }] }, "wrappers": ["formly-field-modal"] }]

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
      },
    }
  public factories: { [id: string]: EdgeConfig.Factory } = {
    ['GoodWe.Grid-Meter']:
    {
      name: "GoodWe Grid-Meter",
      description: "GoodWe Smart Meter.",
      natureIds: [
        "io.openems.edge.goodwe.gridmeter.GoodWeGridMeter",
        "io.openems.edge.meter.api.AsymmetricMeter",
        "io.openems.edge.meter.api.SymmetricMeter",
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
  }
  private readonly ownerAndGuestGridModal: FormlyFieldConfig[] = [{ "key": "ems12786", "type": "input", "templateOptions": { "attributes": { "title": "Netz" }, "required": true, "options": [{ "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "_sum/GridActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "_sum/GridActivePower" }, false, { "type": "line", "name": "Phase L1 ", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL1", "indentation": "5%" }] }, { "type": "line", "name": "Phase L2 ", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL2", "indentation": "5%" }] }, { "type": "line", "name": "Phase L3 ", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL3", "indentation": "5%" }] }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] }] }, "wrappers": ["formly-field-modal"] }]

  private readonly adminAndInstallerGridModal: FormlyFieldConfig[] = [{ "key": "ems12786", "type": "input", "templateOptions": { "attributes": { "title": "Netz" }, "required": true, "options": [{ "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "_sum/GridActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "_sum/GridActivePower" }, false, { "type": "line", "name": "Phase L1 ", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [{ "type": "line-item", "channel": "meter0/VoltageL1", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/CurrentL1", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/ActivePowerL1", "indentation": "5%" }] }, { "type": "line", "name": "Phase L2 ", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [{ "type": "line-item", "channel": "meter0/VoltageL2", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/CurrentL2", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/ActivePowerL2", "indentation": "5%" }] }, { "type": "line", "name": "Phase L3 ", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [{ "type": "line-item", "channel": "meter0/VoltageL3", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/CurrentL3", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/ActivePowerL3", "indentation": "5%" }] }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] }] }, "wrappers": ["formly-field-modal"] }]

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
      },
    }
  public factories: { [id: string]: EdgeConfig.Factory } = {
    ['Meter.Socomec.Threephase']:
    {
      name: "Meter Socomec Threephase",
      description: "Implements a threephase Socomec meter. Actual type is identified automatically.",
      natureIds: [
        "io.openems.edge.meter.api.AsymmetricMeter",
        "io.openems.edge.meter.api.SymmetricMeter",
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
  }
  private readonly ownerAndGuestGridModal: FormlyFieldConfig[] = [{ "key": "ems30012", "type": "input", "templateOptions": { "attributes": { "title": "Netz" }, "required": true, "options": [{ "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "_sum/GridActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "_sum/GridActivePower" }, false, { "type": "line", "name": "Phase L1 ", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL1", "indentation": "5%" }] }, { "type": "line", "name": "Phase L2 ", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL2", "indentation": "5%" }] }, { "type": "line", "name": "Phase L3 ", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL3", "indentation": "5%" }] }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] }] }, "wrappers": ["formly-field-modal"] }]

  private readonly adminAndInstallerGridModal: FormlyFieldConfig[] = [{ "key": "ems30012", "type": "input", "templateOptions": { "attributes": { "title": "Netz" }, "required": true, "options": [{ "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "_sum/GridActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "_sum/GridActivePower" }, false, { "type": "line", "name": "Phase L1 ", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [{ "type": "line-item", "channel": "meter0/VoltageL1", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/CurrentL1", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/ActivePowerL1", "indentation": "5%" }] }, { "type": "line", "name": "Phase L2 ", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [{ "type": "line-item", "channel": "meter0/VoltageL2", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/CurrentL2", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/ActivePowerL2", "indentation": "5%" }] }, { "type": "line", "name": "Phase L3 ", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [{ "type": "line-item", "channel": "meter0/VoltageL3", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/CurrentL3", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/ActivePowerL3", "indentation": "5%" }] }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] }] }, "wrappers": ["formly-field-modal"] }]

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
      },
    }
  public factories: { [id: string]: EdgeConfig.Factory } = {
    ['Meter.Socomec.Threephase']:
    {
      name: "Meter Socomec Threephase",
      description: "Implements a threephase Socomec meter. Actual type is identified automatically.",
      natureIds: [
        "io.openems.edge.meter.api.AsymmetricMeter",
        "io.openems.edge.meter.api.SymmetricMeter",
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
  }
  private readonly ownerAndGuestGridModal: FormlyFieldConfig[] = [{ "key": "ems30034", "type": "input", "templateOptions": { "attributes": { "title": "Netz" }, "required": true, "options": [{ "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "_sum/GridActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "_sum/GridActivePower" }, false, { "type": "line", "name": "Phase L1 ", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL1", "indentation": "5%" }] }, { "type": "line", "name": "Phase L2 ", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL2", "indentation": "5%" }] }, { "type": "line", "name": "Phase L3 ", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL3", "indentation": "5%" }] }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] }] }, "wrappers": ["formly-field-modal"] }]

  private readonly adminAndInstallerGridModal: FormlyFieldConfig[] = [{ "key": "ems30034", "type": "input", "templateOptions": { "attributes": { "title": "Netz" }, "required": true, "options": [{ "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "_sum/GridActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "_sum/GridActivePower" }, false, { "type": "line", "name": "Phase L1 ", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [{ "type": "line-item", "channel": "meter0/VoltageL1", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/CurrentL1", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/ActivePowerL1", "indentation": "5%" }] }, { "type": "line", "name": "Phase L2 ", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [{ "type": "line-item", "channel": "meter0/VoltageL2", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/CurrentL2", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/ActivePowerL2", "indentation": "5%" }] }, { "type": "line", "name": "Phase L3 ", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [{ "type": "line-item", "channel": "meter0/VoltageL3", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/CurrentL3", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/ActivePowerL3", "indentation": "5%" }] }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] }] }, "wrappers": ["formly-field-modal"] }]

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
      },
    }
  public factories: { [id: string]: EdgeConfig.Factory } = {
    ['Meter.Socomec.Threephase']:
    {
      name: "Meter Socomec Threephase",
      description: "Implements a threephase Socomec meter. Actual type is identified automatically.",
      natureIds: [
        "io.openems.edge.meter.api.AsymmetricMeter",
        "io.openems.edge.meter.api.SymmetricMeter",
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
  }
  private readonly ownerAndGuestGridModal: FormlyFieldConfig[] = [{ "key": "ems30048", "type": "input", "templateOptions": { "attributes": { "title": "Netz" }, "required": true, "options": [{ "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "_sum/GridActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "_sum/GridActivePower" }, false, { "type": "line", "name": "Phase L1 ", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL1", "indentation": "5%" }] }, { "type": "line", "name": "Phase L2 ", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL2", "indentation": "5%" }] }, { "type": "line", "name": "Phase L3 ", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [false, false, { "type": "line-item", "channel": "meter0/ActivePowerL3", "indentation": "5%" }] }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] }] }, "wrappers": ["formly-field-modal"] }]

  private readonly adminAndInstallerGridModal: FormlyFieldConfig[] = [{ "key": "ems30048", "type": "input", "templateOptions": { "attributes": { "title": "Netz" }, "required": true, "options": [{ "lines": [{ "type": "line", "name": "Keine Netzverbindung!", "channel": "_sum/GridMode" }, { "type": "line", "name": "Bezug", "channel": "_sum/GridActivePower" }, { "type": "line", "name": "Einspeisung", "channel": "_sum/GridActivePower" }, false, { "type": "line", "name": "Phase L1 ", "indentation": "5%", "channel": "meter0/ActivePowerL1", "children": [{ "type": "line-item", "channel": "meter0/VoltageL1", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/CurrentL1", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/ActivePowerL1", "indentation": "5%" }] }, { "type": "line", "name": "Phase L2 ", "indentation": "5%", "channel": "meter0/ActivePowerL2", "children": [{ "type": "line-item", "channel": "meter0/VoltageL2", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/CurrentL2", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/ActivePowerL2", "indentation": "5%" }] }, { "type": "line", "name": "Phase L3 ", "indentation": "5%", "channel": "meter0/ActivePowerL3", "children": [{ "type": "line-item", "channel": "meter0/VoltageL3", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/CurrentL3", "indentation": "5%" }, { "type": "line-item", "channel": "meter0/ActivePowerL3", "indentation": "5%" }] }, { "type": "line-info", "name": "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen." }] }] }, "wrappers": ["formly-field-modal"] }]

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

describe('ExampleSystemsTest', () => {

  let translate: TranslateService;
  beforeEach((() => {
    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot({ loader: { provide: TranslateLoader, useClass: MyTranslateLoader, }, defaultLanguage: Language.DEFAULT.key }),
      ],
      providers: [TranslateService, { provide: FORMLY_CONFIG, multi: true, useFactory: registerTranslateExtension, deps: [TranslateService] },]
    }).compileComponents();
    translate = TestBed.inject(TranslateService);
  }));

  let testSystems: AbstractSystem[] = [
    new ems1(), //new ems4(), new ems10004(), new ems12786(), new ems30012(), new ems30034(), new ems30048()
  ];

  for (let key in testSystems) {
    let system = testSystems[key];
    for (let widget of system.widgets) {
      it(system.key + "-" + widget.modal.name, () => {
        for (let [key, modelToBeMatched] of widget.modal.fieldsWithRoles) {
          sessionStorage.setItem("old", JSON.stringify(
            GridModal.generateGridModal(system.key, system.config, Role.getRole(Role[key]), translate)))
          sessionStorage.setItem("new", JSON.stringify(modelToBeMatched))
          expect(JSON.stringify(
            GridModal.generateGridModal(system.key, system.config, Role.getRole(Role[key]), translate))).toBe(JSON.stringify(modelToBeMatched))
        }
      })
    }
  }
});