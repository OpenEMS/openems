import { Component } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";

@Component({
    selector: "formly-input-serial-number",
    templateUrl: "./input-serial-number-wrapper.html",
    standalone: false,
    styles: [`
        .input-box {
            border: 1px solid var(--ion-color-dark);
            border-radius: 2px; /* Add rounded corners */
            width: 100%;
            padding-left: 2px;
        }

        .input-box ion-item {
            --min-height: auto;
        }

        .disabled-field {
            pointer-events: none; /* Disable interactions */
            opacity: 0.5; /* Greyed-out effect */
        }

        .disabled-field ion-input,
        .disabled-field ion-label {
            color: var(--ion-color-medium); /* Adjust color for a disabled look */
        }

        .text-center {
            text-align: center; /* Default for larger screens */
        }

        @media (max-width: 768px) {
            .text-center {
                text-align: --webkit-center; /* Override for mobile view */
            }

            .input-box {
                max-width: 90%; /* Shrink width for small screens */
            }
        }

        @media (max-width: 480px) {
            .input-box {
                max-width: 100%; /* Full width for very small screens */
            }
        }
    `],
})
export class FormlyInputSerialNumberWrapperComponent extends FieldWrapper {

    protected isFocused: boolean = false;

    /**
     * Indicates the field if it is focued on not. helpful for setting highlight for the input field.
     *
     * @param focused boolean value indicating the field is focused or not.
     */
    protected setFocus(focused: boolean): void {
        this.isFocused = focused;
    }

    protected onCheckboxChange(checked: boolean): void {
        this.props.checkbox.value = checked;

        if (this.props.checkbox.updateFn) {
            this.props.checkbox.updateFn(checked);
        }
    }
}
