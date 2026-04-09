import { CommonModule } from "@angular/common";
import { Component, inject } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

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
export class ControllerIoHeatpumpHomeComponent extends AbstractFormlyComponent {

    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";
    private route: ActivatedRoute = inject(ActivatedRoute);

    public static getFormlyGeneralView(translate: TranslateService, component: EdgeConfig.Component): OeFormlyView {
        const lines: OeFormlyField[] = [];
        lines.push({
            type: "channel-line",
            name: translate.instant("GENERAL.STATUS"),
            channel: component.id + "/Status",
            converter: Converter.HEAT_PUMP_STATES(translate),
        }, {
            type: "channel-line",
            name: translate.instant("GENERAL.MODE"),
            channel: component.id + "/_PropertyMode",
            converter: Converter.CONTROLLER_PROPERTY_MODES(translate),
        });
        return {
            title: component.alias,
            helpKey: "CONTROLLER_IO_HEAT_PUMP_SG_READY",
            lines: lines,
            component: component,
        };
    }

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const component = config.getComponentSafely(this.route.snapshot.params.componentId);
        AssertionUtils.assertIsDefined(component);

        return ControllerIoHeatpumpHomeComponent.getFormlyGeneralView(this.translate, component);
    }
}
