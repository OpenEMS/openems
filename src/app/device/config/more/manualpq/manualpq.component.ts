import { Component, Input } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';

import { WebsocketService, WebappService, Device } from '../../../../shared/shared';

@Component({
  selector: 'manualpq',
  templateUrl: './manualpq.component.html'
})
export class ManualpqComponent {

  constructor(
    private formBuilder: FormBuilder
  ) { }

  @Input()
  public device: Device;

  public manualPQForm: FormGroup;

  ngOnInit() {
    this.manualPQForm = this.formBuilder.group({
      "p": this.formBuilder.control(''),
      "q": this.formBuilder.control('')
    });
  }

  public applyManualPQ(thing: string, form: FormGroup) {
    this.device.send({
      system: {
        mode: "manualpq",
        active: true,
        ess: thing,
        p: form.value["p"] ? form.value["p"] : 0,
        q: form.value["q"] ? form.value["q"] : 0
      }
    });
  }

  public removeManualPQ(thing: string) {
    this.device.send({
      system: {
        mode: "manualpq",
        active: false,
        ess: thing
      }
    });
  }
}