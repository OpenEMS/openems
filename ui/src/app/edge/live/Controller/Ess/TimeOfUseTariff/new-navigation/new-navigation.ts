import { CommonModule } from "@angular/common";
import { Component, inject, Input, Type } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { MetaComponent } from "src/app/shared/components/edge/config-components/meta/meta";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, Currency, CurrentData, Edge, EdgeConfig, Utils } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../../livedataservice";
import { SharedControllerEssTimeOfUseTariff } from "../shared/shared";
import { SchedulePowerAndSocChartComponent } from "./power-soc-chart";
import { ScheduleStateAndPriceChartComponent } from "./state-price-chart";

@Component({
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    imports: [
        CommonModule,
        IonicModule,
        ReactiveFormsModule,
        FormlyModule,
        TranslateModule,
    ],
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
})
export class ControllerEssTimeOfUseTariffHomeComponent extends AbstractFormlyComponent {

    @Input() public component: EdgeConfig.Component | null = null;
    @Input() public edge: Edge | null = null;

    protected readonly CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC = Utils.CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC(this.translate);
    protected readonly CONVERT_TIME_OF_USE_TARIFF_STATE = Utils.CONVERT_TIME_OF_USE_TARIFF_STATE(this.translate);

    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private routeService: RouteService = inject(RouteService);

    public static generateView(
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
        powerAndSocChartComponent: Type<SchedulePowerAndSocChartComponent>,
        stateAndPriceChartComponent: Type<ScheduleStateAndPriceChartComponent>
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
            }
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
            }
        );

        if (edge.roleIsAtLeast(Role.ADMIN)) {
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
                });
        }

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
        const stateAndPriceChartComponent = ScheduleStateAndPriceChartComponent;
        return ControllerEssTimeOfUseTariffHomeComponent.generateView(this.translate, this.component, this.edge, powerAndSocChartComponent, stateAndPriceChartComponent);
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        return SharedControllerEssTimeOfUseTariff.getChannelAddresses(this.service, this.routeService, this.component);
    }
}
