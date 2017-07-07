import { Component, Input } from '@angular/core';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';

interface Day {
    label: string;
    key: string;
}

@Component({
    selector: 'timelinecharge-controller',
    templateUrl: './timelinecharge.component.html'
})

export class TimelineChargeComponent {
    public _form: FormGroup;

    constructor(
        private formBuilder: FormBuilder
    ) { }

    @Input()
    set form(value: FormGroup) {
        this._form = value;
        console.log(this._form);
    }

    @Input()
    public index: number;

    public days: Day[] = [{
        label: "Montag",
        key: "monday"
    }, {
        label: "Dienstag",
        key: "tuesday"
    }, {
        label: "Mittwoch",
        key: "wednesday"
    }, {
        label: "Donnerstag",
        key: "thursday"
    }, {
        label: "Freitag",
        key: "friday"
    }, {
        label: "Samstag",
        key: "saturday"
    }, {
        label: "Sonntag",
        key: "sunday"
    }]

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