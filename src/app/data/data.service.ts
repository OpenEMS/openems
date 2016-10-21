import { CurrentData } from "./current-data";
import { Observable } from 'rxjs/Observable';
import { IntervalObservable } from 'rxjs/Observable/IntervalObservable';

export abstract class DataService {
  public data: Observable<CurrentData>;

  constructor() {
    /*this.data = new Observable<CurrentData>(observer => {
      // initialize immediately
      this.getCurrentData()
          .then(data => observer.next(data));
      // start interval
      new IntervalObservable(this.getPollInterval()).forEach(() => { 
        this.getCurrentData()
          .then(data => observer.next(data));
      })
    });*/
  }

  //abstract getPollInterval(): number;

  //abstract getCurrentData(): Promise<CurrentData>;

  abstract getDevices(): Promise<any>;

  abstract getOne(name_number: number, fields: string[]): Promise<any>;

  /*abstract getPeriod(fields: string[], fromTime: long ): Object;*/
}
