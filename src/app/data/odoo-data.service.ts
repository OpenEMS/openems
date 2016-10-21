import { Injectable } from '@angular/core';
import { OdooRPCService } from 'angular2-odoo-jsonrpc';

import { DataService } from './data.service';

@Injectable()
export class OdooDataService extends DataService {

  constructor(private odooRPC: OdooRPCService) {
    super();
    this.odooRPC.init({
      odoo_server: "https://fenecon.de:446"
    });
    this.odooRPC.login('FENECON_20161006_1014', 'demo@fenecon.de', 'femsdemo').then(res => {
      /* Object { username: "demo@fenecon.de", user_context: Object {lang,tz}, uid: 18, db: "FENECON", company_id: 1, session_id: "d261030aff9bfcd60aaccce164a400f861b…" } */
      console.log('OdooDataService: login success');
    });
  }

  public getDevices(): Promise<any> {
    return this.odooRPC.searchRead('fems.device', '', ['id', 'soc', 'name', 'lastmessage', 'name_number']);
  }

  public getOne(name_number: number, fields: string[]): Promise<any> {
    /*this.odooRPC.searchRead('fems.device', [['id','=',id]], ['id','name_number']).then(res => {
      let name_number = res.records[0].name_number;*/
      var query = "SELECT";
      for(let field of fields) {
        query += " LAST(" + field + ") AS " + field;
      }
      query += " FROM dess WHERE time > now() - 10m AND fems = '" + name_number + "'";
      return this.odooRPC.sendRequest("/rest/influx/query", { queries: {
        current: query
      }});
    /*}).catch(res => {
      console.log(res);
      return Promise.resolve(res);
    });*/
  }
}
