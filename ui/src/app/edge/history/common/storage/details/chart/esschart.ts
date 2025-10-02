import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { Phase } from "src/app/shared/components/shared/phase";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { TRequiredBy } from "src/app/shared/type/utility";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";
import { ChannelAddress, ChartConstants, EdgeConfig } from "../../../../../../shared/shared";

@Component({
    selector: "common-storage-details-ess-chart",
    templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
    standalone: true,
    imports: [
        BaseChartDirective,
        ReactiveFormsModule,
        CommonModule,
        IonicModule,
        TranslateModule,
        ChartComponentsModule,
        HistoryDataErrorModule,
        NgxSpinnerModule,
    ],
})
export class StorageEssChartComponent extends AbstractHistoryChart {

    public static getChartData(translate: TranslateService, essComponent: EdgeConfig.Component, chartType: "line" | "bar", config: EdgeConfig): HistoryUtils.ChartData {
        AssertionUtils.assertIsDefined(essComponent);

        const isHybridEss = config?.hasComponentNature("io.openems.edge.ess.api.HybridEss", essComponent.id);
        const powerEnergyChannels: HistoryUtils.InputChannel[] =
            StorageEssChartComponent.getChargeDischargeInputChannel(isHybridEss, essComponent.id, chartType, config, translate);
        const yAxes: HistoryUtils.yAxes[] = [{
            unit: YAxisType.ENERGY,
            position: "left",
            yAxisId: ChartAxis.LEFT,
        }];

        if (chartType === "line") {
            yAxes.push({
                unit: YAxisType.PERCENTAGE,
                position: "right",
                yAxisId: ChartAxis.RIGHT,
            });
        }

        const input: HistoryUtils.InputChannel[] = [
            ...powerEnergyChannels,
            {
                name: "Soc",
                powerChannel: ChannelAddress.fromString(essComponent.id + "/Soc"),
            },
        ];

        const emergencyReserveComponent: EdgeConfig.Component | null = config
            .getComponentsByFactory("Controller.Ess.EmergencyCapacityReserve")
            .filter(component =>
                component != null && component.isEnabled && essComponent.hasPropertyValue("ess.id", essComponent.id))[0] ?? null;
        const isReserveSocEnabled = config.getPropertyFromComponent<boolean>(emergencyReserveComponent, "isReserveSocEnabled");
        if (emergencyReserveComponent && isReserveSocEnabled) {
            input.push({
                name: "EmergencyReserve",
                powerChannel: new ChannelAddress(emergencyReserveComponent.id, "ActualReserveSoc"),
            });
        }

        return {
            input: input,
            output: (data: HistoryUtils.ChannelData) => {

                const output: HistoryUtils.DisplayValue[] = [{
                    name: translate.instant("General.CHARGE"),
                    ...StorageEssChartComponent.getChargeDisplayValues(essComponent.id, data, isHybridEss, chartType),
                    stack: 0,
                    color: ChartConstants.Colors.GREEN,
                },
                {
                    name: translate.instant("General.DISCHARGE"),
                    ...StorageEssChartComponent.getDischargeDisplayValues(essComponent.id, data, isHybridEss),
                    stack: 1,
                    color: ChartConstants.Colors.RED,
                }];

                if (config.hasComponentNature("io.openems.edge.ess.api.AsymmetricEss", essComponent.id)) {
                    output.push(...Phase.THREE_PHASE.map((phase, i) => ({
                        name: translate.instant("General.phase") + " " + phase,
                        converter: () => data[essComponent.id + "/ActivePower" + phase],
                        stack: 1,
                        color: ChartConstants.Colors.DEFAULT_PHASES_COLORS[i],
                    })));
                }

                if (chartType === "line") {
                    output.push({
                        name: translate.instant("General.soc"),
                        converter: () => data["Soc"].map(el => Utils.multiplySafely(el, 1000)),
                        color: ChartConstants.Colors.GREY,
                        borderDash: [10, 10],
                        yAxisId: ChartAxis.RIGHT,
                    });

                }
                if (emergencyReserveComponent && isReserveSocEnabled) {
                    output.push({
                        name: translate.instant("Edge.Index.EmergencyReserve.EMERGENCY_RESERVE"),
                        converter: () => data["EmergencyReserve"].map(el => Utils.multiplySafely(el, 1000)),
                        color: ChartConstants.Colors.BLACK,
                        yAxisId: ChartAxis.RIGHT,
                        borderDash: [3, 3],
                    });
                }
                return output;
            },
            tooltip: {
                formatNumber: ChartConstants.NumberFormat.ZERO_TO_TWO,
            },
            yAxes: yAxes,
        };
    }

