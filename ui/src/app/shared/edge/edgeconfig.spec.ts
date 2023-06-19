import { Role } from "../type/role";
import { Edge } from "./edge";
import { EdgeConfig } from "./edgeconfig";

export namespace DummyConfig {

    const DUMMY_EDGE: Edge = new Edge("edge0", "", "", "2023.3.5", Role.ADMIN, true, new Date());

    export function from(...components: Component[]): EdgeConfig {
        components.forEach(c => {
            c.factoryId = c.factory.id;
            c.alias = c.alias || c.id;
            c.properties.alias = c.alias;
            c.channels = c.channels || {};
        });

        return new EdgeConfig(DUMMY_EDGE, <EdgeConfig>{
            components: <unknown>components.reduce((acc, c) => ({ ...acc, [c.id]: c }), {}),
            factories: <unknown>components.map(c => c.factory)
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
    alias: alias,
    factory: Factory.METER_SOCOMEC_THREEPHASE,
    properties: {
        invert: false,
        modbusUnitId: 5,
        type: "GRID"
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
        type: "GRID"
    },
    channels: {}
});
