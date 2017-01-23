import { Component, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';

import { WebsocketService } from '../../../../../service/websocket.service';
import { AbstractConfigForm } from '../../../abstractconfigform';

@Component({
  selector: 'form-bridge-system',
  templateUrl: './system.component.html',
})
export class FormBridgeSystemComponent extends AbstractConfigForm {

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
