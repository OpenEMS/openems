import { ChangeDetectionStrategy, Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { ModeDataset, ScheduleModeChartComponent, } from "src/app/shared/components/chart/schedule-mode-chart/schedule-mode-chart";
import { GetSchedule } from "src/app/shared/components/edge/config-components/energy/getSchedule";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { ChartConstants } from "src/app/shared/shared";
import { TimeOfUseTariffUtils } from "src/app/shared/utils/utils";

type ModeDefinition = Omit<ModeDataset, "data"> & {
    matches: (value: number | null) => boolean;
};

@Component({
    selector: "oe-common-storage-mode-chart",
    templateUrl: "../../../../../history/abstracthistorychart.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [BaseChartDirective, HistoryDataErrorModule, NgxSpinnerModule],
})
export class ModeChartComponent extends ScheduleModeChartComponent {
    public static getDatasets(schedule: GetSchedule.Response, translate: TranslateService): ModeDataset[] {
        const definitions: ModeDefinition[] = [
            {
                color: ChartConstants.Colors.ESS_MODE_DELAY_DISCHARGE,
                label: translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DELAY_DISCHARGE"),
                matches: (value) => value === TimeOfUseTariffUtils.State.DelayDischarge,
            },
            {
                color: ChartConstants.Colors.ESS_MODE_BALANCING,
                label: translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.BALANCING"),
                matches: (value) => value === TimeOfUseTariffUtils.State.Balancing,
            },
            {
                color: ChartConstants.Colors.ESS_MODE_CHARGE_GRID,
                label: translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.CHARGE_GRID"),
                matches: (value) => value === TimeOfUseTariffUtils.State.ChargeGrid,
            },
            {
                color: ChartConstants.Colors.ESS_MODE_DISCHARGE_GRID,
                label: translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DISCHARGE_GRID"),
                matches: (value) => value === TimeOfUseTariffUtils.State.DischargeGrid,
            },
            {
                color: ChartConstants.Colors.ESS_MODE_PEAK_SHAVING,
                label: translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.PEAK_SHAVING"),
                matches: (value) => value === TimeOfUseTariffUtils.State.PeakShaving,
            },
            {
                color: ChartConstants.Colors.ESS_MODE_DELAY_CHARGE,
                label: translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DELAY_CHARGE"),
                matches: (value) => value === TimeOfUseTariffUtils.State.DelayCharge,
            },
            {
                color: ChartConstants.Colors.ESS_MODE_LIMIT_CHARGE,
                label: translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.LIMIT_CHARGE"),
                matches: (value) => value === TimeOfUseTariffUtils.State.LimitCharge,
            },
            {
                color: ChartConstants.Colors.ESS_MODE_AVOID_FEED_IN_LIMIT,
                label: translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.AVOID_GRID_SELL_LIMIT"),
                matches: (value) => value === TimeOfUseTariffUtils.State.AvoidGridSellLimit,
            },
            {
                color: ChartConstants.Colors.ESS_MODE_DISCHARGE_CONSUMPTION,
                label: translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DISCHARGE_CONSUMPTION"),
                matches: (value) => value === TimeOfUseTariffUtils.State.DischargeConsumption,
            },
        ];

        const values = schedule.data24h.map(
            (entry) => entry.eshs.find((esh) => esh.id === "ctrlEssTimeOfUseTariff0")?.mode ?? null,
        );
        const isUnknownMode = (value: number | null): boolean =>
            value != null && definitions.some((definition) => definition.matches(value) === true) === false;
        const allDefinitions =
            values.some(isUnknownMode) === true
                ? [
                      ...definitions,
                      {
                          color: ChartConstants.Colors.GREY,
                          label: translate.instant("GENERAL.STATE_MACHINE.UNDEFINED"),
                          matches: isUnknownMode,
                      },
                  ]
                : definitions;

        return allDefinitions
            .map<ModeDataset>((definition) => ({
                color: definition.color,
                label: definition.label,
                data: values.map((value) => (definition.matches(value) === true ? 1 : null)),
            }))
            .filter((dataset) => dataset.data.includes(1) === true);
    }

    protected override buildModeDatasets(): ModeDataset[] {
        return ModeChartComponent.getDatasets(this.data, this.translate);
    }
}
