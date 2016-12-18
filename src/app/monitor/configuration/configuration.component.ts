import { Component, OnInit } from '@angular/core';
import { ConnectionService, Connection } from './../../service/connection.service';
import { Router, ActivatedRoute, Params } from '@angular/router';
import 'rxjs/add/operator/switchMap';
import { Http, Response, RequestOptions, URLSearchParams, Headers } from '@angular/http';
import { Observable } from 'rxjs/Observable';
import { FormGroup, FormControl, FormBuilder, Validators } from '@angular/forms';

export interface User {
  name: string; // required with minimum 5 chracters
  address?: {
    street?: string; // required
    postcode?: string;
  }
}

class TableData {
  key: string;
  value: any;
};

@Component({
  selector: 'app-configuration',
  templateUrl: './configuration.component.html'
})
export class ConfigurationComponent implements OnInit {

  public controllerForms: FormGroup[] = []
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
            this.connection.event.subscribe((value: string) => {
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
    this.buildControllerForms();
    this.connection.subject.subscribe(null, null, (/* complete */) => {
      this.router.navigate(['/login']);
    })
  }

  private buildControllerForms() {
    this.controllerForms = [];
    for (let controller of this.connection.config.scheduler.controllers) {
      var controlsConfig: { [key: string]: any; } = {};
      for(let channel in controller) {
        controlsConfig[channel] = [controller[channel], [<any>Validators.required]];
      }
      this.controllerForms.push(
        this._fb.group(controlsConfig)
      );
    }
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

}
