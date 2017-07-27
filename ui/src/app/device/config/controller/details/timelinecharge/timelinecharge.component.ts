import { Component, Input } from '@angular/core';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';

import { Controller } from '../../controller';

@Component({
    selector: 'timelinecharge',
    templateUrl: './timelinecharge.component.html'
})

export class TimelineChargeComponent {

    @Input()
    public controller: Controller;

    constructor(
        private formBuilder: FormBuilder
    ) { }

    public days: string[] = [
        "monday",
        "tuesday",
        "wednesday",
        "thursday",
        "friday",
        "saturday",
        "sunday"
    ];

    public addToDay(day: string) {
        let form = <FormArray>this.controller.form.controls[day];
        form.push(this.formBuilder.group({
            "time": this.formBuilder.control(""),
            "soc": this.formBuilder.control("")
        }));
        form.markAsDirty();
    }

    public deleteFromDay(day: string, index: number) {
        let form = <FormArray>this.controller.form.controls[day];
        form.removeAt(index);
        form.markAsDirty();
    }
}