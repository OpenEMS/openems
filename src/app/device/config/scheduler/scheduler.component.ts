import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';

import { WebsocketService, Device } from '../../../shared/shared';
import { AbstractConfig, ConfigureRequest } from '../abstractconfig';

@Component({
  selector: 'scheduler',
  templateUrl: './scheduler.component.html'
})
export class SchedulerComponent extends AbstractConfig {

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
    // console.log(config);
    this.configForm = <FormGroup>this.buildForm(config);
    this.form = this.buildForm(config.scheduler);
  }

  protected getConfigureCreateRequests(form: FormGroup): ConfigureRequest[] {
    return;
  }

  addChannelsToScheduler(schedulerForm: FormGroup, $event: any) {
    for (let controlName in schedulerForm.controls) {
      if (controlName != 'id' && controlName != 'class' && controlName != 'controllers') {
        schedulerForm.removeControl(controlName);
      }
    }

    let clazz = $event.target.value;

    let schedulerMeta = <FormArray>this.configForm.controls['_meta']['controls']['availableSchedulers'];

    for (let indexMeta in schedulerMeta.value) {
      if (schedulerMeta.value[indexMeta].class == clazz) {
        for (let indexChannel in schedulerMeta.value[indexMeta].channels) {

          let channelName = schedulerMeta.value[indexMeta].channels[indexChannel].name;

          if (schedulerMeta.value[indexMeta].channels[indexChannel].type == 'JsonArray') {
            schedulerForm.addControl(channelName, this.formBuilder.array([]));
          } else {
            schedulerForm.addControl(channelName, this.formBuilder.control(""));
          }

        }

        break;
      }
    }
    schedulerForm["_scheduler_new"] = true;

    console.log(schedulerForm);
  }

}