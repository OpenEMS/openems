import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Name } from "src/app/shared/components/shared/name";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { ChartComponent } from "../modal/chart/chart";

@Component({
    selector: "oe-evcs-cluster-home",
    standalone: true,
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class EvcsApiClusterHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";
    private evcss = new Map<string, EdgeConfig.Component>();
    private evcsIds: string[] = [];

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

        this.evcsIds = this.getEvcsIds(clusterComponent);
        const numberOfEvcs = this.evcsIds.length;

        this.evcsIds.forEach((evcsId) => {
            const component = config.getComponent(evcsId);
            if (component == null) {
                return;
            }
            this.evcss.set(evcsId, component);
        });

        const lines: OeFormlyField[] = [
            {
                type: "component-line",
                component: ChartComponent,
                inputs: {
                    evcss: Object.fromEntries(this.evcss),
                    component: clusterComponent,
                    customChartHeightPercentage: 40,
                },
            },
            {
                type: "value-line",
                name: this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.AMOUNT_OF_CHARGING_STATIONS"),
                value: numberOfEvcs.toString(),
                converter: Converter.TO_STRING,
            },
            {
                type: "reorder-group-line",
                controlName: "evcs_ids",
                items: this.evcsIds.map((evcsId) => {
                    const evcs = config.getComponent(evcsId);
                    return {
                        value: evcsId,
                        label: evcs ? Name.METER_ALIAS_OR_ID(evcs) : evcsId,
                    };
                }),
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

    protected override getFormGroup(): FormGroup {
        return new FormGroup({
            evcs_ids: new FormControl(this.evcsIds, { nonNullable: true }),
        });
    }

    protected override buildUpdateComponentArr(fg: FormGroup<any>): { name: string; value: any }[] {
        return (
            super
                .buildUpdateComponentArr(fg)
                // "evcs.ids" throws an error as a form control name, as "." is not supported in names, so map the flat control name to the component property.
                .map(({ name, value }) => (name === "evcs_ids" ? { name: "evcs.ids", value } : { name, value }))
        );
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
