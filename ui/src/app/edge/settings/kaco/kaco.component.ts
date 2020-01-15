import { Component } from '@angular/core';
import { Edge, Service, Websocket } from '../../../shared/shared';
import { ActivatedRoute } from '@angular/router';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'kaco',
    templateUrl: './kaco.component.html'
})

export class KacoComponent {
    public edge: Edge = null;
    public form: FormGroup;

    constructor(
        private service: Service,
        private websocket: Websocket,
        private formBuilder: FormBuilder,
        private route: ActivatedRoute,
        private translate: TranslateService
    ) {
        this.form = this.formBuilder.group({
            password: ['', Validators.required]
        });
    }

    ngOnInit() {
        this.service.setCurrentComponent(this.translate.instant('Edge.Config.Kaco.ChangePassword'), this.route).then(edge => {
            this.edge = edge;
        });
        this.service.getConfig().then(config => {
            let component = config.components['kacoCore0'];
            this.form.setValue({ password: component.properties['userkey'] });

        });
    }

    submit(password: string) {
        this.edge.updateComponentConfig(this.websocket, "kacoCore0", [{ name: 'userkey', value: password }]).then(response => {
            this.form.markAsPristine();
            this.service.toast(this.translate.instant('Edge.Config.Kaco.UpdateSuccess'), 'success');
        }).catch(reason => {
            this.service.toast(this.translate.instant('Edge.Config.Kaco.UpdateError') + ': ' + reason.error.message, 'danger');
        });
    }
}