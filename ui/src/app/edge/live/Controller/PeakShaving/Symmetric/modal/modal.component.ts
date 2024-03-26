import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { Edge, EdgeConfig, Service, Websocket } from '../../../../../../shared/shared';

@Component({
    selector: 'symmetricpeakshaving-modal',
    templateUrl: './modal.component.html',
})
export class Controller_Symmetric_PeakShavingModalComponent implements OnInit {

    @Input() protected component: EdgeConfig.Component;
    @Input() protected edge: Edge;


    public formGroup: FormGroup;
    public loading: boolean = false;

    constructor(
        public formBuilder: FormBuilder,
        public modalCtrl: ModalController,
        public service: Service,
        public translate: TranslateService,
        public websocket: Websocket,
    ) { }

    ngOnInit() {
        this.formGroup = this.formBuilder.group({
            peakShavingPower: new FormControl(this.component.properties.peakShavingPower, Validators.compose([
                Validators.pattern('^(?:[1-9][0-9]*|0)$'),
                Validators.required,
            ])),
            rechargePower: new FormControl(this.component.properties.rechargePower, Validators.compose([
                Validators.pattern('^(?:[1-9][0-9]*|0)$'),
                Validators.required,
            ])),
        });
    }

    applyChanges() {
        if (this.edge != null) {
            if (this.edge.roleIsAtLeast('owner')) {
                const peakShavingPower = this.formGroup.controls['peakShavingPower'];
                const rechargePower = this.formGroup.controls['rechargePower'];
                if (peakShavingPower.valid && rechargePower.valid) {
                    if (peakShavingPower.value >= rechargePower.value) {
                        const updateComponentArray = [];
                        Object.keys(this.formGroup.controls).forEach((element, index) => {
                            if (this.formGroup.controls[element].dirty) {
                                updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value });
                            }
                        });
                        this.loading = true;
                        this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
                            this.component.properties.peakShavingPower = peakShavingPower.value;
                            this.component.properties.rechargePower = rechargePower.value;
                            this.loading = false;
                            this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
                        }).catch(reason => {
                            peakShavingPower.setValue(this.component.properties.peakShavingPower);
                            rechargePower.setValue(this.component.properties.rechargePower);
                            this.loading = false;
                            this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
                            console.warn(reason);
                        });
                        this.formGroup.markAsPristine();
                    } else {
                        this.service.toast(this.translate.instant('Edge.Index.Widgets.Peakshaving.relationError'), 'danger');
                    }
                } else {
                    this.service.toast(this.translate.instant('General.inputNotValid'), 'danger');
                }
            } else {
                this.service.toast(this.translate.instant('General.insufficientRights'), 'danger');
            }
        }
    }
}
