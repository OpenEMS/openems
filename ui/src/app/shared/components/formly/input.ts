import { Component, ViewEncapsulation } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
    selector: "formly-input-section",
    templateUrl: "./input.html",
    standalone: false,
    encapsulation: ViewEncapsulation.None,
})
export class InputTypeComponent extends FieldType { }
