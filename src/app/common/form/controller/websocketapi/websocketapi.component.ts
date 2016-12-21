import { Component, Input } from '@angular/core';
import { FormThingComponent } from '../../formthing.component';
import { FormGroup } from '@angular/forms';

@Component({
  selector: 'form-controller-websocketapi',
  templateUrl: './websocketapi.component.html',
})
export class FormControllerWebsocketApiComponent extends FormThingComponent {

  @Input()
  set form(form: FormGroup) {
    super.setForm(form, []);
  }
  
}
