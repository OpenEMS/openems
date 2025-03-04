import { Component } from "@angular/core";
import { FieldArrayType } from "@ngx-formly/core";

@Component({
    selector: "formly-repeat-section",
    templateUrl: "./repeat.html",
    standalone: false,
})
export class RepeatTypeComponent extends FieldArrayType {
    // TODO: add explicit constructor


    public override add(i?: number, initialModel?: any): void {
        i = Number(i) + 1;
        super.add(i, initialModel);
        this.formControl.markAsDirty();
    }

    public override remove(i: number): void {
        super.remove(i);
        this.formControl.markAsDirty();
    }
}
