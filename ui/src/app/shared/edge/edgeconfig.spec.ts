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
                factories[component['factoryId']].componentIds = [...factories[component['factoryId']].componentIds, ...component['factory'].componentIds];
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
    export const CONTROLLER_SYMMETRIC_PEAKSHAVING = {
        id: "Controller.Symmetric.PeakShaving",
        natureIds: [
            "io.openems.edge.common.component.OpenemsComponent",
            "io.openems.edge.controller.api.Controller"
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

export const CONTROLLER_PEAK_SHAVING = (id: string, alias?: string): Component => ({
    id: id,
    alias: alias,
    factory: Factory.CONTROLLER_SYMMETRIC_PEAKSHAVING,
    properties: {
        alias: "",
        enabled: true,
        ['ess.id']: "ess0",
        ['meter.id']: "meter0",
        peakShavingPower: 22000,
        rechargePower: 18500
    },
    channels: {}
});