// @ts-strict-ignore
import { CommonModule } from "@angular/common";
import { Component, OnInit, ViewEncapsulation } from "@angular/core";
import { FormGroup, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FieldWrapper, FormlyFieldConfig } from "@ngx-formly/core";
import { HelpPopoverButtonComponent } from "src/app/shared/components/shared/view-component/help-popover/help-popover";
import { FormUtils } from "src/app/shared/utils/form/form.utils";
import { AlertingComponent } from "../alerting.component";

@Component({
    selector: "formly-other-users-alerting",
    templateUrl: "./formly-other-users-alerting.html",
    standalone: true,
    imports: [
        IonicModule,
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        HelpPopoverButtonComponent,
    ],
    styleUrls: ["./alerting.scss"],
    encapsulation: ViewEncapsulation.None,
})
export class FormlyOtherUsersAlertingComponent extends FieldWrapper implements OnInit {

    protected mailCheckBoxChecked: Map<string, boolean> = new Map();

    ngOnInit() {
        const userOptions = this.props.options as FormlyFieldConfig[] ?? [];
        for (const user of userOptions) {
            for (const alertingField of user.fieldGroup ?? []) {
                const dependentControls = FormUtils.filterFieldPropsWithKey(alertingField.fieldGroup, "disabledOnFormControl");

                for (const control of dependentControls) {
                    const controlDependentOn = control.props.disabledOnFormControl;
                    const isToggleOn = this.form.controls[user.key as string]?.controls[controlDependentOn]?.value ?? false;
                    this.updateToggleDependentFields(user.key as string, control.key as string, isToggleOn);
                }
            }
        }
    }

    /**
     * Changes the edit mode, disables and enables toggle dependent fields
     *
     * @param user the selected user
     * @param controlName the control to update
     */
    protected changeEditMode(user: string, controlName: string, invert: boolean) {

        /** If formControl provided, use it directly */
        const userForm = FormUtils.findFormControlSafely(this.form as FormGroup, user) as FormGroup;
        const isToggleOn = FormUtils.findFormControlsValueSafely<boolean>(userForm, controlName);
        const toggleState = invert === true ? !isToggleOn : isToggleOn;
        if (invert === true) {
            userForm.get(controlName).setValue(toggleState);
            userForm.get(controlName).markAsDirty();
        }

        this.checkValidity(user);
        const userOptions = (this.props.options as any[]).find(el => el.key == user) as FormlyFieldConfig;
        const affectedControls: string[] = userOptions.fieldGroup.map(el => el.fieldGroup).flat(1).filter(el => el.props?.disabledOnFormControl == controlName)?.map(el => el.key as string) ?? [];
        for (const control of affectedControls) {
            this.updateToggleDependentFields(user, control, toggleState);
        }
    }

    /**
     * Checks the validity of the form
     *
     * @param user the selected user
     */
    protected checkValidity(user: string) {
        const userForm = FormUtils.findFormControlSafely(this.form as FormGroup, user) as FormGroup;
        this.mailCheckBoxChecked.set(user, AlertingComponent.isFormValid(userForm));
    }

    private updateToggleDependentFields(user: string, control: string, isToggleOn: boolean) {
        const userFormControls = this.form.controls[user];
        if (!isToggleOn) {
            userFormControls.controls[control].disable();
        } else {
            userFormControls.controls[control].enable();
        }
    }
}
