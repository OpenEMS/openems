import { Component, Input } from '@angular/core';
import { AbstractControl, FormArray, FormGroup, FormBuilder } from '@angular/forms';
import { Router, ActivatedRoute, Params } from '@angular/router';

import { WebsocketService } from '../../../../shared/shared';
import { AbstractConfig, ConfigureRequest, ConfigureUpdateRequest, ConfigureDeleteRequest } from '../../abstractconfig';
import { AbstractConfigForm } from '../../abstractconfigform';

interface Day {
  label: string;
  key: string;
}

@Component({
  selector: 'scheduler-weektime',
  templateUrl: './weektime.component.html',
})
export class FormSchedulerWeekTimeComponent extends AbstractConfigForm {
  configForm: FormGroup;
  // formBuilder: FormBuilder;
  config: FormGroup;

  constructor(
    websocketService: WebsocketService,
    private formBuilder: FormBuilder
  ) {
    super(websocketService);
  }

  private days: Day[] = [{
    label: "Montag",
    key: "monday"
  }, {
    label: "Dienstag",
    key: "tuesday"
  }, {
    label: "Mittwoch",
    key: "wednesday"
  }, {
    label: "Donnerstag",
    key: "thursday"
  }, {
    label: "Freitag",
    key: "friday"
  }, {
    label: "Samstag",
    key: "saturday"
  }, {
    label: "Sonntag",
    key: "sunday"
  }]

  @Input()
  set form(form: FormGroup) {
    this.config = form;
    this.configForm = <FormGroup>form.controls['scheduler'];
    let ignore: string[] = ["id", "class"];
    for (let day of this.days) {
      if (!this.configForm.value[day.key]) {
        this.configForm.addControl(day.key, this.formBuilder.array([
          this.formBuilder.group({
            time: this.formBuilder.control(""),
            controllers: this.formBuilder.array([])
          })
        ]))
      }
    }

    if (!this.configForm.value["always"]) {
      this.configForm.addControl("always", this.formBuilder.array([]));
    }
  }

  removeHour(dayForm: FormArray, hourIndex: number) {
    dayForm.removeAt(hourIndex);
    dayForm.markAsDirty();
  }

  protected getConfigDeleteRequests(form: AbstractControl): ConfigureRequest[] {
    let requests: ConfigureRequest[] = [];
    if (form instanceof FormGroup) {
      requests.push(<ConfigureDeleteRequest>{
        mode: "delete",
        thing: form.controls["time"].value
      });
    }

    return requests;
  }

  addHour(dayForm: FormArray) {
    dayForm.push(this.formBuilder.group({
      "time": this.formBuilder.control(""),
      "controllers": this.formBuilder.array([])
    }))

    dayForm.markAsDirty();
  }

  addControllerToHour(dayForm: FormArray, hourIndex: number) {
    let controllers = <FormArray>dayForm.controls[hourIndex]["controls"]["controllers"];
    controllers.push(
      this.formBuilder.control("")
    );

    dayForm.markAsDirty();
  }

  addControllerToAlways() {
    let controllers = <FormArray>this.configForm.controls["always"];
    controllers.push(
      this.formBuilder.control("")
    );
    controllers.markAsDirty();
  }

  removeControllerFromHour(dayForm: FormArray, hourIndex: number, controllerIndex: number) {
    let controllers = <FormArray>dayForm.controls[hourIndex]["controls"]["controllers"];
    controllers.removeAt(controllerIndex);
    dayForm.markAsDirty();
  }

  removeControllerFromAlways(controllerIndex: number) {
    let controllers = <FormArray>this.configForm.controls["always"];
    controllers.removeAt(controllerIndex);
    controllers.markAsDirty();
  }

  /**
   * useless, need to be here because it's abstract in superclass
   */
  protected getConfigureCreateRequests(form: FormGroup): ConfigureRequest[] {
    return;
  }

}