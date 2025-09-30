import { Component, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { Role } from "src/app/shared/type/role";
import { environment } from "src/environments";
import { Edge, Service, Utils } from "../../shared/shared";
import { JsonrpcTestPermission } from "./jsonrpctest/JSONRPCTEST.PERMISSION";

@Component({
  selector: "settings",
  templateUrl: "./SETTINGS.COMPONENT.HTML",
  standalone: false,
})
export class SettingsComponent implements OnInit {

  public edge: Edge | null = null;
  public environment = environment;

  public isAtLeastOwner: boolean = false;
  public isAtLeastInstaller: boolean = false;
  public isAtLeastAdmin: boolean = false;
  public canSeeJsonrpcTest: boolean = false;

  protected isEdgeBackend: boolean = ENVIRONMENT.BACKEND === "OpenEMS Edge";

  constructor(
    protected utils: Utils,
    private service: Service,
    private translate: TranslateService,
  ) {
  }

  public ngOnInit() {
    THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
      THIS.EDGE = edge;
      const user = THIS.SERVICE.METADATA?.value?.user;
      THIS.IS_AT_LEAST_OWNER = EDGE.ROLE_IS_AT_LEAST(ROLE.OWNER);
      THIS.IS_AT_LEAST_INSTALLER = EDGE.ROLE_IS_AT_LEAST(ROLE.INSTALLER);
      THIS.IS_AT_LEAST_ADMIN = EDGE.ROLE_IS_AT_LEAST(ROLE.ADMIN);
      THIS.CAN_SEE_JSONRPC_TEST = JSONRPC_TEST_PERMISSION.CAN_SEE(user, edge);
    });
  }
}
