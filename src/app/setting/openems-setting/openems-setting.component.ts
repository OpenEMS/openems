import { Component, OnInit } from '@angular/core';
import { FormsModule }   from '@angular/forms';

import { OpenemsService } from '../../data/openems/openems.service';

@Component({
  selector: 'app-openems-setting',
  templateUrl: './openems-setting.component.html',
  styleUrls: ['./openems-setting.component.css']
})
export class OpenemsSettingComponent implements OnInit {
  config: JSON;
  statusError: {
    title: string,
    message: string
  };
  statusMessage: string;

  constructor(private openemsService: OpenemsService) { }

  ngOnInit() {
    this.getConfig();
  }

  resetConfig(): void {
    this.resetStatus();
    this.getConfig();
  }

  public getConfig(): void {
    this.openemsService.getConfig()
      .then(config => this.config = config);
      //.then(config => this.config = config);
  }

  public postConfig(): void {
    this.resetStatus();
    this.openemsService.postConfig(JSON.stringify(this.config, null, '\t').replace(/(\n|\t)/gm,''))
      .then(response => this.handleSuccess())
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
