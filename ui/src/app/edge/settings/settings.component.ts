import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ProductType } from 'src/app/shared/type/widget';
import { environment } from 'src/environments';
import { Edge, Service, Utils } from '../../shared/shared';
import { canSeeAppCenter } from './app/permissions';

@Component({
  selector: 'settings',
  templateUrl: './settings.component.html'
})
export class SettingsComponent implements OnInit {

  public edge: Edge = null;
  public environment = environment;

  public canSeeAppCenter: boolean | undefined;

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private service: Service
  ) {
  }

  public ngOnInit() {
    this.service.setCurrentComponent({ languageKey: 'Menu.edgeSettings' }, this.route).then(edge => {
      this.edge = edge;
      this.canSeeAppCenter = canSeeAppCenter(this.edge);
    });
  }
}
