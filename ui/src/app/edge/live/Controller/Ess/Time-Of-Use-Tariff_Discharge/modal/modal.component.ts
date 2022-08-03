import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';

type Mode = 'OFF' | 'AUTOMATIC';

@Component({
    selector: Controller_Ess_TimeOfUseTariff_DischargeModalComponent.SELECTOR,
    templateUrl: './modal.component.html',
})
export class Controller_Ess_TimeOfUseTariff_DischargeModalComponent implements OnInit {

    @Input() public edge: Edge;
    @Input() public config: EdgeConfig;
    @Input() public component: EdgeConfig.Component;

    private static readonly SELECTOR = "timeofusetariffdischarge-modal";

    public formGroup: FormGroup;
    public loading: boolean = false;
    public pickerOptions: any;
    public isInstaller: boolean;
    public refreshChart: boolean;

    constructor(
        public formBuilder: FormBuilder,
        public modalCtrl: ModalController,
        public service: Service,
        public translate: TranslateService,
        public websocket: Websocket,
    ) { }

    ngOnInit() {
        this.refreshChart = false;
        if (this.edge.roleIsAtLeast(Role.INSTALLER)) {
            this.isInstaller = true;
        }
        this.formGroup = this.formBuilder.group({
            mode: new FormControl(this.component.properties.mode),
        })
    };

    updateProperty(property: string, event: CustomEvent) {
        this.formGroup.controls[property].setValue(event.detail.value);
        this.formGroup.controls[property].markAsDirty()
    }

    applyChanges() {
        if (this.edge != null) {
            if (this.edge.roleIsAtLeast('owner')) {
                let updateComponentArray = [];
                Object.keys(this.formGroup.controls).forEach((element, index) => {
                    if (this.formGroup.controls[element].dirty) {
                        updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
                    }
                });

                this.loading = true;
                this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
                    this.component.properties.mode = this.formGroup.controls['mode'].value;
                    this.loading = false;
                    this.refreshChart = true;
                    this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
                }).catch(reason => {
                    this.formGroup.controls['mode'].setValue(this.component.properties.mode);
                    this.loading = false;
                    console.warn(reason);
                });
                this.formGroup.markAsPristine()
            } else {
                this.service.toast(this.translate.instant('General.insufficientRights'), 'danger');
            }
        }
    }

    getState(state: number) {
        switch (state) {
            case -1:
                return this.translate.instant('Edge.Index.Widgets.TimeOfUseTariff.State.notStarted');
            case 0:
                return this.translate.instant('Edge.Index.Widgets.TimeOfUseTariff.State.delayed');
            case 1:
                return this.translate.instant('Edge.Index.Widgets.TimeOfUseTariff.State.allowsDischarge');
            case 2:
                return this.translate.instant('Edge.Index.Widgets.TimeOfUseTariff.State.standby');
        }
    }
}