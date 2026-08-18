import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule } from "@ngx-translate/core";
import { takeUntil } from "rxjs";
import { AbstractFormlyComponent, OeFormlyView, ViewContext, } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { FormUtils } from "src/app/shared/utils/form/form.utils";
import { BaseMode, HeatpumpMode, ManualState, SharedControllerIoHeatpump } from "../shared/shared";

@Component({
    selector: "oe-controller-io-heatpump-base-mode",
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: true,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
})
export class ControllerIoHeatpumpBaseModeComponent extends AbstractFormlyComponent<{ mode: HeatpumpMode }> {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private component: EdgeConfig.Component | null = null;

    protected override generateView(viewContext: ViewContext): OeFormlyView<{ mode: HeatpumpMode }> {
        this.component = this.getComponent();
        AssertionUtils.assertIsDefined(this.component);
        return SharedControllerIoHeatpump.getFormlyBaseModeView(
            viewContext.translate,
            this.component,
            viewContext.edge,
        );
    }

    protected override getFormGroup(): FormGroup {
        const component = this.component ?? this.getComponent();

        const formgroup = new FormGroup({
            mode: new FormControl(component.getPropertyFromComponent<HeatpumpMode>("mode")),
            manualState: new FormControl(component.getPropertyFromComponent<ManualState>("manualState")),
            baseMode: new FormControl(component.getPropertyFromComponent<BaseMode>("baseMode")),
        });

        formgroup.controls["baseMode"].valueChanges.pipe(takeUntil(this.stopOnDestroy)).subscribe((value) => {
            if (value === null) {
                return;
            }

            const mode = FormUtils.findFormControlsValueSafely(formgroup, "mode");
            const manualState = FormUtils.findFormControlsValueSafely(formgroup, "manualState");

            AssertionUtils.assertIsDefined(mode);
            AssertionUtils.assertIsDefined(manualState);

            if (value === BaseMode.AUTOMATIC) {
                if (mode !== HeatpumpMode.AUTOMATIC) {
                    this.form.controls["mode"].setValue(HeatpumpMode.AUTOMATIC);
                    this.form.controls["mode"].markAsDirty();
                }
                return;
            }

            if (mode !== HeatpumpMode.MANUAL) {
                this.form.controls["mode"].setValue(HeatpumpMode.MANUAL);
                this.form.controls["mode"].markAsDirty();
            }

            if (value.toString() !== manualState.toString()) {
                this.form.controls["manualState"].setValue(value);
                this.form.controls["manualState"].markAsDirty();
            }
        });

        return formgroup;
    }

    protected override getChannelAddresses(): Promise<ChannelAddress[]> {
        const component = this.component ?? this.getComponent();

        return Promise.resolve([new ChannelAddress(component.id, "_PropertyBaseMode")]);
    }
}
