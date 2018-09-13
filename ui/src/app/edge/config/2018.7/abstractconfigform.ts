import { OnInit, OnDestroy } from '@angular/core';
import { FormGroup, FormArray, AbstractControl } from '@angular/forms';
import { BehaviorSubject, Subject } from 'rxjs';


import { Websocket } from '../../../shared/shared';
import { Edge } from '../../../shared/edge/edge';

export type ConfigureRequestModeType = "update" | "create" | "delete";
export class ConfigureRequest {
  mode: ConfigureRequestModeType;
}
export interface ConfigureUpdateRequest extends ConfigureRequest {
  thing: string
  channel: string;
  value: Object;
}
export interface ConfigureCreateRequest extends ConfigureRequest {
  object: Object;
  parent: string;
}
export interface ConfigureDeleteRequest extends ConfigureRequest {
  thing: string;
}
export interface ConfigureUpdateSchedulerRequest extends ConfigureRequest {
  thing: string
  class: string;
  value: Object;
}

export abstract class AbstractConfigForm implements OnDestroy, OnInit {

  public edge: BehaviorSubject<Edge> = new BehaviorSubject<Edge>(null);
  protected ngUnsubscribe: Subject<void> = new Subject<void>();

  constructor(
    protected websocket: Websocket,
  ) { }

  ngOnInit() {
    // TODO
    // this.websocket.currentEdge.takeUntil(this.ngUnsubscribe).subscribe(edge => {
    //   this.edge.next(edge);
    // });
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  /**
   * general save() for whole configuration
   */
  public save(form: AbstractControl): void {
    if (form instanceof FormGroup) {
      let requests;
      if (form["_meta_new"]) {
        requests = this.getConfigureCreateRequests(form);
        form["_meta_new"] = false;
      } else if (form["_scheduler_new"]) {
        requests = this.getConfigureUpdateSchedulerRequests(form);
        form["_scheduler_new"] = false;
      } else {
        requests = this.getConfigureUpdateRequests(form);
      }
      this.send(requests);
      form.markAsPristine();
    }
  }

  protected abstract getConfigureCreateRequests(form: FormGroup): ConfigureRequest[];

  protected send(requests: ConfigureRequest[]) {
    if (requests.length > 0) {
      let edge = this.edge.getValue();
      if (edge != null) {
        edge.send({
          configure: requests
        });
      } else {
        // TODO: error message: no current edge!
      }
    }
  }

  protected delete(form: FormArray, index: number): void {
    if (form.controls[index]["_meta_new"]) {
      // newly created. No need to delete it at server
      form.removeAt(index);
      form.markAsDirty();
    } else {
      let requests = this.getConfigDeleteRequests(form.controls[index]);
      // console.log(requests);
      this.send(requests);
      form.markAsPristine();
    }
  }

  protected getConfigDeleteRequests(form: AbstractControl): ConfigureRequest[] {
    let requests: ConfigureRequest[] = [];
    if (form instanceof FormGroup) {
      requests.push(<ConfigureDeleteRequest>{
        mode: "delete",
        thing: form.controls["id"].value
      });
    }

    return requests;
  }

  protected getConfigureUpdateRequests(form: AbstractControl): ConfigureRequest[] {
    let requests: ConfigureRequest[] = [];
    if (form instanceof FormGroup) {
      let formControl = form.controls;
      let id = formControl['id'].value;
      for (let key in formControl) {
        if (formControl[key].dirty) {
          let value = formControl[key].value;

          requests.push(<ConfigureUpdateRequest>{
            mode: "update",
            thing: id,
            channel: key,
            value: value
          });
        }
      }
    }

    // console.log(requests);
    return requests;
  }

  protected getConfigureUpdateSchedulerRequests(form: AbstractControl): ConfigureRequest[] {
    let requests: ConfigureRequest[] = [];
    let builder: Object = {};
    if (form instanceof FormGroup) {
      let formControl = form.controls;
      let id = formControl['id'].value;
      for (let key in formControl) {
        if (formControl[key].dirty) {
          // console.log(formControl[key]);
          let value = formControl[key].value;

          if (key != 'class') {
            builder[key] = value;
          }
        }
      }
      requests.push(<ConfigureUpdateSchedulerRequest>{
        mode: "update",
        thing: id,
        class: formControl['class'].value,
        value: builder
      });
    }

    // console.log(requests);
    return requests;
  }

  protected buildValue(form: FormGroup): Object {
    let builder: Object = {};
    for (let key in form.controls) {
      builder[key] = form.controls[key].value;
    }
    return builder;
  }

  /**
   * sets class empty to enable selection of another scheduler
   */
  public createNewScheduler(schedulerForm: AbstractControl) {
    if (schedulerForm instanceof FormGroup) {
      schedulerForm.controls['class'].setValue("");
      schedulerForm.markAsDirty();
    }
  }
}