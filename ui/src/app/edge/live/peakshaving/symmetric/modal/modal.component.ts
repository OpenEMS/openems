import { Component, Input } from '@angular/core';
import { FormGroup, FormBuilder, FormControl, Validators } from '@angular/forms';
import { ModalController } from '@ionic/angular';
import { Service, EdgeConfig, Edge, Websocket } from '../../../../../shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

@Component({
    selector: SymmetricPeakshavingModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class SymmetricPeakshavingModalComponent {

    @Input() component: EdgeConfig.Component | null = null;
    @Input() edge: Edge | null = null;

    private static readonly SELECTOR = "symmetricpeakshaving-modal";

    public formGroup: FormGroup | null = null;
    public loading: boolean = false;

    constructor(
        public formBuilder: FormBuilder,
        public modalCtrl: ModalController,
        public service: Service,
        public translate: TranslateService,
        public websocket: Websocket,
    ) { }

    ngOnInit() {
        if (this.component != null) {
            this.formGroup = this.formBuilder.group({
                peakShavingPower: new FormControl(this.component.properties.peakShavingPower, Validators.compose([
                    Validators.pattern('^(?:[1-9][0-9]*|0)$'),
                    Validators.required
                ])),
                rechargePower: new FormControl(this.component.properties.rechargePower, Validators.compose([
                    Validators.pattern('^(?:[1-9][0-9]*|0)$'),
                    Validators.required
                ]))
            })
        }
    }

    applyChanges() {
        if (this.formGroup != null) {
            let peakShavingPower = this.formGroup.controls['peakShavingPower'];
            let rechargePower = this.formGroup.controls['rechargePower'];
            if (peakShavingPower.valid && rechargePower.valid) {
                if (peakShavingPower.value >= rechargePower.value) {
                    let updateComponentArray: DefaultTypes.UpdateComponentObject[] = [];
                    Object.keys(this.formGroup.controls).forEach((element, index) => {
                        if (this.formGroup != null && this.formGroup.controls[element].dirty) {
                            updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
                        }
                    })
                    if (this.edge != null && this.component != null) {
                        this.loading = true;
                        this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
                            if (this.component != null) {
                                this.component.properties.peakShavingPower = peakShavingPower.value;
                                this.component.properties.rechargePower = rechargePower.value;
                            }
                            this.loading = false;
                            this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
                        }).catch(reason => {
                            if (this.component != null) {
                                peakShavingPower.setValue(this.component.properties.peakShavingPower);
                                rechargePower.setValue(this.component.properties.rechargePower);
                            }
                            this.loading = false;
                            this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
                            console.warn(reason);
                        })
                        this.formGroup.markAsPristine()
                    }
                } else {
                    this.service.toast(this.translate.instant('Edge.Index.Widgets.Peakshaving.relationError'), 'danger');
                }
            } else {
                this.service.toast(this.translate.instant('General.inputNotValid'), 'danger');
            }
        }
    }
}