import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { ChannelAddress, CurrentData, EdgeConfig, Utils } from 'src/app/shared/shared';

import { ModalComponent } from '../modal/modal';

@Component({
    selector: 'Controller_Ess_GridOptimizedCharge',
    templateUrl: './flat.html'
})
export class FlatComponent extends AbstractFlatWidget {

    public override component: EdgeConfig.Component = null;
    public mode: string = '-';
    public state: string = '-';
    public isSellToGridLimitAvoided: boolean = false;
    public sellToGridLimitMinimumChargeLimit: boolean = false;
    public delayChargeMaximumChargeLimit: number | null = null;
    public readonly CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC = Utils.CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC(this.translate);
    public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;

    protected override getChannelAddresses() {
        return [
            new ChannelAddress(this.componentId, "DelayChargeState"),
            new ChannelAddress(this.componentId, "SellToGridLimitState"),
            new ChannelAddress(this.componentId, "DelayChargeMaximumChargeLimit"),
            new ChannelAddress(this.componentId, "SellToGridLimitMinimumChargeLimit"),
            new ChannelAddress(this.componentId, "_PropertyMode")
        ];
    }
    protected override onCurrentData(currentData: CurrentData) {
        this.mode = currentData.allComponents[this.component.id + '/_PropertyMode'];

        // Check if Grid feed in limitation is avoided
        if (currentData.allComponents[this.component.id + '/SellToGridLimitState'] == 0 ||
            (currentData.allComponents[this.component.id + '/SellToGridLimitState'] == 3
                && currentData.allComponents[this.component.id + '/DelayChargeState'] != 0
                && currentData.allComponents[this.component.id + '/SellToGridLimitMinimumChargeLimit'] > 0)) {
            this.isSellToGridLimitAvoided = true;
        }

        this.sellToGridLimitMinimumChargeLimit = currentData.allComponents[this.component.id + '/SellToGridLimitMinimumChargeLimit'];

        switch (currentData.allComponents[this.component.id + '/DelayChargeState']) {
            case -1:
                this.state = this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.State.notDefined');
                break;
            case 0:
                this.state = this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.State.chargeLimitActive');
                break;
            case 1:
                this.state = this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.State.passedEndTime');
                break;
            case 2:
                this.state = this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.State.storageAlreadyFull');
                break;
            case 3:
                this.state = this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.State.endTimeNotCalculated');
                break;
            case 4:
                this.state = this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.State.noLimitPossible');
                break;
            case 5:
            case 7:
                this.state = this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.State.noLimitActive');
                break;
            case 8: this.state = this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.chargingDelayed');
                break;
        }

        this.delayChargeMaximumChargeLimit = currentData.allComponents[this.component.id + '/DelayChargeMaximumChargeLimit'];
    }

    async presentModal() {
        const modal = await this.modalController.create({
            component: ModalComponent,
            componentProps: {
                component: this.component
            }
        });
        return await modal.present();
    }
}