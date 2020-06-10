import { Component, Input } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { EdgeConfig, Edge, Websocket, Service } from 'src/app/shared/shared';
import { FormGroup, FormBuilder, FormControl, Validators } from '@angular/forms';
import { PopoverController } from '@ionic/angular';

@Component({
    selector: 'profile-popover',
    templateUrl: './popover.component.html'
})
export class ProfilePopoverComponent {

    @Input() public component: EdgeConfig.Component;
    @Input() private edge: Edge;

    public formGroup: FormGroup;
    public alias = null;

    constructor(
        private service: Service,
        private websocket: Websocket,
        protected translate: TranslateService,
        public formBuilder: FormBuilder,
        public popoverController: PopoverController,
    ) { }

    ngOnInit() {
        this.formGroup = this.formBuilder.group({
            alias: new FormControl(this.component.alias)
        })
        this.alias = this.formGroup.controls['alias'];
    }


    resetAlias() {
        let oldAlias = this.component.alias;
        let newAlias = "";
        this.formGroup.controls['alias'].setValue(newAlias);
    }

    updateAlias() {
        let oldAlias = this.component.alias;
        let newAlias = this.formGroup.controls['alias'].value;

        if (this.edge != null) {
            this.edge.updateComponentConfig(this.websocket, this.component.id, [
                { name: 'alias', value: newAlias }
            ]).then(() => {
                this.component.properties.alias = newAlias;
                this.popoverController.dismiss();
                this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
            }).catch(reason => {
                this.component.properties.alias = oldAlias;
                this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
                this.popoverController.dismiss();
                console.warn(reason);
            });
        }
    }
}