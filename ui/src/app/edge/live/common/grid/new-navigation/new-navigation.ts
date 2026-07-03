import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { SingleXAxisComponent } from "src/app/shared/components/chart/single-xaxis/single-xaxis";
import { EnergySchedulerV2 } from "src/app/shared/components/edge/config-components/energy/energy";
import { GetSchedule } from "src/app/shared/components/edge/config-components/energy/getSchedule";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, Currency, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../livedataservice";
import { SharedGrid } from "../shared/shared";
import { GridBuySellChartComponent } from "./chart/buy-sell-chart";
import { GridBuyPriceChartComponent } from "./chart/price-buy-price-chart";

@Component({
    selector: "oe-common-grid",
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
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
            view.lines.unshift(
                {
                    type: "component-line",
                    component: SingleXAxisComponent,
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
                    channel: new ChannelAddress(
                        "_sum",
                        "GridActivePower",
                    ).toString(),
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
                {
                    type: "horizontal-line",
                },
                {
                    type: "channel-line",
                    name: translate.instant("GENERAL.GRID_BUY_PRICE"),
                    channel: new ChannelAddress(
                        "_sum",
                        "GridBuyPrice",
                    ).toString(),
                    converter: Converter.CURRENCY_PER_KWH(currencyLabel),
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

export const GRID_BUY_OR_SELL = (translate: TranslateService): Converter =>
    (raw): string =>
        Converter.IF_NUMBER(raw, (value) => {
            if (value > 0) {
                return Converter.POWER_IN_KILO_WATT(value) + " " + translate.instant("GENERAL.GRID_BUY_ADVANCED");
            } else if (value < 0) {
                return Converter.POWER_IN_KILO_WATT(Math.abs(value)) + " " + translate.instant("GENERAL.GRID_SELL_ADVANCED");
            } else {
                return Converter.POWER_IN_KILO_WATT(value);
            }
        });
