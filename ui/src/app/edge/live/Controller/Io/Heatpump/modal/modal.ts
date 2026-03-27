import { CommonModule } from "@angular/common";
import { Component, inject, Input } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule } from "@ngx-translate/core";
import { Subject, takeUntil } from "rxjs";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { FormUtils } from "src/app/shared/utils/form/form.utils";
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
export class ControllerIoHeatpumpModalComponent extends AbstractFormlyComponent {
    @Input() public edge: Edge | null = null;
    @Input() public component: EdgeConfig.Component | null = null;

    private destroy$ = new Subject<void>();
    private route: ActivatedRoute = inject(ActivatedRoute);

    public override async ngOnDestroy() {
        this.destroy$.next();
        this.destroy$.complete();
        super.ngOnDestroy();
    }

    protected override generateView(): OeFormlyView {
        AssertionUtils.assertIsDefined(this.edge);
        AssertionUtils.assertIsDefined(this.component);
        const mode = this.component.getPropertyFromComponent("mode") as "AUTOMATIC" | "MANUAL";
        return SharedControllerIoHeatpump.getFormlyView(this.translate, this.component, this.edge, mode);
    }

    protected override getFormGroup(): FormGroup {
        const fg = SharedControllerIoHeatpump.getFormGroup();
        FormUtils.findFormControlSafely(fg, "mode")?.valueChanges
            .pipe(takeUntil(this.destroy$))
            .subscribe((mode: "AUTOMATIC" | "MANUAL") => {
                this.updateView(mode);
            });
        return fg;
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

    protected updateView(mode: "AUTOMATIC" | "MANUAL") {
        AssertionUtils.assertIsDefined(this.component);
        AssertionUtils.assertIsDefined(this.edge);
        const value = mode;
        const view = SharedControllerIoHeatpump.getFormlyView(this.translate, this.component, this.edge, value);
        this.updateWholeViewOnFormControlChange(view, this.form, this.service.websocket);
    }
}
