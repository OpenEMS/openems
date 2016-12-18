import { Component, OnInit } from '@angular/core';
import { ConnectionService, Connection } from './../../service/connection.service';
import { Router, ActivatedRoute, Params } from '@angular/router';
import 'rxjs/add/operator/switchMap';
import { Http, Response, RequestOptions, URLSearchParams, Headers } from '@angular/http';
import { Observable } from 'rxjs/Observable';

import * as moment from 'moment/moment';

const INFLUX_DATE_FORMAT = "YYYY-MM-DD[T]HH:mm:ss[Z]";

class TableData {
  key: string;
  value: any;
};

@Component({
  selector: 'app-monitor-detail',
  templateUrl: './detail.component.html'
})
export class MonitorDetailComponent implements OnInit {

  private connection: Connection;

  public date;
  public datepicker;
  public dateIsCollapsed: boolean = true;

  public lineChartData: Array<any> = [{ data: [], label: 'Ladezustand' }];
  public lineChartLabels: Array<any> = [];
  public lineChartOptions: any = {
    animation: false,
    responsive: true,
    scales: {
      yAxes: [{
        ticks: {
          max: 100,
          min: 0
        }
      }]
    },
  };
  public lineChartColors: Array<any> = [
    { // grey
      backgroundColor: 'rgba(148,159,177,0.2)',
      borderColor: 'rgba(148,159,177,1)',
      pointBackgroundColor: 'rgba(148,159,177,1)',
      pointBorderColor: '#fff',
      pointHoverBackgroundColor: '#fff',
      pointHoverBorderColor: 'rgba(148,159,177,0.8)'
    }
  ];
  public lineChartLegend: boolean = true;
  public lineChartType: string = 'line';

  public tableData: Array<TableData> = [];

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
          if (!this.connection.isConnected || !this.connection.subject) {
            this.router.navigate(['/login']);
          } else {
            this.connection.subject.subscribe(null, null, (/* complete */) => {
              this.router.navigate(['/login']);
            })
          }
        }
      });
    }

    // fill timeseries labels
    for (var i = 0; i < 24; i++) {
      this.lineChartLabels.push(i);
    }

    // init graph for today
    this.datepicker = this.date = moment().startOf('day');
    this.lineChartUpdateData();
  }

  public lineChartUpdateData() {
    var influxdb = this.connection.config.getInfluxdbPersistence();
    if (!influxdb) {
      return;
      //TODO error message
    }
    var date = this.datepicker;
    if (!date["_isAMomentObject"]) {
      date = moment(date);
    }
    this.date = date.startOf('day');
    this.dateIsCollapsed = true;
    var query = "SELECT MEAN(" + '"ess0/Soc"' + ") FROM db..data WHERE fems='" + influxdb.fems + "' AND time >= '" + this.date.format(INFLUX_DATE_FORMAT) + "' GROUP BY time(1h);";

    this.influxQuery(query)
      .subscribe((value: Object) => {
        if ("results" in value && value["results"].length > 0) {

          if ("error" in value["results"][0]) {
            console.error(value["results"][0]["error"]);
            return;

          } else if ("series" in value["results"][0] && value["results"][0]["series"].length > 0) {
            var d = value["results"][0]["series"][0];
            var data = []
            for (let value of d["values"]) {
              data.push(value[1]);
            }
            this.lineChartData = [{ data: data, label: 'Ladezustand' }]
            this.tableData = [];
            return;
          }
        }
        this.lineChartData = [{ data: [], label: 'Ladezustand' }]
        this.tableData = [];
      });
  }

  public lineChartClicked(event) {
    if (event["active"].length > 0 && "_index" in event["active"][0]) {
      // a specific element was clicked:
      var index = event["active"][0]["_index"];
      this.tableUpdateData(index);
    }
  }

  public tableUpdateData(hour: number) {
    var influxdb = this.connection.config.getInfluxdbPersistence();
    if (!influxdb) {
      return;
      //TODO error message
    }
    this.date.hour(hour);
    this.date = moment(this.date);
    var from = moment(this.date).format(INFLUX_DATE_FORMAT);
    var to = moment(this.date).add(60, 'minutes').format(INFLUX_DATE_FORMAT);
    var query = "SELECT MEAN(*) FROM db..data WHERE fems='" + influxdb.fems + "' AND time >= '" + from + "' AND time < '" + to + "';";

    this.influxQuery(query)
      .subscribe((value: Object) => {
        if ("results" in value && value["results"].length > 0) {

          if ("error" in value["results"][0]) {
            console.error(value["results"][0]["error"]);
            return;

          } else if ("series" in value["results"][0] && value["results"][0]["series"].length > 0) {
            var d = value["results"][0]["series"][0];
            var data: TableData[] = [];
            for (let index in d["columns"]) {
              var key = d["columns"][index];
              var datavalue = d["values"][0][index];
              if (datavalue != null) {
                if (key.startsWith("mean_")) {
                  key = key.substr(5);
                  datavalue = Math.round(datavalue);
                }
                data.push({ key: key, value: datavalue });
              }
            }
            this.tableData = data;
            return;
          }
        }
        this.tableData = [];
      });
  }

  public influxQuery(query: string): Observable<Response> {
    var influxdb = this.connection.config.getInfluxdbPersistence();
    if (!influxdb) {
      return;
      //TODO error message
    }
    let headers = new Headers();
    headers.append('Authorization', 'Basic ' + btoa(influxdb.username + ":" + influxdb.password));
    let options = new RequestOptions({
      search: new URLSearchParams("q=" + query),
      headers: headers
    });
    return this.http.get("http://" + influxdb.ip + ":8086/query", options)
      .map(res => res.json());
  }
}
