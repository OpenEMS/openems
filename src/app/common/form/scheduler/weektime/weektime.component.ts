import { Component, Input } from '@angular/core';
import { FormThingComponent } from '../../formthing.component';
import { FormGroup, FormBuilder, FormArray } from '@angular/forms';
import { Connection } from './../../../../service/connection';

@Component({
  selector: 'form-scheduler-weektime',
  templateUrl: './weektime.component.html',
})
export class FormSchedulerWeekTimeComponent extends FormThingComponent {

  constructor(
    private formBuilder: FormBuilder
  ) {
    super(formBuilder);
  }

  @Input()
  set thing(thing: Object) {
    this.buildForm(thing);
  };

  @Input()
  set connection(connection: Connection) {
    this._connection = connection;
  }

  public addController(controller: FormArray) {
    console.log(controller);
    controller.push(this.formBuilder.control(""));
  }
}
