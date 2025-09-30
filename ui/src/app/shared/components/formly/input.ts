import { Component, ViewEncapsulation } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
    selector: "formly-input-section",
    templateUrl: "./INPUT.HTML",
    standalone: false,
    encapsulation: VIEW_ENCAPSULATION.NONE,
})
export class InputTypeComponent extends FieldType { }
