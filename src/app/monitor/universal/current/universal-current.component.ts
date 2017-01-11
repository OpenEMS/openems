import { Component, OnInit, OnDestroy } from '@angular/core';
import { Http, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Observable';
import { ISubscription } from 'rxjs/Subscription';
import { Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';

@Component({
  selector: 'app-monitor-test-current',
  templateUrl: './universal-current.component.html'
})
export class MonitorUniversalCurrentComponent implements OnInit, OnDestroy {
  private data: Object;
  private error: string;
  private subscription: ISubscription;

  constructor(
    private router: Router) {
  }

  ngOnInit() {
    /*var connection: Connection = this.connectionService.getDefault();
    if (!(connection instanceof ActiveConnection)) {
      this.router.navigate(['login']);

    } else {
      connection.subject.subscribe((message: any) => {
        if ("data" in message) {
          var msg: any = JSON.parse(message.data);

          if ("authenticate" in msg && "failed" in msg.authenticate && msg.authenticate.failed == true) {
            // Authentication failed
            this.data = null;
            this.error = "Authentifizierung fehlgeschlagen.";
            setTimeout(() => this.router.navigate(['login']), 1000);
            return;
          }

          // Data
          if ("data" in msg) {
            this.data = msg.data;
            this.error = null;


      }, (error: any) => {
        this.data = null;
        this.error = "Herstellen der Verbindung ist nicht möglich.";
        setTimeout(() => this.router.navigate(['login']), 1000);
      }, (/* complete *//*) => {
this.data = null;
setTimeout(() => this.router.navigate(['login']), 1000);
});
}*/
  }

  ngOnDestroy() {
    //TODO this.unsubscribeNatures();
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  private contains(array: string[], tag: string): boolean {
    return array.indexOf(tag) != -1
  }
}
