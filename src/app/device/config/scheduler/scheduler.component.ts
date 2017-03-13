import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';

import { AbstractConfig, ConfigureRequest } from '../abstractconfig';
import { WebsocketService } from '../../../service/websocket.service';
import { Device } from '../../../service/device';

@Component({
  selector: 'app-device-config-scheduler',
  templateUrl: './scheduler.component.html'
})
export class DeviceConfigSchedulerComponent extends AbstractConfig {

  private form: AbstractControl;
  configForm: FormGroup;

  constructor(
    route: ActivatedRoute,
    websocketService: WebsocketService,
    formBuilder: FormBuilder
  ) {
    super(route, websocketService, formBuilder);
  }

  initForm(config) {
    // console.log("CONFIG", JSON.stringify(config.scheduler));
    console.log(config);
    this.configForm = <FormGroup>this.buildForm(config);
    // console.log(this.configForm);
    this.form = this.buildForm(config.scheduler);
  }

  protected getConfigureCreateRequests(form: FormGroup): ConfigureRequest[] {
    return;
  }

  // isArray(value: any) {
  //   if (value instanceof Array) {
  //     return true;
  //   }
  //   return false;
  // }

  addChannelsToScheduler(schedulerForm: FormGroup, $event: any) {
    for (let controlName in schedulerForm.controls) {
      if (controlName != 'id' && controlName != 'class') {
        schedulerForm.removeControl(controlName);
      }
    }

    console.log(schedulerForm);
    let clazz = $event.target.value;

    let schedulerMeta = <FormArray>this.configForm.controls['_meta']['controls']['availableSchedulers'];
    console.log(schedulerMeta);

    for (let indexMeta in schedulerMeta.value) {
      // console.log("First For-Loop // get Index of schedulerMeta");
      if (schedulerMeta.value[indexMeta].class == clazz) {
        // console.log("If statement // if both classes equals");
        for (let indexChannel in schedulerMeta.value[indexMeta].channels) {
          // console.log("Second For-Loop // get channel of schedulerMeta");

          let channelName = schedulerMeta.value[indexMeta].channels[indexChannel].name;

          if (schedulerMeta.value[indexMeta].channels[indexChannel].type == 'JsonArray') {
            schedulerForm.addControl(channelName, this.formBuilder.array([]));
          } else {
            schedulerForm.addControl(channelName, this.formBuilder.control(""));
          }

          // schedulerForm.addControl(channelName, this.formBuilder.control(""));

        }

        break;
      }
    }
    schedulerForm["_scheduler_new"] = true;

    console.log(schedulerForm);
  }

}