import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { SharedControllerTimeslotPeakshaving } from "../shared/shared";

@Component({
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class ControllerPeakShavingSymmetricTimeSlotSettingsComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private component: EdgeConfig.Component | null = null;

    public static getFormlyGeneralView(
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
    ): OeFormlyView {
        return {
            title: component.alias,
            icon: SharedControllerTimeslotPeakshaving.SHARED_ICON,
            lines: SharedControllerTimeslotPeakshaving.getFormlySettingsLines(translate, component, edge),
            component: component,
            edge: edge,
        };
    }

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        AssertionUtils.assertIsDefined(edge);
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        this.component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(this.component);
        return ControllerPeakShavingSymmetricTimeSlotSettingsComponent.getFormlyGeneralView(
            this.translate,
            this.component,
            edge,
        );
    }

    protected override onCurrentData(currentData: CurrentData): void {
        this.component ??= this.getComponent();

        this.setFormControlSafelyWithChannel(
            this.form,
            "peakShavingPower",
            currentData,
            new ChannelAddress(this.component.id, "_PropertyPeakShavingPower"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "rechargePower",
            currentData,
            new ChannelAddress(this.component.id, "_PropertyRechargePower"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "slowChargeStartTime",
            currentData,
            new ChannelAddress(this.component.id, "_PropertySlowChargeStartTime"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "slowChargePower",
            currentData,
            new ChannelAddress(this.component.id, "_PropertySlowChargePower"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "endTime",
            currentData,
            new ChannelAddress(this.component.id, "_PropertyEndTime"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "startTime",
            currentData,
            new ChannelAddress(this.component.id, "_PropertyStartTime"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "endDate",
            currentData,
            new ChannelAddress(this.component.id, "_PropertyEndDate"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "startDate",
            currentData,
            new ChannelAddress(this.component.id, "_PropertyStartDate"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "hysteresisSoc",
            currentData,
            new ChannelAddress(this.component.id, "_PropertyHysteresisSoc"),
        );

        this.setFormControlSafelyWithChannel(
            this.form,
            "monday",
            currentData,
            new ChannelAddress(this.component.id, "_PropertyMonday"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "tuesday",
            currentData,
            new ChannelAddress(this.component.id, "_PropertyTuesday"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "wednesday",
            currentData,
            new ChannelAddress(this.component.id, "_PropertyWednesday"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "thursday",
            currentData,
            new ChannelAddress(this.component.id, "_PropertyThursday"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "friday",
            currentData,
            new ChannelAddress(this.component.id, "_PropertyFriday"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "saturday",
            currentData,
            new ChannelAddress(this.component.id, "_PropertySaturday"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "sunday",
            currentData,
            new ChannelAddress(this.component.id, "_PropertySunday"),
        );
    }

    protected override getFormGroup(): FormGroup {
        return SharedControllerTimeslotPeakshaving.getFormGroup();
    }

    protected override getChannelAddresses(): Promise<ChannelAddress[]> {
        this.component ??= this.getComponent();
        return SharedControllerTimeslotPeakshaving.getChannelAddresses(this.component);
    }
}
