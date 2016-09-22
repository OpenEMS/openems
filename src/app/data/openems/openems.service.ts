import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Headers, Http, Response } from '@angular/http';
import 'rxjs/add/operator/toPromise';

import { DataService } from '../data.service';
import { CurrentData } from '../current-data';

@Injectable()
export class OpenemsService extends DataService {
  private url = 'http://localhost:8084/rest/config';

  constructor(private http: Http) {
    super();
  }

  getPollInterval(): number {
    return 5000;
  }

  getCurrentData(): Promise<CurrentData> {
    let data = new CurrentData({
      soc: Math.floor(Math.random() * 100)  
    });
    return Promise.resolve(data); 
  }
  
  getConfig(): Promise<JSON> {
    return Promise.resolve(`
      {
        "channel": {
          "usb0": {
            "type": "io.openems.channel.modbus.ModbusRtuConnection",
            "serialinterface": "/dev/ttyUSB0",
            "baudrate": "38400",
            "databits": 8,
            "parity": "even",
            "stopbits": 1,
            "cycle": 1000
          },
          "lan0": {
            "type": "io.openems.channel.modbus.ModbusTcpConnection",
            "inetAddress": "10.4.0.15",
            "cycle": 1000
          },
          "lan1": {
            "type": "io.openems.channel.modbus.ModbusTcpConnection",
            "inetAddress": "10.4.0.16",
            "cycle": 1000
          },
          "lan2":{
            "type": "io.openems.channel.modbus.ModbusTcpConnection",
            "inetAddress": "192.168.178.178",
            "cycle": 1000
          },
          "lan3":{
            "type": "io.openems.channel.modbus.ModbusTcpConnection",
            "inetAddress": "192.168.?.?",
            "cycle": 1000
          }
        },
        "device": {
          "io0": {
            "type": "io.openems.device.io.Wago",
            "channel": "lan3",
            "modbusUnit": 0
          },
          "ess0": {
            "type": "io.openems.device.ess.commercial.Commercial",
            "channel": "lan0",
            "modbusUnit": 100,
            "minSoc": 15 (%)
          },
          "ess1": {
            "type": "io.openems.device.ess.commercial.Commercial",
            "channel": "lan1",
            "modbusUnit": 100,
            "minSoc": 15 (%)
          },
          "sl0":{
            "type": "io.openems.device.inverter.SolarLog",
            "channel": "lan2",
            "modbusUnit": 1,
            "totalPower": 5000 (W)
          },
          "counter0":{
            "type": "io.openems.device.counter.Socomec",
            "channel": "usb0",
            "modbusUnit": 5
          }
        },
        "controller": {
          "enbag": {
            "type": "io.openems.controller.EnBAGController",
            "chargeFromAc":true,
            "gridCounter": "counter0",
            "ess": ["ess0",“ess1“],
            "essOffGridSwitches":{
              "ess0":"DigitalOutput_1_1",
              „ess1“:“DigitalOutput_1_2“
            },
            "maxGridFeedPower":100,(W)
            "pvOnGridSwitch":"DigitalOutput_2_1",
            "pvOffGridSwitch":"DigitalOutput_2_2",
            "primaryEss":"ess0",
            "io": "io0",
            "solarLog": "sl0"
          }
        },
        "monitor":{
        }
      }
    `);
  }

  postConfig(config: string): Promise<Response> {
    let headers = new Headers({
        'Content-Type': 'application/json'
    });
    let configPost = config.replace(/(\n|\t)/gm,'')
    return this.http
      .post(this.url, config, { headers: headers })
      .toPromise()
      .then(response => response)
      .catch(this.handleError);
  }

  private handleError(error: any): Promise<any> {
    console.error('An error occurred', error);
    return Promise.reject(error.message || error);
  }
}
