import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { ISubscription } from 'rxjs/Subscription';
import { IntervalObservable } from 'rxjs/Observable/IntervalObservable';
import 'rxjs/add/operator/toPromise';
import { Headers, Http, Response } from '@angular/http';

import { CurrentData } from '../../data/current-data';
import { EssData } from '../../data/ess-data';
import { OdooRPCService } from 'angular2-odoo-jsonrpc';

@Component({
  selector: 'app-current-monitor',
  templateUrl: './current-monitor.component.html',
  styleUrls: ['./current-monitor.component.css']
})
export class CurrentMonitorComponent implements OnInit {
  private interval: number = 10000;
  private url = '';
  //private url = 'http://localhost:80';
  private currentData: CurrentData = {
    ess0: {
      soc: null,
      activePower: null,
      reactivePower: null, 
      apparentPower: null,
      gridMode: null
    },
    ess1: {
      soc: null,
      activePower: null,
      reactivePower: null, 
      apparentPower: null,
      gridMode: null
    },
    sl0: {
      pac: null,
      limit: null
    },
    counter0: {
      activePower: null,
      reactivePower: null, 
      apparentPower: null
    },
    io0: {
      digitalOutput_1_1: null,
      digitalOutput_1_2: null,
      digitalOutput_2_1: null,
      digitalOutput_2_2: null
    }
  };

  constructor(private http: Http) {}

  ngOnInit() {
    this.updateData();
    // Start Polling
    new IntervalObservable(this.interval).forEach(() => { 
      this.updateData();
    })
  }

  updateData() {
    // ess0
    this.http.get(this.url + '/rest/device/ess0/current/ActivePower')
      .toPromise()
      .then(result => { this.currentData.ess0.activePower = result.json().value; })
      .catch(this.handleError);
    this.http.get(this.url + '/rest/device/ess0/current/ReactivePower')
      .toPromise()
      .then(result => { this.currentData.ess0.reactivePower = result.json().value; })
      .catch(this.handleError);
    this.http.get(this.url + '/rest/device/ess0/current/ApparentPower')
      .toPromise()
      .then(result => { this.currentData.ess0.apparentPower = result.json().value; })
      .catch(this.handleError);
    this.http.get(this.url + '/rest/device/ess0/current/GridMode')
      .toPromise()
      .then(result => { this.currentData.ess0.gridMode = result.json().value; })
      .catch(this.handleError);
    this.http.get(this.url + '/rest/device/ess0/current/BatteryStringSoc')
      .toPromise()
      .then(result => { this.currentData.ess0.soc = result.json().value; })
      .catch(this.handleError);

    // ess1
    this.http.get(this.url + '/rest/device/ess1/current/ActivePower')
      .toPromise()
      .then(result => { this.currentData.ess1.activePower = result.json().value; })
      .catch(this.handleError);
    this.http.get(this.url + '/rest/device/ess1/current/ReactivePower')
      .toPromise()
      .then(result => { this.currentData.ess1.reactivePower = result.json().value; })
      .catch(this.handleError);
    this.http.get(this.url + '/rest/device/ess1/current/ApparentPower')
      .toPromise()
      .then(result => { this.currentData.ess1.apparentPower = result.json().value; })
      .catch(this.handleError);
    this.http.get(this.url + '/rest/device/ess1/current/GridMode')
      .toPromise()
      .then(result => { this.currentData.ess1.gridMode = result.json().value; })
      .catch(this.handleError);
    this.http.get(this.url + '/rest/device/ess1/current/BatteryStringSoc')
      .toPromise()
      .then(result => { this.currentData.ess1.soc = result.json().value; })
      .catch(this.handleError);

    // Solar-Log
    this.http.get(this.url + '/rest/device/sl0/current/PAC')
      .toPromise()
      .then(result => { this.currentData.sl0.pac = result.json().value; })
      .catch(this.handleError);
    this.http.get(this.url + '/rest/device/sl0/current/GetLimit')
      .toPromise()
      .then(result => { this.currentData.sl0.limit = result.json().value; })
      .catch(this.handleError);

    // counter0
    this.http.get(this.url + '/rest/device/counter0/current/ActivePower')
      .toPromise()
      .then(result => { this.currentData.counter0.activePower = result.json().value; })
      .catch(this.handleError);
    this.http.get(this.url + '/rest/device/counter0/current/ReactivePower')
      .toPromise()
      .then(result => { this.currentData.counter0.reactivePower = result.json().value; })
      .catch(this.handleError);
    this.http.get(this.url + '/rest/device/counter0/current/ApparentPower')
      .toPromise()
      .then(result => { this.currentData.counter0.apparentPower = result.json().value; })
      .catch(this.handleError);

    // io0
    this.http.get(this.url + '/rest/device/io0/current/DigitalOutput_1_1')
      .toPromise()
      .then(result => { this.currentData.io0.digitalOutput_1_1 = result.json().value; })
      .catch(this.handleError);
    this.http.get(this.url + '/rest/device/io0/current/DigitalOutput_1_2')
      .toPromise()
      .then(result => { this.currentData.io0.digitalOutput_1_2 = result.json().value; })
      .catch(this.handleError);
    this.http.get(this.url + '/rest/device/io0/current/DigitalOutput_2_1')
      .toPromise()
      .then(result => { this.currentData.io0.digitalOutput_2_1 = result.json().value; })
      .catch(this.handleError);
    this.http.get(this.url + '/rest/device/io0/current/DigitalOutput_2_2')
      .toPromise()
      .then(result => { this.currentData.io0.digitalOutput_2_2 = result.json().value; })
      .catch(this.handleError);
  }
  
  private handleError(error: any): Promise<any> {
    console.error('An error occurred', error);
    return Promise.reject(error.message || error);
  }
}
