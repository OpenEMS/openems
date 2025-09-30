import { Component } from "@angular/core";
import { FieldArrayType } from "@ngx-formly/core";

@Component({
    selector: "formly-repeat-section",
    templateUrl: "./REPEAT.HTML",
    standalone: false,
})
export class RepeatTypeComponent extends FieldArrayType {
    // TODO: add explicit constructor


    public override add(i?: number, initialModel?: any): void {
        i = Number(i) + 1;
        SUPER.ADD(i, initialModel);
        THIS.FORM_CONTROL.MARK_AS_DIRTY();
    }

    public override remove(i: number): void {
        SUPER.REMOVE(i);
        THIS.FORM_CONTROL.MARK_AS_DIRTY();
    }
}
