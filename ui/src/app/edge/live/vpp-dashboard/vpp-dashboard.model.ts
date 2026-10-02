import { EvcsComponent } from "src/app/shared/components/edge/config-components/evcs/evcsComponent";
import { ChannelAddress, Edge, EdgeConfig } from "src/app/shared/shared";

export type VppAssetType = "pv" | "battery" | "grid" | "ev";
export type VppAssetStatus = "AVAILABLE" | "NOT_CONFIGURED" | "UNKNOWN" | "STALE" | "OFFLINE";
export type VppIntegrationStatus = "NOT_CONFIGURED" | "DISABLED" | "UNVERIFIED";

export interface VppAsset {
    id: string;
    type: VppAssetType;
    name: string;
    icon: string;
    status: VppAssetStatus;
    power: number | null;
    soc: number | null;
}

export interface VppDashboardState {
    assets: VppAsset[];
    edgeStatus: "ONLINE" | "OFFLINE";
    telemetryStatus: "CURRENT" | "STALE" | "UNKNOWN";
    solplanetStatus: VppIntegrationStatus;
}

const TELEMETRY_STALE_AFTER_MS = 20_000;

export namespace VppDashboardModel {
    export function getChannelAddresses(config: EdgeConfig, edge: Edge): ChannelAddress[] {
        const channels: ChannelAddress[] = [];
        const hasPv = getEnabledProductionComponents(config).length > 0;
        const hasBattery = getEnabledBatteryComponents(config).length > 0;
        const hasGrid = getEnabledGridMeters(config).length > 0;

        if (hasPv) {
            channels.push(new ChannelAddress("_sum", "ProductionActivePower"));
        }
        if (hasBattery) {
            channels.push(
                new ChannelAddress("_sum", "EssSoc"),
                new ChannelAddress("_sum", "EssActivePower"),
            );
        }
        if (hasGrid) {
            channels.push(new ChannelAddress("_sum", "GridActivePower"));
        }

        for (const charger of getEnabledEvcsComponents(config, edge)) {
            channels.push(charger.powerChannel);
        }

        return channels;
    }

    export function build(
        config: EdgeConfig,
        edge: Edge,
        channelValues: Record<string, unknown>,
        lastUpdated: Date | null,
        now: number,
    ): VppDashboardState {
        const online = edge.isOnline;
        const age = lastUpdated == null ? null : now - lastUpdated.getTime();
        const fresh = online && age != null && age >= 0 && age <= TELEMETRY_STALE_AFTER_MS;
        const evcsComponents = getEnabledEvcsComponents(config, edge);
        const assets: VppAsset[] = [
            createAsset(
                "pv",
                "pv",
                "VPP.ASSETS.PV",
                "sunny-outline",
                getEnabledProductionComponents(config).length > 0,
                readNumber(channelValues, "_sum/ProductionActivePower"),
                null,
                online,
                fresh,
            ),
            createAsset(
                "battery",
                "battery",
                "VPP.ASSETS.BATTERY",
                "battery-charging-outline",
                getEnabledBatteryComponents(config).length > 0,
                readNumber(channelValues, "_sum/EssActivePower"),
                readNumber(channelValues, "_sum/EssSoc"),
                online,
                fresh,
            ),
            createAsset(
                "grid",
                "grid",
                "VPP.ASSETS.GRID",
                "flash-outline",
                getEnabledGridMeters(config).length > 0,
                readNumber(channelValues, "_sum/GridActivePower"),
                null,
                online,
                fresh,
            ),
        ];

        if (evcsComponents.length === 0) {
            assets.push(
                createAsset(
                    "ev",
                    "ev",
                    "VPP.ASSETS.EV",
                    "car-outline",
                    false,
                    null,
                    null,
                    online,
                    fresh,
                ),
            );
        }

        for (const charger of evcsComponents) {
            assets.push(
                createAsset(
                    "ev:" + charger.id,
                    "ev",
                    charger.alias || charger.id,
                    "car-outline",
                    true,
                    readNumber(channelValues, charger.powerChannel.toString()),
                    null,
                    online,
                    fresh,
                ),
            );
        }

        const hasConfiguredAssets = assets.some((asset) => asset.status !== "NOT_CONFIGURED");
        const hasTelemetry = assets.some((asset) => asset.power != null || asset.soc != null);

        return {
            assets,
            edgeStatus: online ? "ONLINE" : "OFFLINE",
            telemetryStatus: !hasConfiguredAssets || !hasTelemetry ? "UNKNOWN" : fresh ? "CURRENT" : "STALE",
            solplanetStatus: getSolplanetStatus(config),
        };
    }

    function getEnabledProductionComponents(config: EdgeConfig): EdgeConfig.Component[] {
        const producerMeters = config
            .getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
            .filter((component) => component.isEnabled && config.isProducer(component));
        const dcChargers = config
            .getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
            .filter((component) => component.isEnabled);
        return [...producerMeters, ...dcChargers];
    }

    function getEnabledBatteryComponents(config: EdgeConfig): EdgeConfig.Component[] {
        return config
            .getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss")
            .filter(
                (component) =>
                    component.isEnabled &&
                    !config
                        .getNatureIdsByFactoryId(component.factoryId)
                        .includes("io.openems.edge.ess.api.MetaEss"),
            );
    }

    function getEnabledGridMeters(config: EdgeConfig): EdgeConfig.Component[] {
        return config
            .getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
            .filter((component) => component.isEnabled && config.isTypeGrid(component));
    }

    function getEnabledEvcsComponents(config: EdgeConfig, edge: Edge): EvcsComponent[] {
        return EvcsComponent.getComponents(config, edge).filter((component) => component.isEnabled);
    }

    function getSolplanetStatus(config: EdgeConfig): VppIntegrationStatus {
        const components = Object.values(config.components).filter((component) =>
            component.factoryId.toLowerCase().includes("solplanet"),
        );

        if (components.length === 0) {
            return "NOT_CONFIGURED";
        }
        return components.some((component) => component.isEnabled) ? "UNVERIFIED" : "DISABLED";
    }

    function createAsset(
        id: string,
        type: VppAssetType,
        name: string,
        icon: string,
        configured: boolean,
        power: number | null,
        soc: number | null,
        online: boolean,
        fresh: boolean,
    ): VppAsset {
        let status: VppAssetStatus;
        if (!configured) {
            status = "NOT_CONFIGURED";
        } else if (!online) {
            status = "OFFLINE";
        } else if (!fresh) {
            status = "STALE";
        } else if (power == null) {
            status = "UNKNOWN";
        } else {
            status = "AVAILABLE";
        }

        return { id, type, name, icon, status, power, soc };
    }

    function readNumber(values: Record<string, unknown>, channelAddress: string): number | null {
        const value = values[channelAddress];
        if (typeof value === "number" && Number.isFinite(value)) {
            return value;
        }
        if (typeof value === "string" && value.trim() !== "" && Number.isFinite(Number(value))) {
            return Number(value);
        }
        return null;
    }
}
