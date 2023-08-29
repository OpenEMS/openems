import { TextIndentation } from "../genericComponents/modal/modal-line/modal-line";
import { OeFormlyViewTester } from "../genericComponents/shared/tester";
import { Role } from "../type/role";
import { Edge } from "./edge";
import { EdgeConfig } from "./edgeconfig";

export namespace DummyConfig {

    const DUMMY_EDGE: Edge = new Edge("edge0", "", "", "2023.3.5", Role.ADMIN, true, new Date());
    export function from(...components: Component[]): EdgeConfig {

        return new EdgeConfig(DUMMY_EDGE, <EdgeConfig><unknown>{
            components: <unknown>components?.reduce((acc, c) => ({ ...acc, [c.id]: c }), {}),
            factories: <unknown>components?.map(c => c.factory)
        });
    };

    export function convertDummyEdgeConfigToRealEdgeConfig(edgeConfig: EdgeConfig): EdgeConfig {
        let components = Object.values(edgeConfig?.components) ?? null;

        let factories = {};
        components.forEach(obj => {
            const component = obj as unknown;
            if (factories[component['factoryId']]) {
                factories[component['factoryId']].componentIds = [...factories[component['factoryId']].componentIds, ...component['factory'].componentIds];//.push(...component['factory'].componentIds)
            } else {
                factories[component['factoryId']] = {
                    componentIds: component['factory'].componentIds,
                    description: "",
                    id: component['factoryId'],
                    name: component['factoryId'],
                    natureIds: component['factory'].natureIds,
                    properties: []
                };
            }
        });

        return new EdgeConfig(DUMMY_EDGE, <EdgeConfig>{
            components: edgeConfig.components,
            factories: factories
        });
    }
}

/**
 * Factories.
 */
type Factory = {
    id: string
};

namespace Factory {

    export const METER_SOCOMEC_THREEPHASE = {
        id: "Meter.Socomec.Threephase",
        natureIds: [
            "io.openems.edge.common.component.OpenemsComponent",
            "io.openems.edge.bridge.modbus.api.ModbusComponent",
            "io.openems.edge.common.modbusslave.ModbusSlave",
            "io.openems.edge.meter.api.ElectricityMeter",
            "io.openems.edge.meter.socomec.SocomecMeter",
            "io.openems.edge.meter.socomec.threephase.SocomecMeterThreephase"
        ]
    };

    export const METER_GOODWE_GRID = {
        id: "GoodWe.Grid-Meter",
        natureIds: [
            "io.openems.edge.goodwe.gridmeter.GoodWeGridMeter",
            "io.openems.edge.meter.api.ElectricityMeter",
            "io.openems.edge.bridge.modbus.api.ModbusComponent",
            "io.openems.edge.common.modbusslave.ModbusSlave",
            "io.openems.edge.common.component.OpenemsComponent",
            "io.openems.edge.timedata.api.TimedataProvider"
        ]
    };

    export const EVCS_KEBA_KECONTACT = {
        id: "Evcs.Keba.KeContact",
        natureIds: [
            "io.openems.edge.evcs.keba.kecontact.EvcsKebaKeContact",
            "io.openems.edge.common.modbusslave.ModbusSlave",
            "io.openems.edge.common.component.OpenemsComponent",
            "io.openems.edge.evcs.api.ManagedEvcs",
            "io.openems.edge.evcs.api.Evcs"
        ]
    };

export const ESS_GENERIC_MANAGEDSYMMETRIC = {
        id: "Ess.Generic.ManagedSymmetric",
        natureIds: [
            "io.openems.edge.goodwe.common.GoodWe",
            "io.openems.edge.bridge.modbus.api.ModbusComponent",
            "io.openems.edge.common.modbusslave.ModbusSlave",
            "io.openems.edge.ess.api.SymmetricEss",
            "io.openems.edge.common.component.OpenemsComponent",
            "io.openems.edge.ess.api.HybridEss",
            "io.openems.edge.goodwe.ess.GoodWeEss",
            "io.openems.edge.ess.api.ManagedSymmetricEss",
            "io.openems.edge.timedata.api.TimedataProvider"
        ]
    };

