import { Component, Input } from '@angular/core';
import { FormGroup, FormBuilder } from '@angular/forms';

import { WebsocketService } from '../../../../../service/websocket.service';
import { AbstractConfigForm } from '../../../abstractconfigform';
import { WebappService, Notification } from '../../../../../service/webapp.service';

@Component({
  selector: 'form-device-simulator',
  templateUrl: './simulator.component.html',
})
export class FormDeviceSimulatorComponent extends AbstractConfigForm {

  constructor(
    websocketService: WebsocketService,
    private formBuilder: FormBuilder,
    private webapp: WebappService
  ) {
    super(websocketService);
  }

  @Input()
  set form(form: FormGroup) {
    let ignore: string[] = ["id", "class"];
    if (!form.value["ess"]) {
      form.addControl("ess", this.formBuilder.group({
        id: this.formBuilder.control(""),
        minSoc: this.formBuilder.control(""),
        chargeSoc: this.formBuilder.control("")
      }));
    } else {
      ignore.push("ess.id");
    }
    if (!form.value["meter"]) {
      form.addControl("meter", this.formBuilder.group({
        id: this.formBuilder.control("")
      }));
    } else {
      ignore.push("meter.id");
    }
    super.setForm(form, ignore);
  }
}
