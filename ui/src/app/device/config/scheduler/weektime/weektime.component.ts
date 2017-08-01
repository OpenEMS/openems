import { Component, Input } from '@angular/core';
import { AbstractControl, FormArray, FormGroup, FormBuilder } from '@angular/forms';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { WebsocketService } from '../../../../shared/shared';
import { AbstractConfig, ConfigureRequest, ConfigureUpdateRequest, ConfigureDeleteRequest } from '../../abstractconfig';
import { AbstractConfigForm } from '../../abstractconfigform';

interface Day {
  label: string;
  key: string;
}

@Component({
  selector: 'weektime',
  templateUrl: './weektime.component.html',
})
export class WeekTimeComponent extends AbstractConfigForm {

  public configForm: FormGroup;
  public config: FormGroup;

  constructor(
    public websocketService: WebsocketService,
    private formBuilder: FormBuilder,
    private translate: TranslateService
  ) {
    super(websocketService);
  }

  public days: Day[] = [{
    label: this.translate.instant('ConfigScheduler.Weektime.Monday'),
    key: "monday"
  }, {
    label: this.translate.instant('ConfigScheduler.Weektime.Tuesday'),
    key: "tuesday"
  }, {
    label: this.translate.instant('ConfigScheduler.Weektime.Wednesday'),
    key: "wednesday"
  }, {
    label: this.translate.instant('ConfigScheduler.Weektime.Thursday'),
    key: "thursday"
  }, {
    label: this.translate.instant('ConfigScheduler.Weektime.Friday'),
    key: "friday"
  }, {
    label: this.translate.instant('ConfigScheduler.Weektime.Saturday'),
    key: "saturday"
  }, {
    label: this.translate.instant('ConfigScheduler.Weektime.Sunday'),
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

  public removeHour(dayForm: FormArray, hourIndex: number) {
    dayForm.removeAt(hourIndex);
    dayForm.markAsDirty();
  }

  public getConfigDeleteRequests(form: AbstractControl): ConfigureRequest[] {
    let requests: ConfigureRequest[] = [];
    if (form instanceof FormGroup) {
      requests.push(<ConfigureDeleteRequest>{
        mode: "delete",
        thing: form.controls["time"].value
      });
    }

    return requests;
  }

  public addHour(dayForm: FormArray) {
    dayForm.push(this.formBuilder.group({
      "time": this.formBuilder.control(""),
      "controllers": this.formBuilder.array([])
    }))

    dayForm.markAsDirty();
  }

  public addControllerToHour(dayForm: FormArray, hourIndex: number) {
    let controllers = <FormArray>dayForm.controls[hourIndex]["controls"]["controllers"];
    controllers.push(
      this.formBuilder.control("")
    );

    dayForm.markAsDirty();
  }

  public removeControllerFromHour(dayForm: FormArray, hourIndex: number, controllerIndex: number) {
    let controllers = <FormArray>dayForm.controls[hourIndex]["controls"]["controllers"];
    controllers.removeAt(controllerIndex);
    dayForm.markAsDirty();
  }

  public addControllerToAlways() {
    let controllers = <FormArray>this.configForm.controls["always"];
    controllers.push(
      this.formBuilder.control("")
    );
    controllers.markAsDirty();
  }

  public removeControllerFromAlways(controllerIndex: number) {
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