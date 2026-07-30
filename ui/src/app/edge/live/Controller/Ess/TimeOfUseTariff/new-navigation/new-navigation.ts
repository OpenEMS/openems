import { CommonModule } from "@angular/common";
import { Component, inject, Input, Type, ChangeDetectionStrategy } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { MetaComponent } from "src/app/shared/components/edge/config-components/meta/meta";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, Currency, CurrentData, Edge, EdgeConfig, Utils } from "src/app/shared/shared";
import { Mode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../../livedataservice";
import { SharedGridOptimizedCharge } from "../../GridOptimizedCharge/shared/shared";
import { SharedControllerEssTimeOfUseTariff } from "../shared/shared";
import { ScheduleGridSellChartComponent } from "./grid-sell-chart";
import { SchedulePowerAndSocChartComponent } from "./power-soc-chart";
import { ScheduleStateAndPriceChartComponent } from "./state-price-chart";

@Component({
    selector: "oe-controller-ess-time-of-use-tariff-home",
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class ControllerEssTimeOfUseTariffHomeComponent extends AbstractFormlyComponent {
    @Input() public component: EdgeConfig.Component | null = null;
    @Input() public edge: Edge | null = null;

    protected readonly CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC = Utils.CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC(
        this.translate,
    );
    protected readonly CONVERT_TIME_OF_USE_TARIFF_STATE = Utils.CONVERT_TIME_OF_USE_TARIFF_STATE(this.translate);

    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private routeService: RouteService = inject(RouteService);

    public static generateView(
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
        powerAndSocChartComponent: Type<SchedulePowerAndSocChartComponent>,
        gridSellChartComponent: Type<ScheduleGridSellChartComponent>,
        stateAndPriceChartComponent: Type<ScheduleStateAndPriceChartComponent>,
        displayEeg2025: boolean,
    ): OeFormlyView {
        const lines: OeFormlyField[] = [];

        lines.push(
            {
                type: "channel-line",
                name: translate.instant("GENERAL.MODE"),
                channel: component.id + "/_PropertyMode",
                converter: Utils.CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC(translate),
            },
            {
                type: "value-from-channels-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.PRICE"),
                channelsToSubscribe: [new ChannelAddress(component.id, "QuarterlyPrices")],
                value: (currentData: CurrentData) => {
                    const config = edge.getCurrentConfig();

                    const quarterlyPrice = currentData.allComponents[component.id + "/QuarterlyPrices"];
                    const meta = new MetaComponent(config);
                    if (meta == null) {
                        return "-";
                    }
                    const currency = meta.getCurrency();
                    if (typeof currency !== "string") {
                        return "-";
                    }
                    const currencyLabel: Currency.Label = Currency.getCurrencyLabelByCurrency(currency);
                    return Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(quarterlyPrice);
                },
            },
            {
                type: "channel-line",
                name: translate.instant("GENERAL.STATUS"),
                channel: component.id + "/StateMachine",
                converter: Utils.CONVERT_TIME_OF_USE_TARIFF_STATE(translate),
            },
        );

        lines.push(
            {
                type: "horizontal-line",
            },
            {
                type: "info-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.CHART_TITLE"),
            },
            {
                type: "component-line",
                component: stateAndPriceChartComponent,
                inputs: {
                    component: component,
                    edge: edge,
                    refresh: false,
                },
            },
            {
                type: "info-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.CHART_WARNING_NOTE"),
            },
        );

        if (displayEeg2025 == true) {
            lines.push(
                {
                    type: "horizontal-line",
                },
                {
                    type: "info-line",
                    name: translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.EEG_2025_HEADER"),
                },
                {
                    type: "component-line",
                    component: gridSellChartComponent,
                    inputs: {
                        component: component,
                        edge: edge,
                        refresh: false,
                    },
                },
                {
                    type: "info-line",
                    name: translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.EEG_2025_DESCRIPTION"),
                },
            );
        }

        lines.push(
            {
                type: "horizontal-line",
            },
            {
                type: "info-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.POWER_SOC_CHART_TITLE"),
            },
            {
                type: "component-line",
                component: powerAndSocChartComponent,
                inputs: {
                    component: component,
                    edge: edge,
                    refresh: false,
                },
            },
            {
                type: "horizontal-line",
            },
        );

        return {
            title: component.alias,
            helpKey: "REDIRECT.CONTROLLER_ESS_TIME_OF_USE_TARIFF",
            icon: { name: "oe-time-of-use", color: "normal", size: "large" },
            lines: lines,
            component: new EdgeConfig.Component(),
        };
    }

    protected override generateView(): OeFormlyView {
        if (this.edge == null) {
            this.edge = this.service.currentEdge();
        }
        AssertionUtils.assertIsDefined(this.edge);

        if (this.component == null) {
            const config = this.edge.getCurrentConfig();

            AssertionUtils.assertIsDefined(config);
            this.component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        }

        AssertionUtils.assertIsDefined(this.component);

        const powerAndSocChartComponent = SchedulePowerAndSocChartComponent;
        const gridSellChartComponent = ScheduleGridSellChartComponent;
        const stateAndPriceChartComponent = ScheduleStateAndPriceChartComponent;
        return ControllerEssTimeOfUseTariffHomeComponent.generateView(
            this.translate,
            this.component,
            this.edge,
            powerAndSocChartComponent,
            gridSellChartComponent,
            stateAndPriceChartComponent,
            this.displayEeg2025(),
        );
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        return SharedControllerEssTimeOfUseTariff.getChannelAddresses(this.service, this.routeService, this.component);
    }

    private displayEeg2025(): boolean {
        const config = this.edge?.getCurrentConfig();

        if (config == null || this.component == null) {
            return false;
        }

        if (
            SharedGridOptimizedCharge.isEnergySchedulerV2Enabled(config) == false ||
            SharedGridOptimizedCharge.isEeg2025Installed(config) == false
        ) {
            return false;
        }

        const essId = this.component.getPropertyFromComponent<string>("ess.id");

        return config.getComponentIdsByFactory("Controller.Ess.GridOptimizedCharge").some((controllerId) => {
            const controller = config.getComponentSafely(controllerId);

            return (
                controller != null &&
                controller.getPropertyFromComponent<string>("ess.id") === essId &&
                controller.getPropertyFromComponent<string>("mode") !== Mode.OFF
            );
        });
    }
}
