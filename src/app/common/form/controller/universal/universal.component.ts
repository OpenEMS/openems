import { Component, Input } from '@angular/core';
import { FormThingComponent } from '../../formthing.component';
import { FormGroup, FormBuilder } from '@angular/forms';
import { Connection } from './../../../../service/connection';

@Component({
  selector: 'form-controller-universal',
  templateUrl: './universal.component.html',
})
export class FormControllerUniversalComponent extends FormThingComponent {

  constructor(
    private formBuilder: FormBuilder
  ) {
    super(formBuilder);
  }

  @Input()
  set thing(thing: Object) {
    this.buildForm(thing, ["port"]);
  };

  @Input()
  set connection(connection: Connection) {
    this._connection = connection;
  }
}
