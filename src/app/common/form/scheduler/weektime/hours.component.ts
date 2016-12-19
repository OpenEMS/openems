import { Component, Input } from '@angular/core';
import { FormThingComponent } from '../../formthing.component';
import { FormGroup, FormBuilder, FormArray, FormControl } from '@angular/forms';
import { Connection } from './../../../../service/connection';

@Component({
  selector: 'form-scheduler-weektime-hours',
  templateUrl: './hours.component.html',
})
export class FormSchedulerWeekTimeHoursComponent {

  constructor(
    private formBuilder: FormBuilder
  ) {}

  @Input()
  public connection: Connection;

  @Input()
  public form: FormGroup;

  @Input()
  public day: string;

  public addController(controllers: FormArray) {
    controllers.push(this.formBuilder.control(""));
  }

  public removeController(controllers: FormArray, index: number) {
    controllers.removeAt(index);
    controllers.markAsDirty();
  }

  public addTime(hours: FormArray) {
    hours.push(this.formBuilder.group({
      "time": this.formBuilder.control(""),
      "controllers": this.formBuilder.array([]),
    }));
  }

  public removeTime(hours: FormArray, index: number) {
    hours.removeAt(index);
    hours.markAsDirty();
  }
}
