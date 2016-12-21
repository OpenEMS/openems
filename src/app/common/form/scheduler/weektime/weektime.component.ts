import { Component, Input } from '@angular/core';
import { FormThingComponent } from '../../formthing.component';
import { FormGroup, FormBuilder, FormArray, FormControl } from '@angular/forms';
import { Connection } from './../../../../service/connection';

interface Day {
  label: string;
  key: string;
  active: boolean;
}

@Component({
  selector: 'form-scheduler-weektime',
  templateUrl: './weektime.component.html',
})
export class FormSchedulerWeekTimeComponent extends FormThingComponent {

  @Input()
  private form: FormGroup;

  private days: Day[] = [{
    label: "Montag",
    key: "monday",
    active: true
  }, {
    label: "Dienstag",
    key: "tuesday",
    active: false
  }, {
    label: "Mittwoch",
    key: "wednesday",
    active: false
  }, {
    label: "Donnerstag",
    key: "thursday",
    active: false
  }, {
    label: "Freitag",
    key: "friday",
    active: false
  }, {
    label: "Samstag",
    key: "saturday",
    active: false
  }, {
    label: "Sonntag",
    key: "sunday",
    active: false
  }]

  public setDayActive(thisDay: Day) {
    for (let day of this.days) {
      if (day == thisDay) {
        day.active = true;
      } else {
        day.active = false;
      }
    }
  }
}
