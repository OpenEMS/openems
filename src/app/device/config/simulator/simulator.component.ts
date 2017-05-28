import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';

import { WebsocketService, Device, Log } from '../../../shared/shared';
import { AbstractConfig, ConfigureRequest } from '../abstractconfig';

import * as moment from 'moment';

interface SimulatorForm {
  id: string,
  class: string,
  gridMeter: FormGroup,
  productionMeter: FormGroup
}

@Component({
  selector: 'simulator',
  templateUrl: './simulator.component.html'
})
export class SimulatorComponent extends AbstractConfig {

  private forms: SimulatorForm[] = [];

  constructor(
    route: ActivatedRoute,
    websocketService: WebsocketService,
    formBuilder: FormBuilder
  ) {
    super(route, websocketService, formBuilder);
  }

  initForm(config) {
    // finds all Simulator device configurations and adds them to "forms"
    if (config["things"]) {
      for (let bridge of config.things) {
        if (bridge["class"] && bridge.class == 'io.openems.impl.protocol.simulator.SimulatorBridge' && bridge["devices"]) {
          for (let simulator of bridge.devices) {
            if (simulator["class"] && simulator.class == 'io.openems.impl.device.simulator.Simulator' && simulator["gridMeter"] && simulator["productionMeter"]) {
              this.forms.push({
                id: simulator.id,
                class: simulator.class,
                gridMeter: <FormGroup>this.buildForm(simulator.gridMeter),
                productionMeter: <FormGroup>this.buildForm(simulator.productionMeter)
              });
            }
          }
        }
      }
    }
  }

  protected getConfigureCreateRequests(form: FormGroup): ConfigureRequest[] {
    return;
  }
}