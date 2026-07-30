import { Component, ChangeDetectionStrategy } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { EnergySchedulerV2 } from "src/app/shared/components/edge/config-components/energy/energy";
import { GetSchedule } from "src/app/shared/components/edge/config-components/energy/getSchedule";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, ChartConstants, Currency, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { TimeLineChartComponent } from "../../../../../shared/components/chart/timeline-chart/timeline-chart";
import { LiveDataService } from "../../../livedataservice";
import { SharedGrid } from "../shared/shared";
import { GridBuySellChartComponent } from "./chart/buy-sell-chart";
import { GridBuyPriceChartComponent } from "./chart/price-buy-price-chart";
import { GridSellPriceChartComponent } from "./chart/price-sell-price-chart";

@Component({
    selector: "oe-common-grid",
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class CommonGridHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    public static async getFormlyGeneralView(translate: TranslateService, service: Service, edge: Edge, config: EdgeConfig, energyScheduler: EnergySchedulerV2): Promise<OeFormlyView> {
        await energyScheduler?.updateSchedule(edge, service.websocket);

        const meta = config.getComponentSafely("_meta");
        const currency = config.getPropertyFromComponent<string>(meta, "currency");
        const currencyLabel: Currency.Label = Currency.getCurrencyLabelByCurrency(currency);

        const view = SharedGrid.getFormlyView(config, edge.role, translate);
        if (energyScheduler.schedule !== GetSchedule.Response.empty) {
            const lines: OeFormlyField<unknown>[] = [];
            lines.push(
                {
                    type: "component-line",
                    component: TimeLineChartComponent,
                    inputs: {
                        data: energyScheduler.schedule,
                    },
                },
                {
                    type: "horizontal-line",
                },
                {
                    type: "channel-line",
                    name: translate.instant("GENERAL.POWER"),
                    channel: new ChannelAddress("_sum", "GridActivePower").toString(),
                    converter: GRID_BUY_OR_SELL(translate),
                    style: {
                        name: { fontSize: "large" },
                        value: { fontSize: "large" },
                    },
                    cssClass: "ion-padding-top",
                },
                {
                    type: "component-line",
                    component: GridBuySellChartComponent,
                    inputs: {
                        edge: edge,
                        refresh: false,
                        data: energyScheduler.schedule,
                    },
                },
            );

            if (energyScheduler.schedule.hasDataForChannel("GridBuyPrice")) {
                lines.push(
                    {
                        type: "channel-line",
                        name: translate.instant("GENERAL.GRID_BUY_PRICE"),
                        channel: new ChannelAddress("_sum", "GridBuyPrice").toString(),
                        converter: Converter.CURRENCY_PER_MWH_TO_KWH(currencyLabel),
                        style: {
                            name: { fontSize: "large" },
                            value: { fontSize: "large" },
                        },
                        cssClass: "ion-padding-top",
                    },
                    {
                        type: "component-line",
                        component: GridBuyPriceChartComponent,
                        inputs: {
                            edge: edge,
                            refresh: false,
                            data: energyScheduler.schedule,
                        },
                    },
                );
            }

            if (energyScheduler.schedule.hasDataForChannel("GridSellPrice")) {
                lines.push(
                    {
                        type: "channel-line",
                        name: translate.instant("GENERAL.GRID_SELL_PRICE"),
                        channel: new ChannelAddress("_sum", "GridSellPrice").toString(),
                        converter: Converter.CURRENCY_PER_MWH_TO_KWH(currencyLabel),
                        style: {
                            name: { fontSize: "large" },
                            value: { fontSize: "large" },
                        },
                        cssClass: "ion-padding-top",
                    },
                    {
                        type: "component-line",
                        component: GridSellPriceChartComponent,
                        inputs: {
                            edge: edge,
                            refresh: false,
                            data: energyScheduler.schedule,
                        },
                    },
                );
            }

            lines.push(
                {
                    type: "horizontal-line",
                },
                {
                    type: "name-line",
                    name: translate.instant("GENERAL.DETAILS"),
                    style: {
                        name: { fontSize: "large" },
                    },
                    cssClass: "ion-padding-top",
                },
            );

            view.lines.unshift(...lines);
        }

        return view;
    }

    protected override async generateView(): Promise<OeFormlyView> {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const energy = new EnergySchedulerV2(config);

        return await CommonGridHomeComponent.getFormlyGeneralView(this.translate, this.service, edge, config, energy);
    }
}

export const GRID_BUY_OR_SELL =
    (translate: TranslateService): Converter =>
    (raw): string => {
        const displayText = (power: string, color: string, text: string): string =>
            `<span>${power}&nbsp;<ion-label style="color:${color}">${text}</ion-label></span>`;

        return Converter.IF_NUMBER(raw, (value) => {
            if (value > 0) {
                return displayText(
                    Converter.POWER_IN_KILO_WATT(value),
                    ChartConstants.Colors.BLUE_GREY,
                    translate.instant("GENERAL.GRID_BUY_ADVANCED"),
                );
            } else if (value < 0) {
                return displayText(
                    Converter.POWER_IN_KILO_WATT(Math.abs(value)),
                    ChartConstants.Colors.PURPLE,
                    translate.instant("GENERAL.GRID_SELL_ADVANCED"),
                );
            } else {
                return Converter.POWER_IN_KILO_WATT(value);
            }
        });
    };
