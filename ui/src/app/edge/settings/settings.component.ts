import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, Service, Utils } from '../../shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { environment } from 'src/environments';


@Component({
  selector: 'settings',
  templateUrl: './settings.component.html'
})
export class SettingsComponent implements OnInit {

  public edge: Edge = null;
  public env = environment;

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private service: Service,
    private translate: TranslateService
  ) {
  }

  ngOnInit() {
    this.service.setCurrentComponent(this.translate.instant('Menu.edgeSettings'), this.route).then(edge => {
      this.edge = edge
    });
  }

}