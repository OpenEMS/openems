import { Component } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";

@Component({
    selector: "formly-field-modal",
    templateUrl: "./formlyfieldmodal.html",
    standalone: false,
})
export class FormlyFieldModalComponent extends FieldWrapper { }
