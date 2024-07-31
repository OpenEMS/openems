// @ts-strict-ignore
import { TimeUnit } from "chart.js";
import { SumState } from "src/app/index/shared/sumState";

import { Role } from "../../type/role";
import { TextIndentation } from "../modal/modal-line/modal-line";
import { OeChartTester, OeFormlyViewTester } from "../shared/testing/tester";
import { Edge } from "./edge";
import { EdgeConfig, PersistencePriority } from "./edgeconfig";

export namespace DummyConfig {

    export function dummyEdge(values: {
        edgeId?: string,
        comment?: string,
        producttype?: string,
        version?: string,
        role?: Role,
        isOnline?: boolean,
        lastmessage?: Date,
        sumState?: SumState,
        firstSetupProtocol?: Date,
    }): Edge {
        return new Edge(
            values.edgeId ?? "edge0",
            values.comment ?? "edge0",
            values.producttype ?? "",
            values.version ?? "2023.3.5",
            values.role ?? Role.ADMIN,
            values.isOnline ?? true,
            values.lastmessage ?? new Date(),
            values.sumState ?? SumState.OK,
            values.firstSetupProtocol ?? new Date(0),
        );
    }

    const DUMMY_EDGE: Edge = new Edge("edge0", "", "", "2023.3.5", Role.ADMIN, true, new Date(), SumState.OK, new Date(0));
    export function from(...components: Component[]): EdgeConfig {

        return new EdgeConfig(DUMMY_EDGE, <EdgeConfig>{
            components: <unknown>components?.reduce((acc, c) => {
                c.factoryId = c.factory.id;
                return ({ ...acc, [c.id]: c });
            }, {}),
            factories: components?.reduce((p, c) => {
                p[c.factory.id] = new EdgeConfig.Factory(c.factory.id, '', c.factory.natureIds);
                return p;
            }, {}),
        });
    }

    export function convertDummyEdgeConfigToRealEdgeConfig(edgeConfig: EdgeConfig): EdgeConfig {
        const components = Object.values(edgeConfig?.components) ?? null;

        const factories = {};
        components.forEach(obj => {
            if (factories[obj.factoryId]) {
                factories[obj.factoryId].componentIds = [...factories[obj.factoryId].componentIds, obj.id];
            } else {
                factories[obj.factoryId] = {
                    componentIds: [obj.id],
                    description: "",
                    id: obj.factoryId,
                    name: obj.factoryId,
                    natureIds: edgeConfig.factories[obj.factoryId].natureIds,
                    properties: [],
                };
            }
        });

        return new EdgeConfig(DUMMY_EDGE, <EdgeConfig>{
            components: edgeConfig.components,
            factories: factories,
        });
    }

    export namespace Factory {

        export const METER_SOCOMEC_THREEPHASE = {
            id: "Meter.Socomec.Threephase",
            natureIds: [
                "io.openems.edge.common.component.OpenemsComponent",
                "io.openems.edge.bridge.modbus.api.ModbusComponent",
                "io.openems.edge.common.modbusslave.ModbusSlave",
                "io.openems.edge.meter.api.ElectricityMeter",
                "io.openems.edge.meter.socomec.SocomecMeter",
                "io.openems.edge.meter.socomec.threephase.SocomecMeterThreephase",
            ],
        };

        export const METER_GOODWE_GRID = {
            id: "GoodWe.Grid-Meter",
            natureIds: [
                "io.openems.edge.goodwe.gridmeter.GoodWeGridMeter",
                "io.openems.edge.meter.api.ElectricityMeter",
                "io.openems.edge.bridge.modbus.api.ModbusComponent",
                "io.openems.edge.common.modbusslave.ModbusSlave",
                "io.openems.edge.common.component.OpenemsComponent",
                "io.openems.edge.timedata.api.TimedataProvider",
            ],
        };

        export const EVCS_KEBA_KECONTACT = {
            id: "Evcs.Keba.KeContact",
            natureIds: [
                "io.openems.edge.evcs.keba.kecontact.EvcsKebaKeContact",
                "io.openems.edge.common.modbusslave.ModbusSlave",
                "io.openems.edge.common.component.OpenemsComponent",
                "io.openems.edge.evcs.api.ManagedEvcs",
                "io.openems.edge.evcs.api.Evcs",
            ],
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
                "io.openems.edge.timedata.api.TimedataProvider",
            ],
        };

