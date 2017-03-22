import { Component, Input } from '@angular/core';
import { FormGroup, FormBuilder } from '@angular/forms';

@Component({
  selector: 'form-scheduler',
  templateUrl: './scheduler.component.html',
})
export class FormSchedulerComponent {

  constructor(
    private formBuilder: FormBuilder
  ) {
  }

  @Input()
  private form: FormGroup;

}
