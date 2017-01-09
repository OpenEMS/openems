import { Component, Input } from '@angular/core';
import { FormThingComponent } from '../../formthing.component';
import { FormGroup, FormBuilder, FormArray, FormControl } from '@angular/forms';

@Component({
  selector: 'form-scheduler-weektime-hours',
  templateUrl: './hours.component.html',
})
export class FormSchedulerWeekTimeHoursComponent {

  constructor(
    private formBuilder: FormBuilder
  ) { }

  @Input()
  public form: FormGroup;

  @Input()
  public day: string;

  public addController(controllers: FormArray) {
    var control = this.formBuilder.control("");
    controllers.push(control);
    control.markAsDirty();
  }

  public removeController(controllers: FormArray, index: number) {
    controllers.removeAt(index);
    controllers.markAsDirty();
  }

  public addTime(hours: any) {
    var control = this.formBuilder.group({
      "time": this.formBuilder.control(""),
      "controllers": this.formBuilder.array([]),
    })
    hours.push(control);
    control.markAsDirty();
  }

  public removeTime(hours: FormArray, index: number) {
    hours.removeAt(index);
    hours.markAsDirty();
  }
}
