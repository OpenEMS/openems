import { Component, Type } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ScheduleChartComponent } from "src/app/shared/components/chart/schedule-chart/schedule-chart";
import { TimeLineChartComponent } from "src/app/shared/components/chart/timeline-chart/timeline-chart";
import { EnergyScheduler } from "src/app/shared/components/edge/config-components/energy/energy";
import { GetSchedule } from "src/app/shared/components/edge/config-components/energy/getSchedule";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { Edge, EdgeConfig } from "src/app/shared/shared";
import { ConsumptionChartComponent } from "../live/common/consumption/new-navigation/chart/consumption-chart-component";
import { GridBuyChartComponent } from "../live/common/grid/new-navigation/chart/grid-buy-chart";
import { GridSellChartComponent } from "../live/common/grid/new-navigation/chart/grid-sell-chart";
import { GridBuyPriceChartComponent } from "../live/common/grid/new-navigation/chart/price-buy-price-chart";
import { GridSellPriceChartComponent } from "../live/common/grid/new-navigation/chart/price-sell-price-chart";
import { ProductionChartComponent } from "../live/common/production/new-navigation/chart/production-chart-component";
import { ChargeDischargeChartComponent } from "../live/common/storage/new-navigation/chart/charge-discharge-chart";
import { ModeChartComponent } from "../live/common/storage/new-navigation/chart/mode-chart";
import { SocChartComponent } from "../live/common/storage/new-navigation/chart/soc-chart";

@Component({
    selector: "oe-schedule",
    templateUrl: "../../shared/components/formly/formly-field-modal/template.html",
    imports: [CommonUiModule, ReactiveFormsModule, FormlyModule, TranslateModule],
})
export class ScheduleComponent extends AbstractFormlyComponent {
    protected override formlyWrapper:
        "formly-field-modal" | "formly-field-navigation" | "formly-field-waiting-spinner" = "formly-field-navigation";

    protected override async generateView(
        viewContext: Readonly<{ edge: Edge; config: EdgeConfig; translate: TranslateService }>,
    ): Promise<OeFormlyView> {
        const energyScheduler = new EnergyScheduler(viewContext.config);
        await energyScheduler.updateSchedule(
            viewContext.edge,
            this.service.websocket,
            this.service.historyPeriod.value.from,
        );

        const componentLines = <T extends ScheduleChartComponent>(
            component: Type<T>,
            name: string,
            channelName: keyof GetSchedule.Response["result"]["data"][number]["_sum"] | null = null,
        ): OeFormlyField[] => {
            const lines: OeFormlyField[] = [
                {
                    type: "name-line",
                    name: name,
                },
                {
                    type: "component-line",
                    component,
                    inputs: {
                        edge: viewContext.edge,
                        data: energyScheduler.schedule,
                    },
                },
            ];

            if (channelName == null) {
                return lines;
            }

            return energyScheduler.schedule.hasDataForChannel(channelName) ? lines : [];
        };

        const translate = viewContext.translate;
        return {
            lines: [
                {
                    type: "component-line",
                    component: TimeLineChartComponent,
                    inputs: {
                        data: energyScheduler.schedule,
                    },
                    componentWrapperCssClass: "ion-position-sticky ion-background-color",
                },
                ...componentLines(
                    ProductionChartComponent,
                    translate.instant("GENERAL.PRODUCTION"),
                    "ProductionActivePower",
                ).map((el) => {
                    el.cssClass = "ion-padding-top-triple";
                    return el;
                }),
                ...componentLines(
                    ConsumptionChartComponent,
                    translate.instant("GENERAL.CONSUMPTION"),
                    "ConsumptionActivePower",
                ),
                ...componentLines(
                    GridBuyPriceChartComponent,
                    translate.instant("GENERAL.GRID_BUY_PRICE"),
                    "GridBuyPrice",
                ),
                ...componentLines(
                    GridSellPriceChartComponent,
                    translate.instant("GENERAL.GRID_SELL_PRICE"),
                    "GridSellPrice",
                ),
                ...componentLines(GridBuyChartComponent, translate.instant("GENERAL.GRID_BUY"), "GridActivePower"),
                ...componentLines(GridSellChartComponent, translate.instant("GENERAL.GRID_SELL"), "GridActivePower"),

                ...componentLines(
                    ChargeDischargeChartComponent,
                    translate.instant("GENERAL.CHARGE_DISCHARGE"),
                    "EssDischargePower",
                ),
                ...componentLines(SocChartComponent, translate.instant("GENERAL.SOC"), "EssSoc"),
                ...(viewContext.config.getFirstComponentByFactoryId("Controller.Ess.Time-Of-Use-Tariff")
                    ? componentLines(ModeChartComponent, translate.instant("GENERAL.MODE"))
                    : []),
            ],
            title: translate.instant("MENU.SCHEDULE"),
            component: new EdgeConfig.Component(),
        };
    }
}
