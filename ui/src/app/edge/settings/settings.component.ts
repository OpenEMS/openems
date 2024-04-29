// @ts-strict-ignore
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { environment } from 'src/environments';
import { Edge, Service, Utils } from '../../shared/shared';
import { canSeeAppCenter } from './app/permissions';
import { canSeeJsonrpcTest } from './jsonrpctest/permission';

@Component({
  selector: 'settings',
  templateUrl: './settings.component.html',
})
export class SettingsComponent implements OnInit {

  public edge: Edge = null;
  public environment = environment;

  public canSeeAppCenter: boolean | undefined;
  public canSeeJsonrpcTest: boolean | undefined;

  protected isEdgeBackend: boolean = environment.backend === 'OpenEMS Edge';

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private service: Service,
  ) {
  }

  public ngOnInit() {
    this.service.getCurrentEdge().then(edge => {
      this.edge = edge;
      const user = this.service.metadata?.value?.user;
      this.canSeeAppCenter = canSeeAppCenter(this.edge);
      this.canSeeJsonrpcTest = canSeeJsonrpcTest(user, edge);
    });
  }
}
