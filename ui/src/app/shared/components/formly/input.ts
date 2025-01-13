import { Component } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
    selector: "formly-input-section",
    templateUrl: "./input.html",
    standalone: false,
    styles: [`
    :host {
        .label-text-wrapper {
            max-width: fit-content;
            width: 70%;
        }

        .native-wrapper{
            width: max-content;

            @media (width <= 576px) {
                text-align: right;
            }
         
        }

        ion-label>span,
        ion-label>span>small {
            white-space: initial;
        }
}
`]
})
export class InputTypeComponent extends FieldType { }
