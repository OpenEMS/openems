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

    public addTimeline(day: FormArray) {
        day.push(this.formBuilder.group({
            "time": this.formBuilder.control(""),
            "soc": this.formBuilder.control("")
        }));

        day.markAsDirty();
    }

    public deleteTimeline(day: FormArray, index: number) {
        day.removeAt(index);
        day.markAsDirty();
    }

    public deleteCharger(charger: FormArray, index: number) {
        charger.removeAt(index);
        charger.markAsDirty();
    }

    public addCharger(charger: FormArray) {
        charger.push(
            this.formBuilder.control("")
        );
        charger.markAsDirty();
    }

}