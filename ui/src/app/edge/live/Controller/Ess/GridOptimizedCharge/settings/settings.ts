import { Component, inject } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../../livedataservice";
import { NewNavigationPredictionChartComponent } from "../shared/prediction-chart";
import { GridOptimizedChargeViewModel, SharedGridOptimizedCharge } from "../shared/shared";

@Component({
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
})
export class ControllerEssGridOptimizedChargeSettingsComponent extends AbstractFormlyComponent<GridOptimizedChargeViewModel> {

    public component: EdgeConfig.Component | null = null;
    public targetEpochSeconds: number | null = null;
    public chargeStartEpochSeconds: number | null = null;

    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private routeService: RouteService = inject(RouteService);

    public static generateView(
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
        targetEpochSeconds: number | null,
        chargeStartEpochSeconds: number | null,
    ): OeFormlyView<GridOptimizedChargeViewModel> {
        const predictionChartComponent = NewNavigationPredictionChartComponent;
        return SharedGridOptimizedCharge.getFormlyView(translate, component, edge, targetEpochSeconds, chargeStartEpochSeconds, predictionChartComponent);
    }

    protected override generateView(): OeFormlyView<GridOptimizedChargeViewModel> {
        const edge = this.service.currentEdge();
        AssertionUtils.assertIsDefined(edge);
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);
        this.component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(this.component);

        return ControllerEssGridOptimizedChargeSettingsComponent.generateView(this.translate, this.component, edge, this.targetEpochSeconds, this.chargeStartEpochSeconds);
    }

    protected override getFormGroup(): FormGroup {
        return SharedGridOptimizedCharge.getFormGroup();
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        return SharedGridOptimizedCharge.getChannelAddresses(this.service, this.routeService, this.component);
    }

    protected override onCurrentData(currentData: CurrentData): void {
        const component = this.component;
        AssertionUtils.assertIsDefined(component);

        this.targetEpochSeconds = currentData.allComponents[component.id + "/TargetEpochSeconds"];
        this.chargeStartEpochSeconds = currentData.allComponents[component.id + "/PredictedChargeStartEpochSeconds"];

        this.setFormControlSafelyWithChannel(this.form, "mode", currentData, new ChannelAddress(component.id, "_PropertyMode"));
        this.setFormControlSafelyWithChannel(this.form, "delayChargeState", currentData, new ChannelAddress(component.id, "DelayChargeState"));
        this.setFormControlSafelyWithChannel(this.form, "workMode", currentData, new ChannelAddress(component.id, "_PropertyWorkMode"));
        this.setFormControlSafelyWithChannel(this.form, "manualTargetTime", currentData, new ChannelAddress(component.id, "_PropertyManualTargetTime"));
        this.setFormControlSafelyWithChannel(this.form, "delayChargeRiskLevel", currentData, new ChannelAddress(component.id, "_PropertyDelayChargeRiskLevel"));
    }
}
