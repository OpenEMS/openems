// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";

@Component({
    selector: "aliasupdate",
    templateUrl: "./ALIASUPDATE.COMPONENT.HTML",
    standalone: false,
})
export class AliasUpdateComponent implements OnInit {

    public component: EDGE_CONFIG.COMPONENT | null = null;

    public formGroup: FormGroup | null = null;
    public factory: EDGE_CONFIG.FACTORY | null = null;
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
        THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
            THIS.EDGE = edge;
        });
        THIS.SERVICE.GET_CONFIG().then(config => {
            const componentId = THIS.ROUTE.SNAPSHOT.PARAMS["componentId"];
            THIS.COMPONENT = CONFIG.COMPONENTS[componentId];
            THIS.FACTORY = CONFIG.FACTORIES[THIS.COMPONENT.FACTORY_ID];
            THIS.COMPONENT_ICON = CONFIG.GET_FACTORY_ICON(THIS.FACTORY, THIS.TRANSLATE);
            THIS.FORM_GROUP = THIS.FORM_BUILDER.GROUP({
                alias: new FormControl(THIS.COMPONENT.ALIAS),
            });
        });
    }

    updateAlias(alias) {
        const newAlias = alias;
        if (THIS.EDGE != null) {
            if (THIS.COMPONENT.ID == newAlias) {
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.INPUT_NOT_VALID"), "danger");
            } else {
                THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, THIS.COMPONENT.ID, [
                    { name: "alias", value: newAlias },
                ]).then(() => {
                    THIS.FORM_GROUP.MARK_AS_PRISTINE();
                    THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
                }).catch(reason => {
                    THIS.FORM_GROUP.MARK_AS_PRISTINE();
                    THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + REASON.ERROR.MESSAGE, "danger");
                    CONSOLE.WARN(reason);
                });
            }
        }
    }
}
