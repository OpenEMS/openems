// @ts-strict-ignore
import { CommonModule } from "@angular/common";
import { Component, OnInit, ViewEncapsulation } from "@angular/core";
import { FormGroup, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FieldWrapper, FormlyFieldConfig } from "@ngx-formly/core";
import { HelpPopoverButtonComponent } from "src/app/shared/components/shared/view-component/help-popover/help-popover";
import { FormUtils } from "src/app/shared/utils/form/FORM.UTILS";
import { AlertingComponent } from "../ALERTING.COMPONENT";

@Component({
    selector: "formly-other-users-alerting",
    templateUrl: "./formly-other-users-ALERTING.HTML",
    standalone: true,
    imports: [
        IonicModule,
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        HelpPopoverButtonComponent,
    ],
    styleUrls: ["./ALERTING.SCSS"],
    encapsulation: VIEW_ENCAPSULATION.NONE,
})
export class FormlyOtherUsersAlertingComponent extends FieldWrapper implements OnInit {

    protected mailCheckBoxChecked: Map<string, boolean> = new Map();

    ngOnInit() {
        const userOptions = THIS.PROPS.OPTIONS as FormlyFieldConfig[] ?? [];
        for (const user of userOptions) {
            for (const alertingField of USER.FIELD_GROUP ?? []) {
                const dependentControls = FORM_UTILS.FILTER_FIELD_PROPS_WITH_KEY(ALERTING_FIELD.FIELD_GROUP, "disabledOnFormControl");

                for (const control of dependentControls) {
                    const controlDependentOn = CONTROL.PROPS.DISABLED_ON_FORM_CONTROL;
                    const isToggleOn = THIS.FORM.CONTROLS[USER.KEY as string]?.controls[controlDependentOn]?.value ?? false;
                    THIS.UPDATE_TOGGLE_DEPENDENT_FIELDS(USER.KEY as string, CONTROL.KEY as string, isToggleOn);
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
    protected changeEditMode(user: string, controlName: string) {
        const userForm = FORM_UTILS.FIND_FORM_CONTROL_SAFELY(THIS.FORM as FormGroup, user) as FormGroup;
        const isToggleOn = FORM_UTILS.FIND_FORM_CONTROLS_VALUE_SAFELY<boolean>(userForm, controlName);
        THIS.CHECK_VALIDITY(user);
        const userOptions = (THIS.PROPS.OPTIONS as any[]).find(el => EL.KEY == user) as FormlyFieldConfig;
        const affectedControls: string[] = USER_OPTIONS.FIELD_GROUP.MAP(el => EL.FIELD_GROUP).flat(1).filter(el => EL.PROPS?.disabledOnFormControl == controlName)?.map(el => EL.KEY as string) ?? [];
        for (const control of affectedControls) {
            THIS.UPDATE_TOGGLE_DEPENDENT_FIELDS(user, control, isToggleOn);
        }
    }

    /**
     * Checks the validity of the form
     *
     * @param user the selected user
     */
    protected checkValidity(user: string) {
        const userForm = FORM_UTILS.FIND_FORM_CONTROL_SAFELY(THIS.FORM as FormGroup, user) as FormGroup;
        THIS.MAIL_CHECK_BOX_CHECKED.SET(user, ALERTING_COMPONENT.IS_FORM_VALID(userForm));
    }

    private updateToggleDependentFields(user: string, control: string, isToggleOn: boolean) {
        const userFormControls = THIS.FORM.CONTROLS[user];
        if (!isToggleOn) {
            USER_FORM_CONTROLS.CONTROLS[control].disable();
        } else {
            USER_FORM_CONTROLS.CONTROLS[control].enable();
        }
    }
}
