import { ChangeDetectionStrategy, Component, OnInit, signal } from "@angular/core";
import { RouterModule } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { FlatWidgetButtonComponent } from "src/app/shared/components/flat/flat-widget-button/flat-widget-button";
import { NavigationService } from "src/app/shared/components/navigation/service/navigation.service";
import { UserService } from "src/app/shared/service/user.service";
import { Role } from "src/app/shared/type/role";
import { environment } from "src/environments";
import { Edge, Service, Utils } from "../../shared/shared";
import { JsonrpcTestPermission } from "./jsonrpctest/jsonrpctest.permission";

@Component({
    selector: "settings",
    templateUrl: "./settings.component.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonUiModule, RouterModule, FlatWidgetButtonComponent, ComponentsBaseModule],
})
export class SettingsComponent implements OnInit {
    public edge: Edge | null = null;
    public environment = environment;

    public isAtLeastOwner = signal(false);
    public isAtLeastInstaller: boolean = false;
    public isAtLeastAdmin: boolean = false;
    public isNewNavigation = signal(false);
    public canSeeJsonrpcTest: boolean = false;

    protected isEdgeBackend = signal(environment.backend === "OpenEMS Edge");

    constructor(
        protected utils: Utils,
        private service: Service,
        private translate: TranslateService,
        private readonly userService: UserService,
    ) {}

    public ngOnInit() {
        this.service.getCurrentEdge().then((edge) => {
            this.edge = edge;
            const user = this.service.metadata?.value?.user;
            this.isAtLeastOwner.set(edge.roleIsAtLeast(Role.OWNER));
            this.isAtLeastInstaller = edge.roleIsAtLeast(Role.INSTALLER);
            this.isAtLeastAdmin = edge.roleIsAtLeast(Role.ADMIN);
            this.canSeeJsonrpcTest = JsonrpcTestPermission.canSee(user, edge);
            this.isNewNavigation.set(
                NavigationService.isNewNavigation(
                    this.userService.currentUser(),
                    this.service.currentEdge()?.getConfigSignal()(),
                ),
            );
        });
    }
}
