import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { parse } from "date-fns";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { FormUtils } from "src/app/shared/utils/form/form.utils";
import { NumberUtils } from "src/app/shared/utils/number/number-utils";
import { LiveDataService } from "../../../../livedataservice";
import { HeatingElementViewModel, SharedControllerIoHeatingElement } from "../shared/shared";

@Component({
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    providers: [{ provide: DataService, useClass: LiveDataService }],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
})
export class ControllerIoHeatingElementSettingsComponent extends AbstractFormlyComponent<HeatingElementViewModel> {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    public static generateView(
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
    ): OeFormlyView<HeatingElementViewModel> {
        return SharedControllerIoHeatingElement.getFormlyView(translate, component, edge);
    }

    protected override generateView(): OeFormlyView<HeatingElementViewModel> {
        const component = this.getComponent();
        const edge = this.service.currentEdge();
        AssertionUtils.assertIsDefined(component);
        AssertionUtils.assertIsDefined(edge);
        return ControllerIoHeatingElementSettingsComponent.generateView(this.translate, component, edge);
    }

    protected override getFormGroup(): FormGroup {
        const component = this.getComponent();
        return SharedControllerIoHeatingElement.getFormGroup(component);
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        const component = this.getComponent();
        return SharedControllerIoHeatingElement.getChannelAddresses(component);
    }

    protected override onCurrentData(currentData: CurrentData): void {
        const component = this.getComponent();

        AssertionUtils.assertIsDefined(component);
        this.setFormControlSafelyWithChannel(
            this.form,
            "mode",
            currentData,
            new ChannelAddress(component.id, "_PropertyMode"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "defaultLevel",
            currentData,
            new ChannelAddress(component.id, "_PropertyDefaultLevel"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "minTime",
            currentData,
            new ChannelAddress(component.id, "_PropertyMinTime"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "workMode",
            currentData,
            new ChannelAddress(component.id, "_PropertyWorkMode"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "endTime",
            currentData,
            new ChannelAddress(component.id, "_PropertyEndTime"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "minEnergyLimitInKwh",
            currentData,
            new ChannelAddress(component.id, "_PropertyMinEnergyLimitInKwh"),
        );
        let maxPower = 0;
        const avgPowerPhase1 = currentData.allComponents[component.id + "/Phase1AvgPower"];
        const avgPowerPhase2 = currentData.allComponents[component.id + "/Phase2AvgPower"];
        const avgPowerPhase3 = currentData.allComponents[component.id + "/Phase3AvgPower"];
        const totalPower = NumberUtils.addSafely(avgPowerPhase1, avgPowerPhase2, avgPowerPhase3, 3000);
        const neededPower = NumberUtils.multiplySafely(
            NumberUtils.divideSafely(totalPower, 1000),
            SharedControllerIoHeatingElement.PREDICTED_PV_PRODUCTION_HOUR,
        );
        if (totalPower !== null && neededPower != null && neededPower > maxPower) {
            maxPower = neededPower;
        }
        this.setFormControlSafelyWithValue(this.form, "maxPower", maxPower);
        const currentEnergy = currentData.allComponents[component.id + "/SessionEnergy"]; // Wh
        const endTimeWithMeter = FormUtils.findFormControlsValueSafely<string | null>(this.form, "endTimeWithMeter");
        const minEnergyLimitInKwh = FormUtils.findFormControlsValueSafely<number | null>(
            this.form,
            "minEnergyLimitInKwh",
        );

        const requiredPower = this.getRequiredPower(endTimeWithMeter, minEnergyLimitInKwh, currentEnergy); // W
        const isUnreachable =
            requiredPower !== null && totalPower !== null
                ? requiredPower > totalPower * SharedControllerIoHeatingElement.POWER_OVERSHOOT_FACTOR
                : false;

        this.setFormControlSafelyWithValue(this.form, "isUnreachable", isUnreachable);
    }
    private getRequiredPower(
        endTimeWithMeter: string | null,
        minEnergyLimitInKwh: number | null,
        currentEnergyWh: number | null | undefined,
    ): number | null {
        if (endTimeWithMeter == null || currentEnergyWh == null || !Number.isFinite(minEnergyLimitInKwh)) {
            return null;
        }

        const now = new Date();
        const endTime = parse(endTimeWithMeter, "HH:mm", now);
        const startTime = new Date(now);

        const energyLimitWh = NumberUtils.multiplySafely(minEnergyLimitInKwh, 1000);
        let remainingEnergyWh = NumberUtils.subtractSafely(energyLimitWh, currentEnergyWh);

        if (endTime.getTime() < now.getTime()) {
            endTime.setDate(endTime.getDate() + 1);
            startTime.setDate(now.getDate() + 1);
            startTime.setHours(0, 0, 0, 0);
            remainingEnergyWh = energyLimitWh;
        }

        const remainingHours = NumberUtils.divideSafely(
            NumberUtils.divideSafely(NumberUtils.subtractSafely(endTime.getTime(), startTime.getTime()), 1000),
            3600,
        );
        if (remainingHours == null || remainingHours <= 0) {
            return null;
        }

        return NumberUtils.divideSafely(remainingEnergyWh, remainingHours);
    }
}
