import { Component, OnInit } from '@angular/core';
import { Role } from 'src/app/shared/type/role';
import { environment } from 'src/environments';
import { Edge, Service, Utils } from '../../shared/shared';
import { canSeeJsonrpcTest } from './jsonrpctest/permission';

@Component({
  selector: 'settings',
  templateUrl: './settings.component.html',
})
export class SettingsComponent implements OnInit {

  public edge: Edge | null = null;
  public environment = environment;

  public isAtLeastOwner: boolean = false;
  public isAtLeastInstaller: boolean = false;
  public isAtLeastAdmin: boolean = false;
  public canSeeJsonrpcTest: boolean = false;

  protected isEdgeBackend: boolean = environment.backend === 'OpenEMS Edge';

  constructor(
    protected utils: Utils,
    private service: Service,
  ) {
  }

  public ngOnInit() {
    this.service.getCurrentEdge().then(edge => {
      this.edge = edge;
      const user = this.service.metadata?.value?.user;
      this.isAtLeastOwner = Role.isAtLeast(user.globalRole, Role.OWNER);
      this.isAtLeastInstaller = Role.isAtLeast(user.globalRole, Role.INSTALLER);
      this.isAtLeastAdmin = Role.isAtLeast(user.globalRole, Role.ADMIN);
      this.canSeeJsonrpcTest = canSeeJsonrpcTest(user, edge);
    });
  }
}
