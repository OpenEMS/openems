import { Component, Input } from '@angular/core';
import { FormGroup, FormBuilder } from '@angular/forms';

import { WebsocketService } from '../../../../../service/websocket.service';
import { AbstractConfigComponent } from '../../abstractformconfig.component';

@Component({
  selector: 'form-device-system',
  templateUrl: './system.component.html',
})
export class FormDeviceSystemComponent extends AbstractConfigComponent {

  constructor(
    private _websocketService: WebsocketService,
    private formBuilder: FormBuilder
  ) {
    super(_websocketService);
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