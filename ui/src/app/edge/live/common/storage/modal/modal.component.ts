import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { first } from 'rxjs/operators';
import { Edge, EdgeConfig, Service, Utils, Websocket } from 'src/app/shared/shared';

@Component({
    selector: 'storage-modal',
    templateUrl: './modal.component.html',
})
export class StorageModalComponent implements OnInit {

    // TODO after refactoring of Model: subscribe to EssActivePowerL1/L2/L3 here instead of Flat Widget

    @Input() edge: Edge;
    @Input() config: EdgeConfig;
    @Input() essComponents: EdgeConfig.Component[] | null = null;
    @Input() chargerComponents: EdgeConfig.Component[];
    @Input() singleComponent: EdgeConfig.Component = null;

    // reference to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    public formGroup: FormGroup;

    constructor(
        public service: Service,
        public translate: TranslateService,
        public modalCtrl: ModalController,
        public websocket: Websocket,
        public formBuilder: FormBuilder,
    ) { }

    ngOnInit() {

        let emergencyReserveComponents = this.config
            .getComponentsImplementingNature('io.openems.edge.controller.ess.emergencycapacityreserve.EmergencyCapacityReserve')
            .filter(component => component.isEnabled)

            // Reduce emergencyComponents to have essId as key for controller
            .reduce((result, component) => {
                return {
                    ...result,
                    [component.properties['ess.id']]: component
                }
            }, {});

        this.formGroup = new FormGroup({});
        this.edge.currentData
            .pipe(first())
            .subscribe(currentData => {
                for (let essId in emergencyReserveComponents) {
                    let controller = emergencyReserveComponents[essId];
                    let reserveSoc = currentData.channel[controller.id + "/_PropertyReserveSoc"] ?? 20 /* default Reserve-Soc */;
                    let isReserveSocEnabled = currentData.channel[controller.id + "/_PropertyIsReserveSocEnabled"] == 1;
                    this.formGroup.addControl(essId, this.formBuilder.group({
                        controllerId: new FormControl(controller.id),
                        isReserveSocEnabled: new FormControl(isReserveSocEnabled),
                        reserveSoc: new FormControl(reserveSoc),
                    }));
                }
            });
    }

    applyChanges() {
        if (this.edge == null) {
            return;
        }
        for (let essId in this.formGroup.controls) {
            let essGroup = this.formGroup.controls[essId];

            // Check if essGroup didn't change
            if (essGroup.pristine) {
                continue;
            }

            this.edge.updateComponentConfig(this.websocket,
                essGroup.value['controllerId'],
                [{
                    name: 'isReserveSocEnabled',
                    value: essGroup.value['isReserveSocEnabled']
                },
                {
                    name: 'reserveSoc',
                    value: essGroup.value['reserveSoc']
                }]).then(() => {
                    this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
                    this.formGroup.markAsPristine();
                }).catch(reason => {
                    this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason, 'danger');
                });
        }
    }
}
