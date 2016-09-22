import { Injectable } from '@angular/core';
import { DataService } from '../data-service';
import { CurrentData } from '../current-data';

@Injectable()
export class OpenemsService implements DataService {

  constructor() { }

  getCurrentData(): Promise<CurrentData> {
    let data = new CurrentData({
      soc: Math.floor(Math.random() * 100)  
    });
    return Promise.resolve(data);
  }
  
}
