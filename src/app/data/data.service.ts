import { CurrentData } from "./current-data";
import { Observable } from 'rxjs/Observable';
import { IntervalObservable } from 'rxjs/Observable/IntervalObservable';

export abstract class DataService {
  public data: Observable<CurrentData>;

  constructor() {
    this.data = new Observable<CurrentData>(observer => {
      // initialize immediately
      this.getCurrentData()
          .then(data => observer.next(data));
      // start interval
      new IntervalObservable(this.getInterval()).forEach(() => { 
        this.getCurrentData()
          .then(data => observer.next(data));
      })
    });
  }

  abstract getInterval(): number;

  abstract getCurrentData(): Promise<CurrentData>;
}
