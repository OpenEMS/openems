import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress } from "src/app/shared/shared";
import { ArrayUtils } from "src/app/shared/utils/array/array.utils";
import { StringUtils } from "src/app/shared/utils/string/string.utils";
import { HistoryUtils } from "src/app/shared/utils/utils";
import { Edge } from "../../edge";
import { EdgeConfig } from "../../edgeconfig";

export class EvcsComponent extends EdgeConfig.Component {
    private constructor(
        id: string,
        alias: string,
        public readonly powerChannel: ChannelAddress,
        public readonly energyChannel: ChannelAddress,
    ) {
        super(id, alias);
    }

    /**
     * Checks if the provided evcsComponent has the deprecate evcs nature.
     *
     * @param component - The component for which to determine the power channel ID.
     * @param config - The EdgeConfig.
     * @param edge - The edge instance
     * @returns - Returns true, if the component is not deprecated, else false.
     */
    public static isDeprecated(component: EdgeConfig.Component, config: EdgeConfig, edge: Edge | null): boolean {
        if (
            edge != null &&
            component != null &&
            config != null &&
            config.hasComponentNature("io.openems.edge.evcs.api.DeprecatedEvcs", component.id) == false
        ) {
            return false;
        }
        return true;
    }

    public static getComponents(config: EdgeConfig, edge: Edge | null): EvcsComponent[] {
        return ArrayUtils.sanitize(
            config
                .getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
                .filter((component) =>
                    StringUtils.isNotInArr(component.factoryId, [
                        "Evcs.Cluster",
                        "Evcs.Cluster.PeakShaving",
                        "Evcs.Cluster.SelfConsumption",
                    ]),
                )
                .map((component) => EvcsComponent.from(component, config, edge)),
        );
    }

    public static from(component: EdgeConfig.Component, config: EdgeConfig | null, edge: Edge | null) {
        if (config === null) {
            return null;
        }
        const isDeprecated = EvcsComponent.isDeprecated(component, config, edge);
        const powerChannelId = isDeprecated ? "ChargePower" : "ActivePower";
        const energyChannelId = isDeprecated ? "ActiveConsumptionEnergy" : "ActiveProductionEnergy";
        return new EvcsComponent(
            component.id,
            component.alias,
            new ChannelAddress(component.id, powerChannelId),
            new ChannelAddress(component.id, energyChannelId),
        );
    }

    public getChartInputChannel(): HistoryUtils.InputChannel {
        return {
            name: this.powerChannel.toString(),
            powerChannel: this.powerChannel,
            energyChannel: this.energyChannel,
        };
    }

    public getChartDisplayValue(
        data: HistoryUtils.ChannelData,
        color: string,
        rest?: HistoryUtils.DisplayValue<HistoryUtils.PluginCustomOptions>,
    ): HistoryUtils.DisplayValue<HistoryUtils.PluginCustomOptions> {
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
