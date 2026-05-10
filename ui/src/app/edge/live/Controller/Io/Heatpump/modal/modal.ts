import { CommonModule } from "@angular/common";
import { Component, inject, Input } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule } from "@ngx-translate/core";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { Mode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { SharedControllerIoHeatpump } from "../shared/shared";

@Component({
    selector: "heatpump-modal",
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    imports: [
        CommonModule,
        IonicModule,
        ReactiveFormsModule,
        FormlyModule,
        TranslateModule,
    ],
})
export class ControllerIoHeatpumpModalComponent extends AbstractFormlyComponent<{ mode: Mode }> {
    @Input() public edge: Edge | null = null;
    @Input() public component: EdgeConfig.Component | null = null;

    private route: ActivatedRoute = inject(ActivatedRoute);

    protected override generateView(): OeFormlyView<{ mode: Mode }> {
        AssertionUtils.assertIsDefined(this.edge);
        AssertionUtils.assertIsDefined(this.component);
        return SharedControllerIoHeatpump.getFormlyView(this.translate, this.component, this.edge);
    }

    protected override getFormGroup(): FormGroup {
        return SharedControllerIoHeatpump.getFormGroup();
    }

    protected override getChannelAddresses(): Promise<ChannelAddress[]> {
        return SharedControllerIoHeatpump.getChannelAddresses(this.service, this.route, this.component);
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
}
