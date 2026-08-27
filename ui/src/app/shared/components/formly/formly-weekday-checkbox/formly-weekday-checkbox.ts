import { Component, Input, ChangeDetectionStrategy } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FieldType } from "@ngx-formly/core";
import { TranslateModule } from "@ngx-translate/core";

@Component({
    selector: "oe-weekday-checkbox",
    templateUrl: "./formly-weekday-checkbox.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [ReactiveFormsModule, IonicModule, TranslateModule],
})
export class FormlyFieldWeekdaysComponent extends FieldType {
    /** FormGroup */
    @Input({ required: true }) public formGroup!: FormGroup;

    protected readonly days = WEEKDAYS;
}

export const WEEKDAYS = [
    { label: "EDGE.HISTORY.MON", controlName: "monday" },
    { label: "EDGE.HISTORY.TUE", controlName: "tuesday" },
    { label: "EDGE.HISTORY.WED", controlName: "wednesday" },
    { label: "EDGE.HISTORY.THU", controlName: "thursday" },
    { label: "EDGE.HISTORY.FRI", controlName: "friday" },
    { label: "EDGE.HISTORY.SAT", controlName: "saturday" },
    { label: "EDGE.HISTORY.SUN", controlName: "sunday" },
] as const;
