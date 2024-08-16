// @ts-strict-ignore
import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { Edge, EdgeConfig, Service, Websocket } from '../../../../shared/shared';

@Component({
    selector: DelayedSellToGridModalComponent.SELECTOR,
    templateUrl: './modal.component.html',
})
export class DelayedSellToGridModalComponent implements OnInit {

    private static readonly SELECTOR = "delayedselltogrid-modal";
    @Input({ required: true }) protected component!: EdgeConfig.Component;
    @Input({ required: true }) protected edge!: Edge;


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
            continuousSellToGridPower: new FormControl(this.component.properties.continuousSellToGridPower, Validators.compose([
                Validators.pattern('^(?:[1-9][0-9]*|0)$'),
                Validators.required,
            ])),
            sellToGridPowerLimit: new FormControl(this.component.properties.sellToGridPowerLimit, Validators.compose([
                Validators.pattern('^(?:[1-9][0-9]*|0)$'),
                Validators.required,
            ])),
        });
    }

    applyChanges() {
        if (this.edge != null) {
            if (this.edge.roleIsAtLeast('owner')) {
                const continuousSellToGridPower = this.formGroup.controls['continuousSellToGridPower'];
                const sellToGridPowerLimit = this.formGroup.controls['sellToGridPowerLimit'];
                if (continuousSellToGridPower.valid && sellToGridPowerLimit.valid) {
                    if (sellToGridPowerLimit.value > continuousSellToGridPower.value) {
                        const updateComponentArray = [];
                        Object.keys(this.formGroup.controls).forEach((element, index) => {
                            if (this.formGroup.controls[element].dirty) {
                                updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value });
                            }
                        });
                        this.loading = true;
                        this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
                            this.component.properties.continuousSellToGridPower = continuousSellToGridPower.value;
                            this.component.properties.sellToGridPowerLimit = sellToGridPowerLimit.value;
                            this.loading = false;
                            this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
                        }).catch(reason => {
                            continuousSellToGridPower.setValue(this.component.properties.continuousSellToGridPower);
                            sellToGridPowerLimit.setValue(this.component.properties.sellToGridPowerLimit);
                            this.loading = false;
                            this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
                            console.warn(reason);
                        });
                        this.formGroup.markAsPristine();
                    } else {
                        this.service.toast(this.translate.instant('Edge.Index.Widgets.DelayedSellToGrid.relationError'), 'danger');
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
