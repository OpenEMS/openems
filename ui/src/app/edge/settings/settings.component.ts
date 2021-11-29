import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { environment } from 'src/environments';
import { Edge, Service, Utils } from '../../shared/shared';

@Component({
  selector: 'settings',
  templateUrl: './settings.component.html'
})
export class SettingsComponent {

  public edge: Edge = null;
  public environment = environment;

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private service: Service,
    private translate: TranslateService
  ) {
  }

  ionViewWillEnter() {
    this.service.setCurrentComponent(this.translate.instant('Menu.edgeSettings'), this.route).then(edge => {
      this.edge = edge
    });
  }

}