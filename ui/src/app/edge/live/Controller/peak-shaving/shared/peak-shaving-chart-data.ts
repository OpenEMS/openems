import { TranslateService } from "@ngx-translate/core";
import { Phase } from "src/app/shared/components/shared/phase";
import { ChannelAddress, ChartConstants, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { NumberUtils } from "src/app/shared/utils/number/number-utils";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

export type PeakShavingActivePowerMode = "single" | "three-phase";

export interface PeakShavingChartDataOptions {
    activePowerMode: PeakShavingActivePowerMode;
}

export class PeakShavingChartDataBuilder {
    public static build(
        config: EdgeConfig,
        component: EdgeConfig.Component,
        translate: TranslateService,
        options: PeakShavingChartDataOptions,
    ): HistoryUtils.ChartData {
        AssertionUtils.assertIsDefined(component);
        const meterId = config.getPropertyFromComponent<string>(component, "meter.id");

        const input: HistoryUtils.InputChannel[] = [
            ...(meterId == null ? [] : this.buildActivePowerInput(meterId, options.activePowerMode)),
            {
                name: "EssSoc",
                powerChannel: new ChannelAddress("_sum", "EssSoc"),
            },
            {
                name: "Charge",
                powerChannel: new ChannelAddress("_sum", "EssActivePower"),
                converter: HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE,
            },
            {
                name: "Discharge",
                powerChannel: new ChannelAddress("_sum", "EssActivePower"),
                converter: HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO,
            },
            {
                name: "CHARGE_UNDER",
                powerChannel: new ChannelAddress(component.id, "_PropertyRechargePower"),
            },
            {
                name: "DISCHARGE_OVER",
                powerChannel: new ChannelAddress(component.id, "_PropertyPeakShavingPower"),
            },
        ];

        return {
            input: input,
            output: (data: HistoryUtils.ChannelData) => {
                const output: HistoryUtils.DisplayValue[] = [
                    ...this.buildActivePowerOutput(data, translate, options.activePowerMode),
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.PEAKSHAVING.RECHARGE_POWER"),
                        color: ChartConstants.Colors.GREEN,
                        converter: () => data["CHARGE_UNDER"],
                        hideShadow: true,
                        borderDash: [3, 3],
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.PEAKSHAVING.PEAKSHAVING_POWER"),
                        color: ChartConstants.Colors.RED,
                        converter: () => data["DISCHARGE_OVER"],
                        hideShadow: true,
                        borderDash: [3, 3],
                    },
                    {
                        name: translate.instant("GENERAL.CHARGE"),
                        color: ChartConstants.Colors.GREEN,
                        converter: () => data["Charge"],
                    },
                    {
                        name: translate.instant("GENERAL.DISCHARGE"),
                        color: ChartConstants.Colors.RED,
                        converter: () => data["Discharge"],
                    },
                    {
                        name: translate.instant("GENERAL.SOC"),
                        color: ChartConstants.Colors.GREY,
                        converter: () => data["EssSoc"]?.map((value) => NumberUtils.multiplySafely(value, 1000)),
                        yAxisId: ChartAxis.RIGHT,
                        hideShadow: true,
                        borderDash: [10, 10],
                    },
                ];

                return output;
            },
            tooltip: {
                formatNumber: ChartConstants.NumberFormat.ZERO_TO_TWO,
            },
            yAxes: [
                {
                    unit: YAxisType.ENERGY,
                    position: "left",
                    yAxisId: ChartAxis.LEFT,
                },
                {
                    unit: YAxisType.PERCENTAGE,
                    position: "right",
                    yAxisId: ChartAxis.RIGHT,
                },
            ],
        };
    }

    private static buildActivePowerInput(
        meterId: string,
        mode: PeakShavingActivePowerMode,
    ): HistoryUtils.InputChannel[] {
        if (mode === PowerMode.THREE_PHASE) {
            return Phase.THREE_PHASE.map((phase) => ({
                name: "ActivePower" + phase,
                powerChannel: new ChannelAddress(meterId, "ActivePower" + phase),
                converter: HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO,
            }));
        }

        return [
            {
                name: "ActivePower",
                powerChannel: new ChannelAddress(meterId, "ActivePower"),
                converter: HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO,
            },
        ];
    }

    private static buildActivePowerOutput(
        data: HistoryUtils.ChannelData,
        translate: TranslateService,
        mode: PeakShavingActivePowerMode,
    ): HistoryUtils.DisplayValue[] {
        if (mode === PowerMode.THREE_PHASE) {
            return Phase.THREE_PHASE.map((phase, i) => ({
                name: translate.instant("GENERAL.PHASE") + " " + phase,
                color: ChartConstants.Colors.DEFAULT_PHASES_COLORS[i],
                converter: () => data["ActivePower" + phase],
            }));
        }

        return [
            {
                name: translate.instant("GENERAL.GRID_BUY_ADVANCED"),
                color: ChartConstants.Colors.BLUE_GREY,
                converter: () => data["ActivePower"],
            },
        ];
    }
}

export enum PowerMode {
    THREE_PHASE = "three-phase",
    SINGLE = "single",
}
