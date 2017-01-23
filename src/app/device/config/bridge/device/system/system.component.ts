import { Component, Input } from '@angular/core';
import { FormGroup, FormBuilder } from '@angular/forms';

import { WebsocketService } from '../../../../../service/websocket.service';
import { AbstractConfigForm } from '../../../abstractconfigform';

@Component({
  selector: 'form-device-system',
  templateUrl: './system.component.html',
})
export class FormDeviceSystemComponent extends AbstractConfigForm {

  constructor(
    websocketService: WebsocketService,
    private formBuilder: FormBuilder
  ) {
    super(websocketService);
  }

  @Input()
  set form(form: FormGroup) {
    let ignore: string[] = ["id", "class"];
    if (!form.value["system"]) {
      form.addControl("system", this.formBuilder.group({
        id: this.formBuilder.control("")
      }));
    } else {
      ignore.push("system.id");
    }
    super.setForm(form, ignore);
  }
}