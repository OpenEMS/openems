import { Component } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
    selector: "formly-input-section",
    templateUrl: "./input.html",
    standalone: false,
    styles: [`
    :host {
        min-width: fit-content;

        .label-text-wrapper{
            .label-text{
                overflow: visible;
            }
        }

        .native-wrapper{
            max-width: max-content !important;
            width: max-content;
            min-width: 20%;

            @media (width <= 576px) {
                text-align: right;
            }
        }

        ion-label{
            text-align: left;
        }

        ion-label>span,
        ion-label>span>small {
            white-space: initial;
        }
}
`],
})
export class InputTypeComponent extends FieldType { }
