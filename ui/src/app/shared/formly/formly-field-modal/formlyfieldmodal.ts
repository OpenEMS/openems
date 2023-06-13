import { Component } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";
import { Utils } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";

@Component({
    selector: 'formly-field-modal',
    templateUrl: './formlyfieldmodal.html'
})
export class FormlyFieldModalComponent extends FieldWrapper {

    protected readonly Role = Role;
    protected readonly Utils = Utils;
}