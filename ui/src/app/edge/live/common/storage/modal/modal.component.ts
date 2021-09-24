import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, EdgeConfig, Edge, Websocket, Utils } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';

@Component({
    selector: 'storage-modal',
    templateUrl: './modal.component.html',
})
export class StorageModalComponent {

    // TODO after refactoring of Model: subscribe to EssActivePowerL1/L2/L3 here instead of Flat Widget

    @Input() edge: Edge;
    @Input() config: EdgeConfig;
    @Input() essComponents: EdgeConfig.Component[];
    @Input() chargerComponents: EdgeConfig.Component[];
    @Input() singleComponent: EdgeConfig.Component = null;

    // reference to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    public formGroup: FormGroup;
    public emergencyReserveComponents: { [essId: string]: EdgeConfig.Component } = {};
    public isEmergencyReserveEnabled: boolean[] = [];
    public isEnabled: boolean[] = [];


    constructor(
        public service: Service,
        public translate: TranslateService,
        public modalCtrl: ModalController,
        public websocket: Websocket,
        public formBuilder: FormBuilder,
    ) { }

    ngOnInit() {

        this.emergencyReserveComponents = this.config
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

        for (let essId in this.emergencyReserveComponents) {
            let controller = this.emergencyReserveComponents[essId];

            let reserveSoc: [] = [];
            this.edge.currentData.subscribe(currentData => {
                reserveSoc = currentData.channel[controller.id + "/_PropertyReserveSoc"];
                this.isEmergencyReserveEnabled[essId] = currentData.channel[controller.id + "/_PropertyIsReserveSocEnabled"] == 1 ? true : false;
            })

            // Use EssId as FormGroupName
            this.formGroup.addControl(essId, this.formBuilder.group({
                isReserveSocEnabled: new FormControl(this.isEmergencyReserveEnabled[essId]),
                reserveSoc: new FormControl(reserveSoc),
            }));
            this.isEnabled[essId] = this.isEmergencyReserveEnabled[essId];
        }
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
                this.emergencyReserveComponents[essId].id,
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

    toggleEnabledMode(componentId: string) {
        this.isEnabled[componentId] = !this.isEnabled[componentId];
    }
}
