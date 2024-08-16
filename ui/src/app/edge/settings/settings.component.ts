import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Role } from 'src/app/shared/type/role';
import { environment } from 'src/environments';
import { Edge, Service, Utils } from '../../shared/shared';
import { JsonrpcTestPermission } from './jsonrpctest/jsonrpctest.permission';

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
    private translate: TranslateService,
  ) {
  }

  public ngOnInit() {
    this.service.getCurrentEdge().then(edge => {
      this.edge = edge;
      const user = this.service.metadata?.value?.user;
      this.isAtLeastOwner = edge.roleIsAtLeast(Role.OWNER);
      this.isAtLeastInstaller = edge.roleIsAtLeast(Role.INSTALLER);
      this.isAtLeastAdmin = edge.roleIsAtLeast(Role.ADMIN);
      this.canSeeJsonrpcTest = JsonrpcTestPermission.canSee(user, edge);
    });
  }
}
