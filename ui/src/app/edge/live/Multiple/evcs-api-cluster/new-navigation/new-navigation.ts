import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

@Component({
    selector: "oe-evcs-cluster-home",
    standalone: true,
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class EvcsApiClusterHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const singletonClusterId =
            config.getFirstComponentByFactoryId("Evcs.Cluster.SelfConsumption")?.id ??
            config.getFirstComponentByFactoryId("Evcs.Cluster.PeakShaving")?.id ??
            null;

        const clusterComponent = config.getComponentSafely(singletonClusterId);
        AssertionUtils.assertIsDefined(clusterComponent);

        const lines: OeFormlyField[] = [
            {
                type: "value-line",
                name: this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.AMOUNT_OF_CHARGING_STATIONS"),
                value: "0",
                converter: Converter.TO_STRING,
            },
        ];

        return {
            title: clusterComponent.alias ?? this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.CHARGING_STATION_CLUSTER"),
            icon: { name: "oe-evcs", color: "normal", size: "large" },
            lines: lines,
            component: clusterComponent,
            edge: edge,
        };
    }

    private getEvcsIds(component: EdgeConfig.Component): string[] {
        const raw = component.properties["evcs.ids"];
        if (Array.isArray(raw)) {
            return raw.map((id) => (typeof id === "string" ? id.trim() : "")).filter((id) => id.length > 0);
        }

        if (typeof raw === "string") {
            return raw
                .split(",")
                .map((id) => id.trim())
                .filter((id) => id.length > 0);
        }

        return [];
    }
}