    export const SOLAR_EDGE_PV_INVERTER = {
        id: "SolarEdge.PV-Inverter",
        natureIds: [
            "io.openems.edge.pvinverter.sunspec.SunSpecPvInverter", "io.openems.edge.meter.api.AsymmetricMeter", "io.openems.edge.meter.api.SymmetricMeter", "io.openems.edge.bridge.modbus.api.ModbusComponent", "io.openems.edge.common.modbusslave.ModbusSlave", "io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter", "io.openems.edge.common.component.OpenemsComponent"
        ]
    };
}

/**
 * Components
 */
type Component = {
    id: string,
    alias: string, // defaults to id
    factory: Factory,
    factoryId?: string // generated
    properties: { [property: string]: any },
    channels?: {}
};

export const SOCOMEC_GRID_METER = (id: string, alias?: string): Component => ({
    id: id,
    alias: alias ?? id,
    factoryId: 'Meter.Socomec.Threephase',
    factory: Factory.METER_SOCOMEC_THREEPHASE,
    properties: {
        invert: false,
        modbusUnitId: 5,
        type: "GRID"
    },
    channels: {}
});

export const SOCOMEC_CONSUMPTION_METER = (id: string, alias?: string): Component => ({
    id: id,
    alias: alias ?? id,
    factory: Factory.METER_SOCOMEC_THREEPHASE,
    factoryId: Factory.METER_SOCOMEC_THREEPHASE.id,
    properties: {
        invert: false,
        modbusUnitId: 5,
        type: "CONSUMPTION_METERED"
    },
    channels: {}
});
export const GOODWE_GRID_METER = (id: string, alias?: string): Component => ({
    id: id,
    alias: alias,
    factory: Factory.METER_GOODWE_GRID,
    properties: {
        invert: false,
        modbusUnitId: 5,
        type: "PRODUCTION"
    },
    channels: {}
});

export const SOLAR_EDGE_PV_INVERTER = (id: string, alias?: string): Component => ({
    id: id,
    alias: alias,
    factoryId: 'SolarEdge.PV-Inverter',
    factory: Factory.SOLAR_EDGE_PV_INVERTER,
    properties: {
        invert: false,
        modbusUnitId: 5,
        type: "PRODUCTION"
    },
    channels: {}
});

export const ESS_GENERIC_MANAGEDSYMMETRIC = (id: string, alias?: string): Component => ({
    id: id,
    alias: alias ?? id,
    factoryId: 'Ess.Generic.ManagedSymmetric',
    factory: Factory.ESS_GENERIC_MANAGEDSYMMETRIC,
    properties: {
        invert: false,
        modbusUnitId: 5
    },
    channels: {}
});

export const EVCS_KEBA_KECONTACT = (id: string, alias?: string): Component => ({
    id: id,
    alias: alias ?? id,
    factory: Factory.EVCS_KEBA_KECONTACT,
    properties: {
        invert: false,
        modbusUnitId: 5,
        // TODO
        type: "CONSUMPTION_METERED"
    },
    channels: {}
});


export const CHANNEL_LINE = (name: string, value: string, indentation?: TextIndentation): OeFormlyViewTester.Field => ({
    type: "channel-line",
    name: name,
    ...(indentation && { indentation: indentation }),
    value: value
});

export const VALUE_FROM_CHANNELS_LINE = (name: string, value: string, indentation?: TextIndentation): OeFormlyViewTester.Field => ({
    type: "value-from-channels-line",
    name: name,
    ...(indentation && { indentation: indentation }),
    value: value
});

export const PHASE_ADMIN = (name: string, voltage: string, current: string, power: string): OeFormlyViewTester.Field => ({
    type: "children-line",
    name: name,
    indentation: TextIndentation.SINGLE,
    children: [
        {
            type: "item",
            value: voltage
        },
        {
            type: "item",
            value: current
        },
        {
            type: "item",
            value: power
        }
    ]
});

export const PHASE_GUEST = (name: string, power: string): OeFormlyViewTester.Field => ({
    type: "children-line",
    name: name,
    indentation: TextIndentation.SINGLE,
    children: [
        {
            type: "item",
            value: power
        }
    ]
});

export const LINE_HORIZONTAL: OeFormlyViewTester.Field = {
    type: "horizontal-line"
};

export const LINE_INFO_PHASES_DE: OeFormlyViewTester.Field = {
    type: "info-line",
    name: "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
};

export const LINE_INFO = (text: string): OeFormlyViewTester.Field => ({
    type: "info-line",
    name: text
});
