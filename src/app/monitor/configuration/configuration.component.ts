import { Component, OnInit } from '@angular/core';
import { ConnectionService, Connection } from './../../service/connection.service';
import { Notification } from './../../service/notification';
import { OpenemsConfig } from './../../service/config';
import { Router, ActivatedRoute, Params } from '@angular/router';
import 'rxjs/add/operator/switchMap';
import { Http, Response, RequestOptions, URLSearchParams, Headers } from '@angular/http';
import { Observable } from 'rxjs/Observable';
import { FormGroup, FormControl, FormArray, FormBuilder, Validators } from '@angular/forms';

export interface User {
  name: string; // required with minimum 5 chracters
  address?: {
    street?: string; // required
    postcode?: string;
  }
}

type ConfigRequestType = "update" | "create" | "delete";

interface ConfigRequest {
  operation: string;
}

interface ConfigCreateRequest extends ConfigRequest {
  object: Object;
  path: string[];
}

interface ConfigUpdateRequest extends ConfigRequest {
  thing: string
  channel: string;
  value: Object;
}

interface ConfigDeleteRequest extends ConfigRequest {
  thing: string;
}

interface ConfigPath {
  type: "controller" | "scheduler";
  thing: string;
}

interface Controller {
  class: string;
  id: string;
  _collapsed: boolean;
}

class TableData {
  key: string;
  value: any;
}

@Component({
  selector: 'app-configuration',
  templateUrl: './configuration.component.html'
})
export class ConfigurationComponent implements OnInit {

  private form: FormGroup;

  private configRequests: ConfigRequest[];

  public submitted: boolean; // keep track on whether form is submitted
  public events: any[] = []; // use later to display form changes

  private thing: string;
  private channel: string;
  private value: string;

  private connection: Connection;

  constructor(
    private route: ActivatedRoute,
    private connectionService: ConnectionService,
    private router: Router,
    private http: Http,
    private _fb: FormBuilder
  ) { }

  ngOnInit() {
    if (!(this.route.params)) {
      this.router.navigate(['/login']);
    } else {
      this.route.params.subscribe((params: Params) => {
        if (!("name" in params)) {
          this.router.navigate(['/login']);
        } else {
          this.connection = this.connectionService.connections[params['name']];
          if (this.connection.isConnected) {
            this.onConnectionEvent();
          } else {
            this.connection.event.subscribe((value: Notification) => {
              if (this.connection.isConnected) {
                this.onConnectionEvent();
              }
            });
          }
        }
      });
    }
  }

  private onConnectionEvent() {
    this.form = this.buildFormGroup(this.connection.config);
    console.log("FORM", this.form);
    this.connection.subject.subscribe(null, null, (/* complete */) => {
      this.router.navigate(['/login']);
    })
  }

  public setChannel() {
    this.connection.send({
      config: {
        thing: this.thing,
        channel: this.channel,
        operation: "update",
        value: this.value
      }
    })
  }

  public newController(controllers: Controller[]) {
    controllers.push({
      class: "",
      id: "",
      _collapsed: true
    });
  }

  private save() {
    var configRequests = this.getRequests(this.form);
    console.log("SAVE", configRequests, this.form);
    if (configRequests.length > 0) {
      this.connection.send({
        config: configRequests
      })
    }
    // TODO: clear dirty flag
  }

  private getRequests(form: FormGroup | FormArray, path?: string[]): ConfigRequest[] {
    if (!path) {
      path = [];
    }
    var configRequests: ConfigRequest[] = [];
    for (let channel in form.controls) {
      var control = form.controls[channel];
      if (control.dirty) {
        if (control instanceof FormGroup || control instanceof FormArray) {
          if (control["_new"]) {
            // New Thing
            configRequests.push(<ConfigCreateRequest>{
              operation: "create",
              object: control.value,
              path: path
            });
          } if (control["_deleted"]) {
            // Delete Thing
            configRequests.push(<ConfigDeleteRequest>{
              operation: "delete",
              thing: control.value.id
            });
            if (form instanceof FormGroup) {
              form.removeControl(channel);
            } else {
              form.removeAt(Number.parseInt(channel));
            }
          } else {
            // Update existing thing
            path.unshift(channel);
            configRequests = configRequests.concat(this.getRequests(control, path));
          }
        } else {
          // => FormControl
          var parent = control.parent;
          if (parent instanceof FormArray) {
            if (control["_deleted"]) {
              parent.removeAt(Number.parseInt(channel));
              if (parent.parent.value["time"] && parent.parent.value["controllers"]) {
                // WeekTimeSchedule -> delete controller
                var day = path[2];
                console.log(control);
                configRequests.push(<ConfigUpdateRequest>{
                  operation: "update",
                  thing: "_scheduler0",
                  channel: day,
                  value: this.form.value["scheduler"][day]
                });
              }
            }
          } else if (parent instanceof FormGroup) {
            if (parent.value["id"]) {
              configRequests.push(<ConfigUpdateRequest>{
                operation: "update",
                thing: parent.value.id,
                channel: channel,
                value: control._value,
              });
            }
          }
        }
      }
    }
    return configRequests;
  }

  private buildFormControl(item: Object): FormControl {
    return this._fb.control(item);
  }

  private buildFormArray(array: any[]): FormArray {
    var builder: any[] = [];
    for (let item of array) {
      var control = this.buildForm(item);
      if (control) {
        builder.push(control);
      }
    }
    return this._fb.array(builder);
  }

  private buildFormGroup(object: Object): FormGroup {
    var builder: Object = {};
    for (let property in object) {
      var control = this.buildForm(object[property]);
      if (control) {
        builder[property] = control;
      }
    }
    return this._fb.group(builder);
  }

  private buildForm(item: any): FormControl | FormGroup | FormArray {
    if (typeof item === "function") {
      // ignore
    } else if (item instanceof Array) {
      return this.buildFormArray(item);
    } else if (item instanceof Object) {
      return this.buildFormGroup(item);
    } else {
      return this.buildFormControl(item);
    }
  }
}
