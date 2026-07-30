import { CommonModule } from "@angular/common";
import { Component, inject, ChangeDetectionStrategy } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { SharedControllerTimeslotPeakshaving } from "../shared/shared";

@Component({
    selector: "oe-controller-peak-shaving-symmetric-time-slot",
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class ControllerPeakShavingSymmetricTimeSlotHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";
    private readonly routeService: RouteService = inject(RouteService);

    public static getFormlyGeneralView(translate: TranslateService, component: EdgeConfig.Component): OeFormlyView {
        return {
            title: component.alias,
            icon: SharedControllerTimeslotPeakshaving.SHARED_ICON,
            lines: SharedControllerTimeslotPeakshaving.getFormlyFlatLines(translate, component),
            component: component,
        };
    }

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        AssertionUtils.assertIsDefined(edge);
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(component);

        return ControllerPeakShavingSymmetricTimeSlotHomeComponent.getFormlyGeneralView(this.translate, component);
    }
}
