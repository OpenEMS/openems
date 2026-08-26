import { Component, ViewEncapsulation, ChangeDetectionStrategy } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
    selector: "formly-input-section",
    templateUrl: "./input.html",
    standalone: false,
    changeDetection: ChangeDetectionStrategy.Eager,
    encapsulation: ViewEncapsulation.None,
})
export class InputTypeComponent extends FieldType {}