        export const SOLAR_EDGE_PV_INVERTER = {
            id: "SolarEdge.PV-Inverter",
            natureIds: [
                "io.openems.edge.pvinverter.sunspec.SunSpecPvInverter", "io.openems.edge.meter.api.AsymmetricMeter", "io.openems.edge.meter.api.SymmetricMeter", "io.openems.edge.bridge.modbus.api.ModbusComponent", "io.openems.edge.common.modbusslave.ModbusSlave", "io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter", "io.openems.edge.common.component.OpenemsComponent",
            ],
        };
        export const EVCS_HARDY_BARTH = {
            id: "Evcs.HardyBarth",
            natureIds: [
                "io.openems.edge.common.component.OpenemsComponent",
                "io.openems.edge.evcs.hardybarth.EvcsHardyBarth",
                "io.openems.edge.evcs.api.ManagedEvcs",
                "io.openems.edge.evcs.api.Evcs",
            ],
        };
    }

    export namespace Component {

        export const EVCS_HARDY_BARTH = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factoryId: 'Evcs.HardyBarth',
            factory: Factory.EVCS_HARDY_BARTH,
            properties: {
                enabled: "true",
            },
            channels: {},
        });

        export const SOCOMEC_GRID_METER = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factoryId: 'Meter.Socomec.Threephase',
            factory: Factory.METER_SOCOMEC_THREEPHASE,
            properties: {
                invert: false,
                modbusUnitId: 5,
                type: "GRID",
            },
            channels: {},
        });

        export const SOCOMEC_CONSUMPTION_METER = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factory: Factory.METER_SOCOMEC_THREEPHASE,
            factoryId: Factory.METER_SOCOMEC_THREEPHASE.id,
            properties: {
                invert: false,
                modbusUnitId: 5,
                type: "CONSUMPTION_METERED",
            },
            channels: {},
        });
        export const GOODWE_GRID_METER = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias,
            factory: Factory.METER_GOODWE_GRID,
            properties: {
                invert: false,
                modbusUnitId: 5,
                type: "PRODUCTION",
            },
            channels: {},
        });

        export const SOLAR_EDGE_PV_INVERTER = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias,
            factoryId: 'SolarEdge.PV-Inverter',
            factory: Factory.SOLAR_EDGE_PV_INVERTER,
            properties: {
                invert: false,
                modbusUnitId: 5,
                type: "PRODUCTION",
            },
            channels: {},
        });

        export const ESS_GENERIC_MANAGEDSYMMETRIC = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factoryId: 'Ess.Generic.ManagedSymmetric',
            factory: Factory.ESS_GENERIC_MANAGEDSYMMETRIC,
            properties: {
                invert: false,
                modbusUnitId: 5,
            },
            channels: {},
        });

        export const EVCS_KEBA_KECONTACT = (id: string, alias?: string): Component => ({
            id: id,
            alias: alias ?? id,
            factory: Factory.EVCS_KEBA_KECONTACT,
            properties: {
                invert: false,
                modbusUnitId: 5,
                // TODO
                type: "CONSUMPTION_METERED",
            },
            channels: {},
        });
    }
}

/**
 * Factories.
 */
// identifier `Factory` is also used in namespace
// eslint-disable-next-line @typescript-eslint/no-unused-vars
type Factory = {
    id: string,
    natureIds: string[],
};

/**
 * Components
 */
// identifier `Component` is also used in namespace
// eslint-disable-next-line @typescript-eslint/no-unused-vars
type Component = {
    id: string,
    alias: string, // defaults to id
    factory: Factory,
    factoryId?: string // generated
    properties: { [property: string]: any },
    channels?: {}
};

export const CHANNEL_LINE = (name: string, value: string, indentation?: TextIndentation): OeFormlyViewTester.Field => ({
    type: "channel-line",
    name: name,
    ...(indentation && { indentation: indentation }),
    value: value,
});

export const VALUE_FROM_CHANNELS_LINE = (name: string, value: string, indentation?: TextIndentation): OeFormlyViewTester.Field => ({
    type: "value-from-channels-line",
    name: name,
    ...(indentation && { indentation: indentation }),
    value: value,
});

export const PHASE_ADMIN = (name: string, voltage: string, current: string, power: string): OeFormlyViewTester.Field => ({
    type: "children-line",
    name: name,
    indentation: TextIndentation.SINGLE,
    children: [
        {
            type: "item",
            value: voltage,
        },
        {
            type: "item",
            value: current,
        },
        {
            type: "item",
            value: power,
        },
    ],
});

export const PHASE_GUEST = (name: string, power: string): OeFormlyViewTester.Field => ({
    type: "children-line",
    name: name,
    indentation: TextIndentation.SINGLE,
    children: [
        {
            type: "item",
            value: power,
        },
    ],
});

