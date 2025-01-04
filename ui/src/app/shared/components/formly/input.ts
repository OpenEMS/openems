import { Component } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
    selector: "formly-input-section",
    templateUrl: "./input.html",
    standalone: false,
})
export class InputTypeComponent extends FieldType { }
