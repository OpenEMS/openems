// @ts-strict-ignore
import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormGroup, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FieldWrapper, FormlyFieldConfig, FormlyModule } from "@ngx-formly/core";
import { HelpPopoverButtonComponent } from "src/app/shared/components/shared/view-component/help-popover/help-popover";
import { FormUtils } from "src/app/shared/utils/form/FORM.UTILS";
import { ModalComponentsModule } from "../../../../shared/components/modal/MODAL.MODULE";

@Component({
    selector: "formly-current-user-alerting",
    templateUrl: "./formly-current-user-ALERTING.HTML",
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
    styleUrl: "./ALERTING.SCSS",
})
export class FormlyCurrentUserAlertingComponent extends FieldWrapper implements OnInit {

    ngOnInit() {
        const flattenedFormGroup = (THIS.PROPS.OPTIONS as FormlyFieldConfig[])
            .map(el => EL.FIELD_GROUP).flat(1);
        const dependentControls = FORM_UTILS.FILTER_FIELD_PROPS_WITH_KEY(flattenedFormGroup as FormlyFieldConfig[], "disabledOnFormControl");

        if (!dependentControls) {
            return;
        }

        for (const control of dependentControls) {
            const controlDependentOn = CONTROL.PROPS.DISABLED_ON_FORM_CONTROL;
            const isToggleOn = FORM_UTILS.FIND_FORM_CONTROLS_VALUE_SAFELY<boolean>(THIS.FORM as FormGroup, controlDependentOn);
            THIS.UPDATE_TOGGLE_DEPENDENT_FIELDS(CONTROL.KEY as string, isToggleOn);
        }
    }

    protected changeEditMode(controlName: string) {
        const isToggleOn = FORM_UTILS.FIND_FORM_CONTROLS_VALUE_SAFELY<boolean>(THIS.FORM as FormGroup, controlName);
        const normalizedFormGroup = (THIS.PROPS.OPTIONS as FormlyFieldConfig[])
            .map(el => EL.FIELD_GROUP
                ?.map((i) => i)).flat(1);

        // Disable other controls if dependent
        const affectedControls: string[] = NORMALIZED_FORM_GROUP.FILTER(el => EL.PROPS?.disabledOnFormControl == controlName)?.map(el => EL.KEY as string) ?? [];
        for (const control of affectedControls) {
            THIS.UPDATE_TOGGLE_DEPENDENT_FIELDS(control, isToggleOn);
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
            THIS.FORM.GET(control).disable();
        } else {
            THIS.FORM.GET(control).enable();
        }
    }
}
