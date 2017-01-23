import { Component, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';

import { WebsocketService } from '../../../../../service/websocket.service';
import { AbstractConfigForm } from '../../../abstractconfigform';

@Component({
  selector: 'form-bridge-simulator',
  templateUrl: './simulator.component.html',
})
export class FormBridgeSimulatorComponent extends AbstractConfigForm {

  constructor(
    websocketService: WebsocketService
  ) {
    super(websocketService);
  }

  @Input()
  set form(form: FormGroup) {
    super.setForm(form, ["id", "class"]);
  }
}
