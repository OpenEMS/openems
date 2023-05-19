import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';

@Component({
    selector: 'aliasupdate',
    templateUrl: './aliasupdate.component.html'
})
export class AliasUpdateComponent implements OnInit {

    private edge: Edge;

    public component: EdgeConfig.Component = null;
    public formGroup: FormGroup | null = null;
    public factory: EdgeConfig.Factory = null;
    public componentIcon: string = null;

    constructor(
        private service: Service,
        private route: ActivatedRoute,
        private websocket: Websocket,
        private translate: TranslateService,
        private formBuilder: FormBuilder,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent({ languageKey: 'Edge.Config.Index.renameComponents' }, this.route).then(edge => {
            this.edge = edge;
        });
        this.service.getConfig().then(config => {
            let componentId = this.route.snapshot.params["componentId"];
            this.component = config.components[componentId];
            this.factory = config.factories[this.component.factoryId];
            this.componentIcon = config.getFactoryIcon(this.factory);
            this.formGroup = this.formBuilder.group({
                alias: new FormControl(this.component.alias)
            });
        });
    }

    updateAlias(alias) {
        let newAlias = alias;
        if (this.edge != null) {
            if (this.component.id == newAlias) {
                this.service.toast(this.translate.instant('General.inputNotValid'), 'danger');
            } else {
                this.edge.updateComponentConfig(this.websocket, this.component.id, [
                    { name: 'alias', value: newAlias }
                ]).then(() => {
                    this.formGroup.markAsPristine();
                    this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
                }).catch(reason => {
                    this.formGroup.markAsPristine();
                    this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
                    console.warn(reason);
                });
            }
        }
    }
}