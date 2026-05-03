import { CommonModule } from "@angular/common";
import { Component, inject } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { Mode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { SharedControllerIoHeatpump } from "../shared/shared";

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
export class ControllerIoHeatpumpSettingsComponent extends AbstractFormlyComponent<{ mode: Mode }> {

    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private component: EdgeConfig.Component | null = null;
    private route: ActivatedRoute = inject(ActivatedRoute);

    public static getFormlyGeneralView(translate: TranslateService, component: EdgeConfig.Component, edge: Edge): OeFormlyView<{ mode: Mode }> {
        return SharedControllerIoHeatpump.getFormlyView(translate, component, edge);
    }

    protected override generateView(): OeFormlyView<{ mode: Mode }> {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);
        AssertionUtils.assertIsDefined(edge);

        this.component = config.getComponentSafely(this.route.snapshot.params.componentId);
        AssertionUtils.assertIsDefined(this.component);
        return ControllerIoHeatpumpSettingsComponent.getFormlyGeneralView(this.translate, this.component, edge);
    }

    protected override onCurrentData(currentData: CurrentData): void {
        if (this.component == null) {
            return;
        }
        this.setFormControlSafelyWithChannel(this.form, "mode", currentData, new ChannelAddress(this.component.id, "_PropertyMode"));
        this.setFormControlSafelyWithChannel(this.form, "automaticRecommendationCtrlEnabled", currentData, new ChannelAddress(this.component.id, "_PropertyAutomaticRecommendationCtrlEnabled"));
        this.setFormControlSafelyWithChannel(this.form, "automaticForceOnCtrlEnabled", currentData, new ChannelAddress(this.component.id, "_PropertyAutomaticForceOnCtrlEnabled"));
        this.setFormControlSafelyWithChannel(this.form, "automaticForceOnSurplusPower", currentData, new ChannelAddress(this.component.id, "_PropertyAutomaticForceOnSurplusPower"));
        this.setFormControlSafelyWithChannel(this.form, "automaticRecommendationSurplusPower", currentData, new ChannelAddress(this.component.id, "_PropertyAutomaticRecommendationSurplusPower"));
        this.setFormControlSafelyWithChannel(this.form, "automaticForceOnSoc", currentData, new ChannelAddress(this.component.id, "_PropertyAutomaticForceOnSoc"));
        this.setFormControlSafelyWithChannel(this.form, "automaticLockCtrlEnabled", currentData, new ChannelAddress(this.component.id, "_PropertyAutomaticLockCtrlEnabled"));
        this.setFormControlSafelyWithChannel(this.form, "automaticLockGridBuyPower", currentData, new ChannelAddress(this.component.id, "_PropertyAutomaticLockGridBuyPower"));
        this.setFormControlSafelyWithChannel(this.form, "automaticLockSoc", currentData, new ChannelAddress(this.component.id, "_PropertyAutomaticLockSoc"));
        this.setFormControlSafelyWithChannel(this.form, "minimumSwitchingTime", currentData, new ChannelAddress(this.component.id, "_PropertyMinimumSwitchingTime"));
        this.setFormControlSafelyWithChannel(this.form, "manualState", currentData, new ChannelAddress(this.component.id, "_PropertyManualState"));
    }

    protected override getFormGroup(): FormGroup {
        return SharedControllerIoHeatpump.getFormGroup();
    }

    protected override getChannelAddresses(): Promise<ChannelAddress[]> {
        return SharedControllerIoHeatpump.getChannelAddresses(this.service, this.route);
    }
}
