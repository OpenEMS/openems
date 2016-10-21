import { Component, OnInit, Input } from '@angular/core';

import { OdooRPCService } from 'angular2-odoo-jsonrpc';

@Component({
  selector: 'app-odoo-monitor-one',
  templateUrl: './odoo-monitor-one.component.html',
  styleUrls: ['./odoo-monitor-one.component.css']
})
export class OdooMonitorOneComponent implements OnInit {
  _device: any;  
  @Input('device')
  set in(val) {
    console.log('device = ' + val);
    this._device = val;

    this.odooRPC.sendRequest("/rest/influx/query", { queries: {
      current: {
        query: "SELECT LAST(BSMU_Battery_Stack_Overall_SOC) as Soc FROM dess WHERE time > now() - 10m AND fems = '" + this._device.name.substr(4) + "'"
      }
      /*,
      history: {
        query: "SELECT BSMU_Battery_Stack_Overall_SOC FROM dess WHERE time > now() - 10m AND fems = '20'"
        /*fields: ['BSMU_Battery_Stack_Overall_SOC'],
        timeFrom: '2016-10-09T00:00:01+02:00',
        timeTo: '2016-10-10T00:00:01+02:00',
        fems: '6',
        groupBy: 'time(1h)'
      }*/
    }}).then(res => console.log(res));
  }

  constructor(private odooRPC: OdooRPCService) { }

  ngOnInit() {
    this.odooRPC.init({
      odoo_server: "https://fenecon.de:446"
      //odoo_server: "http://localhost:8069"
    });
    this.odooRPC.isLoggedIn().then(isLoggedIn => {
      if(isLoggedIn) {
        console.log("isLoggedIn: " + isLoggedIn);
      } else {
        console.log("isNotLoggedIn" + isLoggedIn);
      }
    });
  }
}
