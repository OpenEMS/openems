import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, Service, Utils } from '../../shared/shared';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'settings',
  templateUrl: './settings.component.html'
})
export class SettingsComponent {

  public edge: Edge = null

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