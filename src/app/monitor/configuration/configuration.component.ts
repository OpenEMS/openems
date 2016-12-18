import { Component, OnInit } from '@angular/core';
import { ConnectionService, Connection } from './../../service/connection.service';
import { Router, ActivatedRoute, Params } from '@angular/router';
import 'rxjs/add/operator/switchMap';
import { Http, Response, RequestOptions, URLSearchParams, Headers } from '@angular/http';
import { Observable } from 'rxjs/Observable';

class TableData {
  key: string;
  value: any;
};

@Component({
  selector: 'app-configuration',
  templateUrl: './configuration.component.html'
})
export class ConfigurationComponent implements OnInit {

  private thing: string;
  private channel: string;
  private value: string;

  private connection: Connection;

  constructor(
    private route: ActivatedRoute,
    private connectionService: ConnectionService,
    private router: Router,
    private http: Http
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
            this.connection.subject.subscribe(null, null, (/* complete */) => {
              this.router.navigate(['/login']);
            })
          } else {
            this.connection.event.subscribe((value: string) => {
              if (this.connection.isConnected) {
                this.connection.subject.subscribe(null, null, (/* complete */) => {
                  this.router.navigate(['/login']);
                })
              }
            });
          }
        }
      });
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
