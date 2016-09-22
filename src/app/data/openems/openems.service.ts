import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';

import { DataService } from '../data.service';
import { CurrentData } from '../current-data';

@Injectable()
export class OpenemsService extends DataService {

  constructor() {
    super();

  }

  getInterval(): number {
    return 5000;
  }

  getCurrentData(): Promise<CurrentData> {
    let data = new CurrentData({
      soc: Math.floor(Math.random() * 100)  
    });
    return Promise.resolve(data); 
  }
  
}
