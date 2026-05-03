import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Name } from "src/app/shared/components/shared/name";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, Edge, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../livedataservice";
import { ProductionChartComponent } from "./production-chart-component";

@Component({
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
})
export class CommonProductionHomeComponent extends AbstractFormlyComponent {

    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    protected productionMeterComponents: EdgeConfig.Component[] = [];
    protected chargerComponents: EdgeConfig.Component[] = [];

    public static getFormlyGeneralView(translate: TranslateService, productionMeterComponents: EdgeConfig.Component[], chargerComponents: EdgeConfig.Component[], edge: Edge, timeOfUseComponent: EdgeConfig.Component | null): OeFormlyView {

        const lines: OeFormlyField[] = [];

        if (timeOfUseComponent != null) {
            lines.push(
                {
                    type: "horizontal-line",
                },
                {
                    type: "component-line",
                    component: ProductionChartComponent,
                    inputs: {
                        component: timeOfUseComponent,
                        edge: edge,
                        refresh: false,
                    },
                },
                {
                    type: "horizontal-line",
                }
            );
        }

        for (const meter of productionMeterComponents) {
            lines.push({
                type: "channel-line",
                name: Name.METER_ALIAS_OR_ID(meter),
                channel: new ChannelAddress(meter.id, "ActivePower").toString(),
                converter: Converter.POWER_IN_KILO_WATT,
            });
        }
        for (const charger of chargerComponents) {
            lines.push({
                type: "channel-line",
                name: Name.METER_ALIAS_OR_ID(charger),
                channel: new ChannelAddress(charger.id, "ActualPower").toString(),
                converter: Converter.POWER_IN_KILO_WATT,
            });
        }

        return {
            title: translate.instant("GENERAL.PRODUCTION"),
            helpKey: "REDIRECT.COMMON_PRODUCTION",
            lines: lines,
            component: new EdgeConfig.Component(),
            useDefaultPrefix: false,
            isCommonWidget: true,
        };
    }

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);
        const componentIds = config.getComponentIdsByFactory("Controller.Ess.Time-Of-Use-Tariff");
        const firstId = componentIds?.[0];
        const timeOfUseComponent = firstId != null
            ? config.getComponentSafely(firstId)
            : null;
        // Get Chargers
        this.chargerComponents =
            config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
                .filter(component => component.isEnabled);

        // Get productionMeters
        this.productionMeterComponents =
            config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
                .filter(component => component.isEnabled && config.isProducer(component));

        return CommonProductionHomeComponent.getFormlyGeneralView(this.translate, this.productionMeterComponents, this.chargerComponents, edge, timeOfUseComponent);
    }
}
