// @ts-strict-ignore
import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormGroup, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FieldWrapper, FormlyFieldConfig, FormlyModule } from "@ngx-formly/core";
import { HelpPopoverButtonComponent } from "src/app/shared/components/shared/view-component/help-popover/help-popover";
import { FormUtils } from "src/app/shared/utils/form/form.utils";
import { ModalComponentsModule } from "../../../../shared/components/modal/modal.module";

@Component({
    selector: "formly-current-user-alerting",
    templateUrl: "./formly-current-user-alerting.html",
    standalone: true,
    imports: [
        IonicModule,
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        HelpPopoverButtonComponent,
        ModalComponentsModule,
        FormlyModule,
    ],
    styleUrl: "./alerting.scss",
})
export class FormlyCurrentUserAlertingComponent extends FieldWrapper implements OnInit {

    ngOnInit() {
        const flattenedFormGroup = (this.props.options as FormlyFieldConfig[])
            .map(el => el.fieldGroup).flat(1);
        const dependentControls = FormUtils.filterFieldPropsWithKey(flattenedFormGroup as FormlyFieldConfig[], "disabledOnFormControl");

        if (!dependentControls) {
            return;
        }

        for (const control of dependentControls) {
            const controlDependentOn = control.props.disabledOnFormControl;
            const isToggleOn = FormUtils.findFormControlsValueSafely<boolean>(this.form as FormGroup, controlDependentOn);
            this.updateToggleDependentFields(control.key as string, isToggleOn);
        }
    }

    protected changeEditMode(controlName: string) {
        const isToggleOn = FormUtils.findFormControlsValueSafely<boolean>(this.form as FormGroup, controlName);
        const normalizedFormGroup = (this.props.options as FormlyFieldConfig[])
            .map(el => el.fieldGroup
                ?.map((i) => i)).flat(1);

        // Disable other controls if dependent
        const affectedControls: string[] = normalizedFormGroup.filter(el => el.props?.disabledOnFormControl == controlName)?.map(el => el.key as string) ?? [];
        for (const control of affectedControls) {
            this.updateToggleDependentFields(control, isToggleOn);
        }
    }

    /**
     * Updates edit mode of toggle dependent fields
     *
     * @param control the control to update
     * @param isToggleOn the current state of the dependent toggle
     */
    private updateToggleDependentFields(control: string, isToggleOn: boolean) {
        if (!isToggleOn) {
            this.form.get(control).disable();
        } else {
            this.form.get(control).enable();
        }
    }
}
