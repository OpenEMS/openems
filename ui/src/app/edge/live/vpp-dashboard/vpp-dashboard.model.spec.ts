import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { VppDashboardModel } from "./vpp-dashboard.model";

describe("VppDashboardModel", () => {
    const edge = DummyConfig.dummyEdge({ version: "2025.1.0" });
    const config = DummyConfig.from(
        DummyConfig.Component.SOLAR_EDGE_PV_INVERTER("pv0"),
        DummyConfig.Component.ESS_GENERIC_MANAGEDSYMMETRIC("ess0"),
        DummyConfig.Component.SOCOMEC_GRID_METER("grid0"),
        DummyConfig.Component.EVCS_MENNEKES("evcs0", "Garage charger"),
    );

    it("builds live read-only asset telemetry from configured components", () => {
        const state = VppDashboardModel.build(
            config,
            edge,
            {
                "_sum/ProductionActivePower": 2_400,
                "_sum/EssActivePower": -1_100,
                "_sum/EssSoc": 76,
                "_sum/GridActivePower": 500,
                "evcs0/ActivePower": 3_600,
            },
            new Date(1_000),
            2_000,
        );

        expect(state.assets).toEqual([
            {
                id: "pv",
                type: "pv",
                name: "VPP.ASSETS.PV",
                icon: "sunny-outline",
                status: "AVAILABLE",
                power: 2_400,
                soc: null,
            },
            {
                id: "battery",
                type: "battery",
                name: "VPP.ASSETS.BATTERY",
                icon: "battery-charging-outline",
                status: "AVAILABLE",
                power: -1_100,
                soc: 76,
            },
            {
                id: "grid",
                type: "grid",
                name: "VPP.ASSETS.GRID",
                icon: "flash-outline",
                status: "AVAILABLE",
                power: 500,
                soc: null,
            },
            {
                id: "ev:evcs0",
                type: "ev",
                name: "Garage charger",
                icon: "car-outline",
                status: "AVAILABLE",
                power: 3_600,
                soc: null,
            },
        ]);
        expect(state.edgeStatus).toBe("ONLINE");
        expect(state.telemetryStatus).toBe("CURRENT");
        expect(state.solplanetStatus).toBe("NOT_CONFIGURED");
    });

    it("marks configured assets unknown instead of treating missing telemetry as zero", () => {
        const state = VppDashboardModel.build(config, edge, { "_sum/EssSoc": 50 }, new Date(1_000), 2_000);

        expect(state.assets.find((asset) => asset.type === "battery")).toEqual(
            jasmine.objectContaining({
                status: "UNKNOWN",
                power: null,
                soc: 50,
            }),
        );
        expect(state.assets.find((asset) => asset.type === "pv")?.power).toBeNull();
    });

    it("does not report telemetry as current before receiving any channel values", () => {
        const state = VppDashboardModel.build(config, edge, {}, new Date(1_000), 2_000);

        expect(state.telemetryStatus).toBe("UNKNOWN");
    });

    it("marks telemetry stale when no recent update is available", () => {
        const state = VppDashboardModel.build(
            config,
            edge,
            { "_sum/ProductionActivePower": 1_000 },
            new Date(1_000),
            22_000,
        );

        expect(state.telemetryStatus).toBe("STALE");
        expect(state.assets.find((asset) => asset.type === "pv")?.status).toBe("STALE");
    });

    it("marks assets offline and does not present stale values as current", () => {
        const offlineEdge = DummyConfig.dummyEdge({ isOnline: false });
        const state = VppDashboardModel.build(
            config,
            offlineEdge,
            { "_sum/ProductionActivePower": 1_000 },
            new Date(1_000),
            2_000,
        );

        expect(state.edgeStatus).toBe("OFFLINE");
        expect(state.telemetryStatus).toBe("STALE");
        expect(state.assets.find((asset) => asset.type === "pv")?.status).toBe("OFFLINE");
    });

    it("shows unconfigured assets and subscribes only to read-only telemetry channels", () => {
        const emptyConfig = DummyConfig.from();
        const channels = VppDashboardModel.getChannelAddresses(emptyConfig, edge).map((channel) =>
            channel.toString(),
        );
        const state = VppDashboardModel.build(emptyConfig, edge, {}, new Date(1_000), 2_000);

        expect(state.assets.length).toBe(4);
        expect(state.assets.every((asset) => asset.status === "NOT_CONFIGURED")).toBe(true);
        expect(channels).toEqual([]);
        expect(state.telemetryStatus).toBe("UNKNOWN");
    });

    it("subscribes to channels for configured assets only", () => {
        const channels = VppDashboardModel.getChannelAddresses(config, edge).map((channel) =>
            channel.toString(),
        );

        expect(channels).toEqual([
            "_sum/ProductionActivePower",
            "_sum/EssSoc",
            "_sum/EssActivePower",
            "_sum/GridActivePower",
            "evcs0/ActivePower",
        ]);
    });
});
