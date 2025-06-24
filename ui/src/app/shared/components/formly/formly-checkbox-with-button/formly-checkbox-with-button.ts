import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
    selector: "formly-checkbox-button",
    templateUrl: "./formly-checkbox-with-button.html",
    styles: [`
      .custom-item {
        padding-top: 50px; /* Adjust padding for proper spacing */
        display: flex;
        align-items: center; /* Vertically align checkbox, label, and button */
        gap: 1rem; /* Add spacing between items */
        flex-wrap: wrap; /* Allow wrapping on small screens */
    }

    .button-with-icon {
        padding-left: 5%;
        font-size: small;
    }

    .checkbox-label {
        margin-left: 8px; /* Space between checkbox and label */
        font-size: 1rem; /* Adjust text size if needed */
        text-align: left; /* Keep label aligned to the left */
    }

    @media (max-width: 768px) {
        .custom-item {
            padding-top: 20px; /* Reduce padding for smaller screens */
            flex-direction: row; /* Keep items in a row unless the button is large */
            justify-content: space-between; /* Distribute items */
            gap: 0.5rem; /* Adjust spacing */
        }

        .button-with-icon {
            text-align: center;
        }

        .checkbox-label {
            margin-left: 8px; /* Maintain consistent spacing */
            text-align: left; /* Avoid center-aligning unnecessarily */
        }
    }
    `],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class CheckboxButtonTypeComponent extends FieldType {
    onCheckboxChange(event: any) {
        this.formControl.setValue(event.detail.checked);
    }

    onButtonClick() {
        if (this.props.onButtonClick) {
            this.props.onButtonClick();
        }
    }
}
