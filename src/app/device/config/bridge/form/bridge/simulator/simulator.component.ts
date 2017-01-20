import { Component, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';

import { WebsocketService } from '../../../../../../service/websocket.service';
import { AbstractConfigComponent } from '../../abstractformconfig.component';

@Component({
  selector: 'form-bridge-simulator',
  templateUrl: './simulator.component.html',
})
export class FormBridgeSimulatorComponent extends AbstractConfigComponent {

  constructor(
    private _websocketService: WebsocketService
  ) {
    super(_websocketService);
  }

  @Input()
  set form(form: FormGroup) {
    super.setForm(form, ["id", "class"]);
  }
}
