// @ts-strict-ignore
import { Subject } from "rxjs";
import { GetSchedule } from "src/app/shared/components/edge/config-components/energy/getSchedule";
import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";
import { ChannelAddress, Currency, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { EnergyflowComponent } from "./flow.component";

/**
 * Unit tests for {@link EnergyflowComponent}.
 *
 * The component extends {@link AbstractFlatWidget}, which has a heavy DI graph. Like other flat widget specs, the logic
 * is exercised via the prototype with a stub `this` that only contains the fields the methods read.
 */
describe("EnergyflowComponent", () => {
    const Static = EnergyflowComponent as any;

    beforeEach(async () => {
        // registers the "de" locale used for number formatting
        await TestingUtils.sharedSetup();
    });

    function createComponent(overrides: Record<string, any> = {}): any {
        const component: any = Object.create(EnergyflowComponent.prototype);
        Object.assign(component, {
            NODES: Static.LAYOUTS.wide,
            compact: false,
            SOC_CIRCUMFERENCE: 2 * Math.PI * 50,
            uid: "test",
            production: null,
            gridBuy: 0,
            gridSell: 0,
            storageCharge: 0,
            storageDischarge: 0,
            consumption: null,
            soc: null,
            hasStorage: false,
            flows: [],
            idlePaths: [],
            consumers: [],
            today: null,
            todayCost: null,
            evcss: [],
            meters: [],
            currencyLabel: null,
            currencyCode: "EUR",
            todayTimer: null,
            resizeObserver: null,
            lastCurrentData: null,
            iconCache: new Map(),
            sanitizer: { bypassSecurityTrustHtml: (value: string) => value },
            translate: {
                instant: (key: string, params?: { value: string }) => (params ? key + " " + params.value : key),
                setTranslation: jasmine.createSpy("setTranslation"),
            },
            ...overrides,
        });
        return component;
    }

    function data(values: Record<string, number | null>): CurrentData {
        return { allComponents: values } as CurrentData;
    }

    function meter(id: string, alias: string): EdgeConfig.Component {
        return new EdgeConfig.Component(id, alias, true, false, "Meter.Socomec.Threephase", {});
    }

    function evcs(id: string, alias: string) {
        return { alias: alias, powerChannel: new ChannelAddress(id, "ActivePower") };
    }

    function flowPowers(component: any): Record<string, number> {
        return Object.fromEntries(
            component.flows.map((flow) => [flow.pathId.replace("oe-ef-test-", ""), Math.round(flow.power)]),
        );
    }

    describe("static helpers", () => {
        it("scales the stroke width with the power (2 to 10)", () => {
            expect(Static.strokeWidth(0)).toBe(2);
            expect(Static.strokeWidth(2500)).toBe(4.5);
            expect(Static.strokeWidth(20000)).toBe(10);
        });

        it("makes the dots faster with more power", () => {
            expect(Static.duration(0)).toBe("5s");
            expect(Static.duration(5000)).toBe("2s");
            expect(Static.duration(20000)).toBe("1s");
        });

        it("derives the consumer icon from the alias", () => {
            expect(Static.consumerIcon("Wallbox", true)).toBe("car-outline");
            expect(Static.consumerIcon("Wärmepumpe", false)).toBe("flame-outline");
            expect(Static.consumerIcon("Heating rod", false)).toBe("flame-outline");
            expect(Static.consumerIcon("Klimaanlage", false)).toBe("snow-outline");
            expect(Static.consumerIcon("Emergency power", false)).toBe("shield-checkmark-outline");
            expect(Static.consumerIcon("Pool", false)).toBe("speedometer-outline");
            expect(Static.consumerIcon(null, false)).toBe("speedometer-outline");
        });

        it("formats a missing energy value as dash", () => {
            expect(Static.formatEnergyValue(null)).toBe("-");
            expect(Static.formatEnergyValue(1500)).toContain("kWh");
        });
    });

    describe("#onCurrentData()", () => {
        it("splits production into house, storage and grid", () => {
            const component = createComponent();
            component.onCurrentData(
                data({
                    "_sum/ProductionActivePower": 7900,
                    "_sum/GridActivePower": -2100,
                    "_sum/EssDischargePower": -3100,
                    "_sum/ConsumptionActivePower": 2700,
                    "_sum/EssSoc": 64,
                    "_sum/EssCapacity": 10000,
                }),
            );

            expect(flowPowers(component)).toEqual({
                "production-consumption": 2700,
                "production-storage": 3100,
                "production-grid": 2100,
            });
            expect(component.idlePaths.length).toBe(3);
            expect(component.hasStorage).toBeTrue();
            expect(component.socDash).toBe("201.1 999");
            expect(component.storageEstimate).toContain("EDGE.INDEX.ENERGYFLOW.FULL_AT");
        });

        it("supplies the house from storage and grid at night", () => {
            const component = createComponent();
            component.onCurrentData(
                data({
                    "_sum/ProductionActivePower": 0,
                    "_sum/GridActivePower": 500,
                    "_sum/EssActivePower": 1500,
                    "_sum/ConsumptionActivePower": 2000,
                    "_sum/EssSoc": 40,
                    "_sum/EssCapacity": 10000,
                }),
            );

            expect(flowPowers(component)).toEqual({
                "storage-consumption": 1500,
                "grid-consumption": 500,
            });
            expect(component.storageEstimate).toContain("EDGE.INDEX.ENERGYFLOW.EMPTY_AT");
        });

        it("draws discharging into the grid in reverse direction", () => {
            const component = createComponent();
            component.onCurrentData(
                data({
                    "_sum/GridActivePower": -2000,
                    "_sum/EssDischargePower": 3000,
                    "_sum/ConsumptionActivePower": 1000,
                    "_sum/EssSoc": 80,
                }),
            );

            expect(flowPowers(component)).toEqual({
                "storage-consumption": 1000,
                "grid-storage-r": 2000,
            });
            expect(component.flows[1].reverse).toBeTrue();
            expect(component.storageEstimate).toBeNull();
        });

        it("draws charging from the grid", () => {
            const component = createComponent();
            component.onCurrentData(
                data({
                    "_sum/GridActivePower": 2000,
                    "_sum/EssDischargePower": -2000,
                    "_sum/ConsumptionActivePower": 0,
                    "_sum/EssSoc": 20,
                    "_sum/EssCapacity": 1,
                }),
            );

            expect(flowPowers(component)).toEqual({ "grid-storage": 2000 });
            // estimate below 3 minutes is hidden
            expect(component.storageEstimate).toBeNull();
        });

        it("hides storage paths without storage", () => {
            const component = createComponent();
            component.onCurrentData(
                data({
                    "_sum/ProductionActivePower": 1000,
                    "_sum/ConsumptionActivePower": 1000,
                }),
            );

            expect(component.hasStorage).toBeFalse();
            expect(component.socDash).toBe("0 999");
            expect(Object.keys(flowPowers(component))).toEqual(["production-consumption"]);
            expect(component.idlePaths.length).toBe(2);
        });

        it("shows the grid buy price when the currency is known", () => {
            const component = createComponent({ currencyLabel: Currency.getCurrencyLabelByCurrency("EUR") });
            component.onCurrentData(data({ "_sum/GridBuyPrice": 260 }));

            expect(component.gridPrice).toContain("26");
        });

        it("hides the grid buy price without currency", () => {
            const component = createComponent();
            component.onCurrentData(data({ "_sum/GridBuyPrice": 260 }));

            expect(component.gridPrice).toBeNull();
        });
    });

    describe("#updateConsumers()", () => {
        it("lists no consumers without EVCS and consumption meters", () => {
            const component = createComponent({ consumption: 1000 });
            component.updateConsumers({});

            expect(component.consumers).toEqual([]);
        });

        it("adds EVCS, consumption meters and other consumption as single nodes", () => {
            const component = createComponent({
                consumption: 3000,
                evcss: [evcs("evcs0", "Wallbox")],
                meters: [meter("meter0", "Heizstab"), meter("meter1", "Emergency power supply")],
            });
            component.updateConsumers({ "evcs0/ActivePower": 1400, "meter0/ActivePower": 0 });

            const consumers = component.consumers;
            expect(consumers.map((c) => c.icon)).toEqual([
                "car-outline",
                "flame-outline",
                "shield-checkmark-outline",
                "ellipsis-horizontal",
            ]);
            expect(consumers.map((c) => c.power)).toEqual([1400, 0, null, 1600]);
            expect(consumers.map((c) => c.active)).toEqual([true, false, false, true]);
            expect(consumers[2].name).toBe("Emergency power…");
            expect(consumers.every((c) => c.r === 26)).toBeTrue();
            expect(consumers[1].flow.dots).toEqual([]);
            expect(consumers[0].flow.dots.length).toBe(3);
        });

        it("places consumers of one category side by side when there are many", () => {
            const component = createComponent({
                consumption: 6000,
                evcss: [evcs("evcs0", "Wallbox Garage"), evcs("evcs1", "Wallbox Carport")],
                meters: [
                    meter("meter0", "Wärmepumpe"),
                    meter("meter1", "Heizstab"),
                    meter("meter2", "Klimaanlage"),
                    meter("meter3", "Pool"),
                ],
            });
            component.updateConsumers({ "evcs0/ActivePower": 1000, "evcs1/ActivePower": 2000 });

            const consumers = component.consumers;
            const rows = new Set(consumers.map((c) => c.y));
            expect(consumers.length).toBe(7);
            expect(rows.size).toBe(5);
            expect(consumers.every((c) => c.r === 22)).toBeTrue();

            // second wallbox sits to the right of the first one, the line to the first carries both
            const [first, second] = consumers;
            expect(second.y).toBe(first.y);
            expect(second.x).toBeGreaterThan(first.x);
            expect(first.flow.power).toBe(3000);
            expect(second.flow.path).toMatch(/^M\d+,\d+ L/);

            expect(component.viewBox).not.toBe("0 0 680 460");
        });

        it("limits the rows and keeps other consumption as last row", () => {
            const meters = Array.from({ length: 19 }, (_, i) => meter("meter" + i, "Meter " + i));
            const component = createComponent({ consumption: 1000, meters: meters });
            component.updateConsumers({});

            const rows = [...new Set(component.consumers.map((c) => c.y))];
            expect(rows.length).toBe(6);
            expect(component.consumers[component.consumers.length - 1].icon).toBe("ellipsis-horizontal");
        });

        it("lists all consumers one by one in the compact layout", () => {
            const meters = Array.from({ length: 7 }, (_, i) => meter("meter" + i, "Meter " + i));
            const component = createComponent({ consumption: 1000, meters: meters, compact: true });
            component.updateConsumers({});

            expect(component.consumers.length).toBe(8);
            expect(component.consumers.every((c) => c.r === 26)).toBeTrue();
        });
    });

    describe("#viewBox", () => {
        it("depends on layout and consumers", () => {
            expect(createComponent({ compact: true }).viewBox).toBe("0 0 400 440");
            expect(createComponent().viewBox).toBe("0 0 560 460");
            expect(createComponent({ consumers: [{ x: 625, r: 26 }] }).viewBox).toBe("0 0 680 460");
        });
    });

    describe("#icon()", () => {
        it("inlines and caches the SVG markup of an ionicon", () => {
            const component = createComponent();
            const markup = component.icon("sunny-outline");

            expect(markup).toContain("<");
            expect(markup).not.toContain("<svg");
            expect(component.icon("sunny-outline")).toBe(markup);
        });

        it("falls back to the default icon for unknown names", () => {
            const component = createComponent();

            expect(component.icon("unknown")).toBe(component.icon("speedometer-outline"));
        });
    });

    describe("#getChannelAddresses()", () => {
        it("subscribes sum channels, consumption meters and EVCS", () => {
            const config = DummyConfig.convertDummyEdgeConfigToRealEdgeConfig(
                DummyConfig.from(
                    DummyConfig.Component.SOCOMEC_CONSUMPTION_METER("meter0", "Wärmepumpe"),
                    DummyConfig.Component.SOCOMEC_GRID_METER("meter1", "Netz"),
                    DummyConfig.Component.EVCS_KEBA_KECONTACT("evcs0", "Wallbox"),
                ),
            );
            const component = createComponent({ config: config, edge: DummyConfig.dummyEdge({}) });

            const channels = component.getChannelAddresses().map((c: ChannelAddress) => c.toString());

            expect(channels).toContain("_sum/ProductionActivePower");
            expect(channels).toContain("_sum/EssDischargePower");
            expect(channels).toContain("meter0/ActivePower");
            expect(channels).not.toContain("meter1/ActivePower");
            expect(channels.some((c) => c.startsWith("evcs0/"))).toBeTrue();
            expect(component.meters.map((m) => m.id)).toEqual(["meter0"]);
            expect(component.evcss.length).toBe(1);
        });
    });

    describe("#updateTodayCost()", () => {
        const QUARTER = 15 * 60 * 1000;
        const start = new Date(2026, 9, 13, 10, 0).getTime();
        const timestamps = [0, 1, 2].map((i) => new Date(start + i * QUARTER).toISOString());

        function edgeWith(gridPower: (number | null)[], prices: (number | null)[]) {
            return {
                sendRequest: () =>
                    Promise.resolve({
                        result: {
                            timestamps: timestamps,
                            data: { "_sum/GridActivePower": gridPower, "_sum/GridBuyPrice": prices },
                        },
                    }),
            };
        }

        function createCostComponent(edge: any): any {
            const component = createComponent({ edge: edge, websocket: {} });
            spyOn(component, "formatCurrency").and.callFake((value: number) => value.toFixed(3));
            return component;
        }

        beforeEach(() => {
            spyOn(GetSchedule, "getSchedule").and.returnValue(Promise.resolve(null));
        });

        it("distributes the metered energy by grid buy power and prices each quarter hour", async () => {
            const component = createCostComponent(edgeWith([1000, 0, 3000], [200, null, 300]));
            await component.updateTodayCost(1000);

            // 250 Wh x 200 /MWh + 750 Wh x 300 /MWh
            expect(component.todayCost).toBe("0.275");
        });

        it("uses schedule prices when historic prices are missing", async () => {
            (GetSchedule.getSchedule as jasmine.Spy).and.returnValue(
                Promise.resolve({
                    data: [
                        { timestamp: new Date(start), _sum: { GridBuyPrice: 400 } },
                        { timestamp: new Date(start), _sum: {} },
                    ],
                }),
            );
            const component = createCostComponent(edgeWith([1000, 1000, 0], [null, null, null]));
            await component.updateTodayCost(2000);

            // first quarter hour from the schedule, second with the last known price
            expect(component.todayCost).toBe("0.800");
        });

        it("hides the cost without any price", async () => {
            const component = createCostComponent(edgeWith([1000, 0, 0], [null, null, null]));
            await component.updateTodayCost(1000);

            expect(component.todayCost).toBeNull();
        });

        it("shows zero cost without grid buy", async () => {
            const component = createCostComponent(edgeWith([0, -500, null], [200, 200, 200]));
            await component.updateTodayCost(0);

            expect(component.todayCost).toBe("0.000");
        });

        it("hides the cost when energy was bought but no power was recorded", async () => {
            const component = createCostComponent(edgeWith([0, 0, 0], [200, 200, 200]));
            await component.updateTodayCost(500);

            expect(component.todayCost).toBeNull();
        });

        it("hides the cost without edge, energy or on errors", async () => {
            const component = createCostComponent(null);
            await component.updateTodayCost(1000);
            expect(component.todayCost).toBeNull();

            const failing = createCostComponent({ sendRequest: () => Promise.reject(new Error("offline")) });
            failing.todayCost = "1.000";
            await failing.updateTodayCost(1000);
            expect(failing.todayCost).toBeNull();

            const noEnergy = createCostComponent(edgeWith([1000, 0, 0], [200, 200, 200]));
            await noEnergy.updateTodayCost(null);
            expect(noEnergy.todayCost).toBeNull();
        });
    });

    describe("#formatCurrency()", () => {
        it("formats with the currency code and falls back for invalid codes", () => {
            expect(createComponent({ currencyCode: "EUR" }).formatCurrency(1.5)).toContain("€");
            expect(createComponent({ currencyCode: "invalid" }).formatCurrency(1.5)).toBe("1.50");
        });
    });

    describe("#updateToday()", () => {
        it("sets the daily values and calculates the cost", async () => {
            const component = createComponent({
                service: {
                    queryEnergy: () =>
                        Promise.resolve({
                            result: {
                                data: {
                                    "_sum/ProductionActiveEnergy": 31400,
                                    "_sum/ConsumptionActiveEnergy": 15000,
                                    "_sum/GridBuyActiveEnergy": 3000,
                                    "_sum/GridSellActiveEnergy": 13800,
                                },
                            },
                        }),
                },
            });
            const cost = spyOn(component, "updateTodayCost").and.returnValue(Promise.resolve());
            await component.updateToday();

            expect(component.today).toEqual({
                production: 31400,
                consumption: 15000,
                gridBuy: 3000,
                gridSell: 13800,
                autarchy: 80,
            });
            expect(cost).toHaveBeenCalledWith(3000);
        });

        it("keeps the last values on errors", async () => {
            const today = { production: 1 };
            const component = createComponent({
                today: today,
                service: { queryEnergy: () => Promise.reject(new Error("offline")) },
            });
            await component.updateToday();

            expect(component.today).toBe(today);
        });
    });

    describe("lifecycle", () => {
        let host: HTMLElement;

        beforeEach(() => {
            host = document.createElement("div");
            host.style.width = "400px";
            document.body.appendChild(host);
        });

        afterEach(() => {
            host.remove();
        });

        it("loads translations and today's values and switches to the compact layout on narrow hosts", async () => {
            const component = createComponent({
                config: null,
                host: { nativeElement: host },
                zone: { run: (fn: () => void) => fn() },
                lastCurrentData: data({ "_sum/ConsumptionActivePower": 500 }),
                service: { queryEnergy: () => Promise.reject(new Error("offline")) },
                dataService: { unsubscribeFromChannels: jasmine.createSpy("unsubscribe") },
                stopOnDestroy: new Subject<void>(),
                subscription: [],
            });
            spyOn(component, "getChannelAddresses").and.returnValue([]);

            component.afterIsInitialized();
            await new Promise((resolve) => setTimeout(resolve, 100));

            expect(component.currencyCode).toBe("EUR");
            expect(component.translate.setTranslation).toHaveBeenCalled();
            expect(component.todayTimer).not.toBeNull();
            expect(component.compact).toBeTrue();
            expect(component.NODES).toBe(Static.LAYOUTS.compact);
            expect(component.consumption).toBe(500);

            component.ngOnDestroy();
            expect(component.dataService.unsubscribeFromChannels).toHaveBeenCalled();
        });
    });
});
