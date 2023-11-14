import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { environment } from 'src/environments';
import { Edge, Service, UserPermission, Utils } from '../../shared/shared';
import { canSeeAppCenter } from './app/permissions';

@Component({
  selector: 'settings',
  templateUrl: './settings.component.html',
})
export class SettingsComponent implements OnInit {

  public edge: Edge = null;
  public environment = environment;

  public canSeeAppCenter: boolean | undefined;

  protected canSeeHomeAssistent: boolean = false;
  protected canSeeCommercialAssistent: boolean = false;
  protected isEdgeBackend: boolean = environment.backend === 'OpenEMS Edge';

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private service: Service,
  ) {
  }

  public ngOnInit() {
    this.service.setCurrentComponent({ languageKey: 'Menu.edgeSettings' }, this.route).then(edge => {
      this.edge = edge;
      this.canSeeAppCenter = canSeeAppCenter(this.edge);
      this.canSeeHomeAssistent = UserPermission.isUserAllowedToSeeHomeAssistent(this.service.metadata?.value?.user, edge);
      this.canSeeCommercialAssistent = UserPermission.isUserAllowedToSeeCommercialServiceAssistent(this.service.metadata?.value?.user, edge);
    });
  }
}