    private static getChargeDischargeInputChannel(isHybridEss: boolean, componentId: string, chartType: "bar" | "line", config: EdgeConfig, translate: TranslateService): HistoryUtils.InputChannel[] {

        const inputChannel: HistoryUtils.InputChannel[] = [{
            name: componentId + "Charge",
            ...StorageEssChartComponent.getChargeInputChannels(isHybridEss, componentId, chartType),
        },
        {
            name: componentId + "Discharge",
            ...StorageEssChartComponent.getDischargeInputChannels(isHybridEss, componentId),
            ...(chartType === "line" && { converter: HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO }),
        }];

        if (config.hasComponentNature("io.openems.edge.ess.api.AsymmetricEss", componentId)) {
            inputChannel.push(...Phase.THREE_PHASE.map(phase => ({
                name: translate.instant("General.phase") + " " + phase,
                powerChannel: new ChannelAddress(componentId, "ActivePower" + phase),
                ...(chartType === "line" && { converter: HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE }),
            })));
        }

        return inputChannel;
    }

    private static getChargeInputChannels(isHybridEss: boolean, componentId: string, chartType: "bar" | "line"): RequiredChannels {
        return {
            energyChannel: new ChannelAddress(componentId, isHybridEss ? "DcChargeEnergy" : "ActiveChargeEnergy"),
            powerChannel: new ChannelAddress(componentId, isHybridEss ? "DcDischargePower" : "ActivePower"),
            ...(chartType === "line" ? { converter: HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE } : {}),
        };
    }

    private static getDischargeInputChannels(isHybridEss: boolean, componentId: string): RequiredChannels {
        return {
            energyChannel: new ChannelAddress(componentId, isHybridEss ? "DcDischargeEnergy" : "ActiveDischargeEnergy"),
            powerChannel: new ChannelAddress(componentId, isHybridEss ? "DcDischargePower" : "ActivePower"),
        };
    }

    private static getChargeDisplayValues(essComponentId: string, data: HistoryUtils.ChannelData, isHybridEss: boolean, chartType: "bar" | "line"): StrippedDisplayValue {
        const chargeChannels = StorageEssChartComponent.getChargeInputChannels(isHybridEss, essComponentId, chartType);
        return {
            converter: () => data[essComponentId + "Charge"],
            nameSuffix: (energyResponse: QueryHistoricTimeseriesEnergyResponse) => energyResponse.result.data[chargeChannels.energyChannel.toString()],
        };
    }

    private static getDischargeDisplayValues(essComponentId: string, data: HistoryUtils.ChannelData, isHybridEss: boolean): StrippedDisplayValue {
        const dischargeChannels = StorageEssChartComponent.getDischargeInputChannels(isHybridEss, essComponentId);
        return {
            converter: () => data[essComponentId + "Discharge"],
            nameSuffix: (energyResponse: QueryHistoricTimeseriesEnergyResponse) => energyResponse.result.data[dischargeChannels.energyChannel.toString()],
        };
    }

    public override getChartData() {
        const component = this.config.getComponent(this.route.snapshot.params.componentId);
        return StorageEssChartComponent.getChartData(this.translate, component, this.chartType, this.config);
    }
}

type RequiredChannels = TRequiredBy<Pick<HistoryUtils.InputChannel, "powerChannel" | "energyChannel">, "energyChannel" | "powerChannel"> & Pick<HistoryUtils.InputChannel, "converter">;
type StrippedDisplayValue = Pick<HistoryUtils.DisplayValue, "converter" | "nameSuffix">;
