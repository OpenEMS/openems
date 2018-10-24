import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { environment } from '../../environments';
import { Alerts } from '../shared/shared';

@Component({
  selector: 'settings',
  templateUrl: './settings.component.html'
})
export class SettingsComponent {

  public env = environment;

  constructor(
    public translate: TranslateService,
    private alerts: Alerts
  ) { }

  public toggleDebugMode(event: CustomEvent) {
    this.env.debugMode = event.detail['checked'];
  }

  public clearLogin() {
    this.alerts.confirmLoginDelete();
  }
}
