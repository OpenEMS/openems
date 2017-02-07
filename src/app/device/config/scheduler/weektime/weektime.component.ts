import { Component, Input } from '@angular/core';
import { AbstractControl, FormArray, FormGroup, FormBuilder } from '@angular/forms';

import { WebsocketService } from '../../../../service/websocket.service';
import { AbstractConfigForm, ConfigureRequest, ConfigureUpdateRequest } from '../../abstractconfigform';

interface Day {
  label: string;
  key: string;
}

@Component({
  selector: 'form-scheduler-weektime',
  templateUrl: './weektime.component.html',
})
export class FormSchedulerWeekTimeComponent extends AbstractConfigForm {

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
    let ignore: string[] = ["id", "class"];
    super.setForm(form, ignore);
  }

  removeHour(dayForm: FormArray, hourIndex: number) {
    dayForm.removeAt(hourIndex);
    dayForm.markAsDirty();
  }

  addHour(dayForm: FormArray) {
    dayForm.push(
      this.formBuilder.group({
        "time": this.formBuilder.control(""),
        "controllers": this.formBuilder.array([])
      })
    )
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
    let controllers = <FormArray>this._form.controls["always"];
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
    let controllers = <FormArray>this._form.controls["always"];
    controllers.removeAt(controllerIndex);
    controllers.markAsDirty();
  }

  protected save(form: FormGroup) {
    let requests: ConfigureRequest[] = [];
    for (let controlName in form.controls) {
      let control = form.controls[controlName];
      if (control.dirty) {
        let request = <ConfigureUpdateRequest>{
          mode: "update",
          thing: this._form.controls["id"].value,
          channel: controlName,
          value: control.value
        };
        requests.push(request);
      }
    }
    this.send(requests);
    form["_meta_new"] = false;
    form.markAsPristine();
  }
}
