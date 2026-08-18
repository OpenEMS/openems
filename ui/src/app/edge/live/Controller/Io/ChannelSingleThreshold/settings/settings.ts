import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { takeUntil } from "rxjs";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { Mode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { FormUtils } from "src/app/shared/utils/form/form.utils";
import { LiveDataService } from "../../../../livedataservice";
import { SharedIoChannelSingleThreshold } from "../shared/shared";

@Component({
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
    providers: [{ provide: DataService, useClass: LiveDataService }],
    changeDetection: ChangeDetectionStrategy.Eager,
})
export class ControllerIoChannelSingleThresholdSettingsComponent extends AbstractFormlyComponent<{
    mode: Mode;
    inputChannelAddressToggleValue: "SOC" | "GRIDSELL" | "GRIDBUY" | "PRODUCTION";
}> {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    protected refreshInputMode: boolean = false;

    private component: EdgeConfig.Component | null = null;
    private thresholdNormalizationInitialized = false;

    private readonly websocket: Websocket = inject(Websocket);

    public static async generateView(
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
        websocket: Websocket,
    ): Promise<
        OeFormlyView<{
            mode: Mode;
            inputChannelAddressToggleValue: "SOC" | "GRIDSELL" | "GRIDBUY" | "PRODUCTION";
        }>
    > {
        return SharedIoChannelSingleThreshold.getFormlyView(translate, component, edge, websocket);
    }

    protected override async generateView(): Promise<
        OeFormlyView<{
            mode: Mode;
            inputChannelAddressToggleValue: "SOC" | "GRIDSELL" | "GRIDBUY" | "PRODUCTION";
        }>
    > {
        const edge = this.service.currentEdge();
        const component = this.getComponent();
        AssertionUtils.assertIsDefined(edge);
        AssertionUtils.assertIsDefined(component);

        return ControllerIoChannelSingleThresholdSettingsComponent.generateView(
            this.translate,
            component,
            edge,
            this.websocket,
        );
    }

    protected override applyChanges(
        fg: FormGroup<any>,
        service: Service,
        websocket: Websocket,
        component: EdgeConfig.Component | null,
        edge: Edge | null,
    ): void {
        this.refreshInputMode = true;
        super.applyChanges(fg, service, websocket, component, edge);
    }

    protected override getFormGroup(): FormGroup {
        return SharedIoChannelSingleThreshold.getFormGroup();
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        const component = this.getComponent();
        return SharedIoChannelSingleThreshold.getChannelAddresses(this.service, this.routeService, component);
    }

    protected override onCurrentData(currentData: CurrentData): void {
        this.component ??= this.getComponent();

        const inputChannelAddressToggleValueControl = FormUtils.findFormControlSafely(
            this.form,
            "inputChannelAddressToggleValue",
        ) as FormControl<SharedIoChannelSingleThreshold.InputMode> | null;

        const inputChannelAddress = this.component.getPropertyFromComponent<string>("inputChannelAddress");

        if (inputChannelAddress == null) {
            return;
        }

        if (!this.thresholdNormalizationInitialized) {
            this.normalizeThresholdForInputMode();
        }

        const inputMode = SharedIoChannelSingleThreshold.getInputMode(this.component, inputChannelAddress, currentData);

        const inputChannelAddressToggleValue = FormUtils.findFormControlsValueSafely<string>(
            this.form,
            "inputChannelAddressToggleValue",
        );

        // need to set the value once at the beginning with getInputMode, as the edge does not tell wich mode it is in
        let value = inputChannelAddressToggleValue ?? inputMode;

        if (this.refreshInputMode) {
            value = SharedIoChannelSingleThreshold.getInputModeFromChannel(
                this.component,
                currentData,
                new ChannelAddress(this.component.id, "_PropertyInputChannelAddress").toString(),
            );
            this.form.get("inputChannelAddress")?.markAsPristine();
            this.refreshInputMode = false;
        }

        this.setFormControlSafelyWithValue(this.form, "inputChannelAddressToggleValue", value);

        if (this.form.controls["invert"]?.value === null) {
            this.setFormControlSafelyWithValue(
                this.form,
                "invert",
                currentData.allComponents[new ChannelAddress(this.component.id, "_PropertyInvert").toString()] == 1,
            );
        }
        if (inputChannelAddressToggleValueControl != null && inputChannelAddressToggleValue != null) {
            this.setFormControlSafelyWithValue(
                this.form,
                "inputChannelAddress",
                SharedIoChannelSingleThreshold.convertToChannelAddress(inputChannelAddressToggleValueControl.value),
            );
        }

        if (
            inputChannelAddressToggleValue != null &&
            inputMode != null &&
            inputChannelAddressToggleValue != inputMode
        ) {
            FormUtils.findFormControlSafely(this.form, "inputChannelAddress")?.markAsDirty();
        }

        this.setFormControlSafelyWithChannel(
            this.form,
            "mode",
            currentData,
            new ChannelAddress(this.component.id, "_PropertyMode"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "threshold",
            currentData,
            new ChannelAddress(this.component.id, "_PropertyThreshold"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "switchedLoadPower",
            currentData,
            new ChannelAddress(this.component.id, "_PropertySwitchedLoadPower"),
        );
        this.setFormControlSafelyWithChannel(
            this.form,
            "minimumSwitchingTime",
            currentData,
            new ChannelAddress(this.component.id, "_PropertyMinimumSwitchingTime"),
        );
    }

    private normalizeThresholdForInputMode(): void {
        const modeCtrl = this.form.get(
            "inputChannelAddressToggleValue",
        ) as FormControl<SharedIoChannelSingleThreshold.InputMode> | null;
        const thresholdCtrl = this.form.get("threshold") as FormControl<number | null> | null;

        if (!modeCtrl || !thresholdCtrl) {
            return;
        }

        const normalizeValue = (mode: SharedIoChannelSingleThreshold.InputMode | null) => {
            const value = thresholdCtrl.value;
            if (value == null) {
                return;
            }

            if (mode === "SOC") {
                const clampedValue = Math.max(0, Math.min(100, value));
                if (clampedValue !== value) {
                    thresholdCtrl.setValue(clampedValue);
                }
                return;
            }

            if (mode === "GRIDSELL") {
                const negativeValue = value > 0 ? -value : value;
                if (negativeValue !== value) {
                    thresholdCtrl.setValue(negativeValue);
                    thresholdCtrl.markAsDirty();
                }
                return;
            }

            if (value < 0) {
                thresholdCtrl.setValue(Math.abs(value));
            }
        };

        normalizeValue(modeCtrl.value);

        modeCtrl.valueChanges.pipe(takeUntil(this.stopOnDestroy)).subscribe((mode) => normalizeValue(mode));

        this.thresholdNormalizationInitialized = true;
    }
}
