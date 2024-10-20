import { Component, OnInit } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
    selector: "form-field-multi-step",
    templateUrl: "./form-field-multi-step.html",
})
export class FormlyFieldMultiStepComponent extends FieldType implements OnInit {

    public currentStep: number = 0;

    protected get steps() {
        return this.props.steps || [];
    }

    public ngOnInit() {
        // Ensure the model has an array to track steps
        const stepArray = this.formControl.value;

        if (!Array.isArray(stepArray)) {
            this.formControl.setValue(Array(this.steps.length).fill(false));
        }

        // Listen to status changes to reset steps if disabled
        this.formControl.statusChanges.subscribe(status => {
            if (status === "DISABLED") {
                this.resetSteps();
            }
        });

        // Determine the current step based on the array of steps
        const lastFalseIndex = stepArray.lastIndexOf(false);
        const lastTrueIndex = stepArray.lastIndexOf(true);

        if (lastFalseIndex === -1) {
            // All steps are true, show the final step
            this.currentStep = this.steps.length - 1;
        } else if (lastTrueIndex === -1) {
            // No true steps, show the first step
            this.currentStep = 0;
        } else {
            // Show the last true step
            this.currentStep = lastTrueIndex;
        }
    }


    protected nextStep() {
        if (this.currentStep < this.steps.length - 1) {
            this.currentStep++;
        }
    }

    protected prevStep() {
        if (this.currentStep > 0) {
            this.currentStep--;
        }
    }

    protected onCheckboxChange(event: any, index: number) {
        const updatedValue = this.formControl.value;
        updatedValue[index] = event.detail.checked;
        this.formControl.setValue(updatedValue);
    }

    private resetSteps() {
        this.formControl.setValue(Array(this.steps.length).fill(false));
        this.currentStep = 0;
    }
}
