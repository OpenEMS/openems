import { CommonModule } from "@angular/common";
import { Component, inject, ChangeDetectionStrategy } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { SharedControllerPeakShaving } from "../../shared/shared";

@Component({
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class ControllerPeakShavingSymmetricSettingsComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private component: EdgeConfig.Component | null = null;
    private readonly routeService: RouteService = inject(RouteService);

    public static getFormlyGeneralView(
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
    ): OeFormlyView {
        return {
            title: component.alias,
            lines: [
                ...SharedControllerPeakShaving.getSingleMeasuredLine(translate, component),
                { type: "horizontal-line" },
                ...SharedControllerPeakShaving.getSettingsInputLines(translate, edge),
            ],
            component: component,
        };
    }

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        AssertionUtils.assertIsDefined(edge);
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        this.component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(this.component);
        return ControllerPeakShavingSymmetricSettingsComponent.getFormlyGeneralView(
            this.translate,
            this.component,
            edge,
        );
    }

    protected override onCurrentData(currentData: CurrentData): void {
        if (this.component == null) {
            return;
        }
        SharedControllerPeakShaving.setSettingsCurrentData(
            this.form,
            currentData,
            this.component.id,
            this.setFormControlSafelyWithChannel.bind(this),
        );
    }

    protected override getFormGroup(): FormGroup {
        return SharedControllerPeakShaving.getFormGroup();
    }

    protected override getChannelAddresses(): Promise<ChannelAddress[]> {
        return SharedControllerPeakShaving.getChannelAddresses(this.service, this.routeService);
    }
}
