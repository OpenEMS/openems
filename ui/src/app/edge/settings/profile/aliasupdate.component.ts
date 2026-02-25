// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { CommonUiModule } from "../../../shared/common-ui.module";

@Component({
    selector: "aliasupdate",
    templateUrl: "./aliasupdate.component.html",
    standalone: true,
    imports: [
        CommonUiModule,
        RouterModule,
        ReactiveFormsModule,
    ],
})
export class AliasUpdateComponent implements OnInit {

    public component: EdgeConfig.Component | null = null;

    public formGroup: FormGroup | null = null;
    public factory: EdgeConfig.Factory | null = null;
    public componentIcon: string | null = null;

    private edge: Edge;

    constructor(
        private service: Service,
        private route: ActivatedRoute,
        private websocket: Websocket,
        private translate: TranslateService,
        private formBuilder: FormBuilder,
    ) { }

    ngOnInit() {
        this.service.getCurrentEdge().then(edge => {
            this.edge = edge;
        });
        this.service.getConfig().then(config => {
            const componentId = this.route.snapshot.params["componentId"];
            this.component = config.components[componentId];
            this.factory = config.factories[this.component.factoryId];
            this.componentIcon = config.getFactoryIcon(this.factory, this.translate);
            this.formGroup = this.formBuilder.group({
                alias: new FormControl(this.component.alias),
            });
        });
    }

    updateAlias(alias) {
        const newAlias = alias;
        if (this.edge != null) {
            if (this.component.id == newAlias) {
                this.service.toast(this.translate.instant("GENERAL.INPUT_NOT_VALID"), "danger");
            } else {
                this.edge.updateComponentConfig(this.websocket, this.component.id, [
                    { name: "alias", value: newAlias },
                ]).then(() => {
                    this.formGroup.markAsPristine();
                    this.service.toast(this.translate.instant("GENERAL.CHANGE_ACCEPTED"), "success");
                }).catch(reason => {
                    this.formGroup.markAsPristine();
                    this.service.toast(this.translate.instant("GENERAL.CHANGE_FAILED") + "\n" + reason.error.message, "danger");
                    console.warn(reason);
                });
            }
        }
    }
}
