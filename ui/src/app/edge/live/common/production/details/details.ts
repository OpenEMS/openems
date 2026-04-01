import { Component } from "@angular/core";
import { TextIndentation } from "src/app/shared/components/modal/modal-line/modal-line";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { Phase } from "src/app/shared/components/shared/phase";
import { EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../livedataservice";

@Component({
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
})
export class CommonProductionDetailsComponent extends AbstractFormlyComponent {

    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();

        AssertionUtils.assertIsDefined(config);
        // Get Chargers
        const chargerComponents =
            config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
                .filter(component => component.isEnabled);

        // Get productionMeters
        const productionMeters = config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
            .filter(component => component.isEnabled && config.isProducer(component));

        const lines: OeFormlyField[] = [];

        if (productionMeters?.length > 0 && chargerComponents?.length > 0) {
            // Total
            lines.push({
                type: "channel-line",
                name: this.translate.instant("GENERAL.TOTAL"),
                channel: "_sum/ProductionActivePower",
                converter: Converter.POWER_IN_WATT,
            }, { type: "horizontal-line" });
        }

        if (productionMeters?.length === 1) {
            lines.push({ type: "advanced-electricity-meter-line", component: productionMeters[0] }, { type: "horizontal-line" });
        }

        if (productionMeters?.length > 1) {
            lines.push(
                {
                    type: "channel-line", channel: "_sum/ProductionAcActivePower",
                    name: this.translate.instant("GENERAL.TOTAL") + (chargerComponents.length > 0 ? " AC" : ""), converter: Converter.POWER_IN_WATT,
                },
                ...Phase.THREE_PHASE
                    .map(phase => <OeFormlyField>{
                        type: "channel-line",
                        name: this.translate.instant("GENERAL.PHASE") + " " + phase,
                        indentation: TextIndentation.SINGLE,
                        channel: "_sum/ProductionAcActivePower" + phase,
                        converter: Converter.POWER_IN_WATT,
                    }),
                { type: "horizontal-line" },
            );

            for (const meter of productionMeters) {
                lines.push(
                    { type: "advanced-electricity-meter-line", component: meter },
                    { type: "horizontal-line" },
                );
            }
        }

        if (chargerComponents.length > 1) {
            lines.push({
                type: "channel-line",
                channel: "_sum/ProductionDcActualPower",
                name: this.translate.instant("GENERAL.TOTAL") + (productionMeters.length > 0 ? " DC" : ""),
                converter: Converter.POWER_IN_WATT,
            });
        }

        for (const component of chargerComponents) {
            lines.push(
                { type: "horizontal-line" },
                { type: "advanced-ess-charger-line", component: component },
            );
        }
        lines.push({ type: "info-line", name: this.translate.instant("EDGE.INDEX.WIDGETS.PHASES_INFO") });


        return {
            title: this.translate.instant("GENERAL.PRODUCTION"),
            helpKey: "REDIRECT.COMMON_PRODUCTION",
            lines: lines,
            component: new EdgeConfig.Component(),
            isCommonWidget: "true",
        };
    }
}