export const LINE_HORIZONTAL: OeFormlyViewTester.Field = {
    type: "horizontal-line",
};

export const LINE_INFO_PHASES_DE: OeFormlyViewTester.Field = {
    type: "info-line",
    name: "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen.",
};

export const LINE_INFO = (text: string): OeFormlyViewTester.Field => ({
    type: "info-line",
    name: text,
});

export namespace ChartConfig {

    export const LINE_CHART_OPTIONS = (period: string, chartType: 'line' | 'bar', labelString?: string): OeChartTester.Dataset.Option => ({
        type: 'option',
        options: { "responsive": true, "maintainAspectRatio": false, "elements": { "point": { "radius": 0, "hitRadius": 0, "hoverRadius": 0 }, "line": { "stepped": false, "fill": true } }, "datasets": { "bar": {}, "line": {} }, "plugins": { "colors": { "enabled": false }, "legend": { "display": true, "position": "bottom", "labels": { "color": '' } }, "tooltip": { "intersect": false, "mode": "index", "callbacks": {} } }, "scales": { "x": { "stacked": true, "offset": false, "type": "time", "ticks": { "source": "auto", "maxTicksLimit": 31 }, "bounds": "ticks", "adapters": { "date": { "locale": { "code": "de", "formatLong": {}, "localize": {}, "match": {}, "options": { "weekStartsOn": 1, "firstWeekContainsDate": 4 } } } }, "time": { "unit": period as TimeUnit, "displayFormats": { "datetime": "yyyy-MM-dd HH:mm:ss", "millisecond": "SSS [ms]", "second": "HH:mm:ss a", "minute": "HH:mm", "hour": "HH:00", "day": "dd", "week": "ll", "month": "MM", "quarter": "[Q]Q - YYYY", "year": "yyyy" } } }, "left": { ...(chartType === 'line' ? { stacked: false } : {}), "title": { "text": "kW", "display": true, "padding": 5, "font": { "size": 11 } }, "position": "left", "grid": { "display": true }, "ticks": {} } } },
    });

    export const BAR_CHART_OPTIONS = (period: string, chartType: 'line' | 'bar', labelString?: string): OeChartTester.Dataset.Option => ({
        type: 'option', options: {
            "responsive": true, "maintainAspectRatio": false, "elements": { "point": { "radius": 0, "hitRadius": 0, "hoverRadius": 0 }, "line": { "stepped": false, "fill": true } }, "datasets": { "bar": { "barPercentage": 1 }, "line": {} }, "plugins": { "colors": { "enabled": false }, "legend": { "display": true, "position": "bottom", "labels": { "color": '' } }, "tooltip": { "intersect": false, "mode": "x", "callbacks": {} } }, "scales": {
                "x": { "stacked": true, "offset": true, "type": "time", "ticks": { "source": "auto", "maxTicksLimit": 31 }, "bounds": "ticks", "adapters": { "date": { "locale": { "code": "de", "formatLong": {}, "localize": {}, "match": {}, "options": { "weekStartsOn": 1, "firstWeekContainsDate": 4 } } } }, "time": { "unit": period as TimeUnit, "displayFormats": { "datetime": "yyyy-MM-dd HH:mm:ss", "millisecond": "SSS [ms]", "second": "HH:mm:ss a", "minute": "HH:mm", "hour": "HH:00", "day": "dd", "week": "ll", "month": "MM", "quarter": "[Q]Q - YYYY", "year": "yyyy" } } }, "left": { ...(chartType === 'line' ? { stacked: false } : {}), "title": { "text": "kWh", "display": true, "padding": 5, "font": { "size": 11 } }, "position": "left", "grid": { "display": true }, "ticks": {} },
            },
        },
    });
}

describe('PersistencePriority', () => {
    it('#isLessThan', () => {
        expect(PersistencePriority.isLessThan(PersistencePriority.LOW, PersistencePriority.HIGH)).toBe(true);
        expect(PersistencePriority.isLessThan(PersistencePriority.VERY_HIGH, PersistencePriority.HIGH)).toBe(false);
        expect(PersistencePriority.isLessThan(PersistencePriority.HIGH, PersistencePriority.HIGH)).toBe(false);
        expect(PersistencePriority.isLessThan(null, PersistencePriority.HIGH)).toBe(false);
        expect(PersistencePriority.isLessThan(undefined, PersistencePriority.HIGH)).toBe(false);
        expect(PersistencePriority.isLessThan(undefined, null)).toBe(false);
    });
});
