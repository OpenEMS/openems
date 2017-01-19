import { Component, OnInit } from '@angular/core';
import { FormsModule }   from '@angular/forms';
import { Headers, Http, Response } from '@angular/http';
import 'rxjs/add/operator/toPromise';

@Component({
  selector: 'app-openems-setting',
  templateUrl: './openems-setting.component.html',
  styleUrls: ['./openems-setting.component.css']
})
export class OpenemsSettingComponent implements OnInit {
  config: JSON;
  expertMode: false;
  statusError: {
    title: string,
    message: string
  };
  statusMessage: string;
  private url = '';
  //private url = 'http://localhost:80';

  constructor(private http: Http) { }

  ngOnInit() {
    this.getConfig();
  }

  resetConfig(): void {
    this.resetStatus();
    this.getConfig();
  }

  public getConfig(): void {
    this.resetStatus();
    this.http
      .get(this.url + '/rest/config')
      .toPromise()
      .then(result => {
        console.log(result);
        this.config = result.json();
      })
      .catch(this.handleError);
  }

  public postConfig(): void {
    this.resetStatus();
    let headers = new Headers({
        'Content-Type': 'application/json'
    });
    //let configPost = this.config.replace(/(\n|\t)/gm,'')
    this.http
      .post(this.url + '/rest/config', this.config, { headers: headers })
      .toPromise()
      .then(success => this.handleSuccess())
      .catch(error => this.handleError(error));
    } 

  private resetStatus() {
    this.statusError = null;
    this.statusMessage = null;
  }

  private handleSuccess(): void {
    console.log("saved")
    this.statusMessage = "Saved changes!"
  }

  private handleError(error: any): void {
    this.statusError = {
      title: "Error",
      message: JSON.stringify(error, null, '\t')
    };
  }
}
