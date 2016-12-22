import { Component, Input } from '@angular/core';
import { FormThingComponent } from '../../formthing.component';
import { FormGroup, FormBuilder } from '@angular/forms';

@Component({
  selector: 'form-controller-new',
  templateUrl: './new.component.html',
})
export class FormControllerNewComponent extends FormThingComponent {

  @Input()
  set form(form: FormGroup) {
    super.setForm(form, []);
  }

}
