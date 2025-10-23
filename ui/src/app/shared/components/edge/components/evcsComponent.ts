import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress } from "src/app/shared/shared";
import { HistoryUtils } from "src/app/shared/utils/utils";
import { Edge } from "../edge";
import { EdgeConfig } from "../edgeconfig";

export class EvcsComponent extends EdgeConfig.Component {

    private constructor(
        id: string,
        alias: string,
        public readonly powerChannel: ChannelAddress,
        public readonly energyChannel: ChannelAddress
    ) {
        super(id, alias);
    }

    /**
     * Retrieves the appropriate power channel ID for an Electric Vehicle Charging Station (EVCS) component.
     *
     * The method returns "ActivePower", unless the given `edge` object does not meet the minimum
     * required version or the component implements DeprecatedEvcs, in which case it returns "ChargePower".
     *
     * @param component - The component for which to determine the power channel ID.
     * @param config - The EdgeConfig.
     * @param edge - The edge instance
     * @returns - Returns "ActivePower" if the edge version is at least "2024.10.2" and
     * the component is not deprecated. Otherwise, returns "ChargePower".
     */
    public static isDeprecated(component: EdgeConfig.Component, config: EdgeConfig, edge: Edge | null): boolean {
        if (edge != null && component != null && config != null && (config.hasComponentNature("io.openems.edge.evcs.api.DeprecatedEvcs", component.id) == false && edge.isVersionAtLeast("2024.10.2"))) {
            return false;
        }
        return true;
    }

    public static getComponents(config: EdgeConfig, edge: Edge | null): EvcsComponent[] {
        return config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
            .filter(component =>
                !["Evcs.Cluster", "Evcs.Cluster.PeakShaving", "Evcs.Cluster.SelfConsumption"]
                    .includes(component.factoryId))
            .map(component => EvcsComponent.from(component, config, edge));
    }

    public static from(component: EdgeConfig.Component, config: EdgeConfig, edge: Edge | null) {
        const powerChannelId = EvcsComponent.isDeprecated(component, config, edge) ? "ChargePower" : "ActivePower";
        const energyChannelId = EvcsComponent.isDeprecated(component, config, edge) ? "ActiveConsumptionEnergy" : "ActiveProductionEnergy";
        return new EvcsComponent(component.id, component.alias, new ChannelAddress(component.id, powerChannelId), new ChannelAddress(component.id, energyChannelId));
    }

    public getChartInputChannel(): HistoryUtils.InputChannel {
        console.log(this.powerChannel);
        console.log(this.energyChannel);
        return {
            name: this.powerChannel.toString(),
            powerChannel: this.powerChannel,
            energyChannel: this.energyChannel,
        };
    }

    public getChartDisplayValue(data: HistoryUtils.ChannelData, color: string, rest?: HistoryUtils.DisplayValue<HistoryUtils.PluginCustomOptions>): HistoryUtils.DisplayValue<HistoryUtils.PluginCustomOptions> {
        return {
            name: this.alias,
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
                return energyValues?.result.data[this.energyChannel.toString()];
            },
            converter: () => {
                return data[this.powerChannel.toString()] ?? null;
            },
            color: color,
            ...rest,
        };
    }
}
