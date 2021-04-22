import { ChannelAddress, EdgeConfig } from '../../../../shared/shared';
import { Component } from '@angular/core';
import { StorageModalComponent } from './modal/modal.component';
import { AbstractFlatWidget } from '../../flat/abstract-flat-widget';
import { CurrentData } from "src/app/shared/shared";

@Component({
    selector: 'storage',
    templateUrl: './storage.component.html'
})
export class StorageComponent extends AbstractFlatWidget {

    public essComponents: EdgeConfig.Component[] = [];
    public chargerComponents: EdgeConfig.Component[] = [];
    public storageItem: string = null;
    public stateOfCharge: number[] = [];

    protected getChannelAddresses() {
        let channelAddresses: ChannelAddress[] = [];
        channelAddresses.push(
            new ChannelAddress('_sum', 'EssSoc'),
            new ChannelAddress('_sum', 'EssActivePower'),
            // channels for modal component, subscribe here for better UX
            new ChannelAddress('_sum', 'EssActivePowerL1'),
            new ChannelAddress('_sum', 'EssActivePowerL2'),
            new ChannelAddress('_sum', 'EssActivePowerL3'),
            new ChannelAddress('_sum', 'EssCapacity'),
        )
        this.chargerComponents = this.config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger").filter(component => component.isEnabled);
        for (let component of this.chargerComponents) {
            channelAddresses.push(
                new ChannelAddress(component.id, 'ActualPower'),
            )
        }
        this.essComponents = this.config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss").filter(component => !component.factoryId.includes("Ess.Cluster") && component.isEnabled);
        for (let component of this.essComponents) {
            channelAddresses.push(
                new ChannelAddress(component.id, 'Soc'),
                new ChannelAddress(component.id, 'Capacity'),
            );
            if (this.config.factories[component.factoryId].natureIds.includes("io.openems.edge.ess.api.AsymmetricEss")) {
                channelAddresses.push(
                    new ChannelAddress(component.id, 'ActivePowerL1'),
                    new ChannelAddress(component.id, 'ActivePowerL2'),
                    new ChannelAddress(component.id, 'ActivePowerL3')
                );
            }
        }
        return channelAddresses
    }
    /**
     * Use 'checkStateOfCharge' to check, if a Storage-System has a State of Charge
     * 
     * @param componentID componentID of essComponents
     * @returns boolean
     */
    public checkStateOfCharge(componentID: string): boolean {
        if (this.stateOfCharge[componentID] != null) {
            return true;
        }
    }
    protected onCurrentData(currentData: CurrentData) {
        // Check State_of_Charge for every component of essComponents
        for (let component of this.essComponents) {
            this.stateOfCharge[component.id] = currentData.allComponents[component.id + '/Soc'];
        }
        // Check total State_of_Charge for dynamical icon in widget-header
        let soc = currentData.allComponents['_sum' + '/EssSoc'];
        if (soc < 20) {
            this.storageItem = 'assets/img/storage_20.png'
        } else if (soc < 40 || soc == 20) {
            this.storageItem = 'assets/img/storage_40.png'
        } else if (soc < 60 || soc == 40) {
            this.storageItem = 'assets/img/storage_60.png'
        } else if (soc < 80 || soc == 60) {
            this.storageItem = 'assets/img/storage_80.png'
        } else if (soc < 100 || soc == 80) {
            this.storageItem = 'assets/img/storage_100.png'
        } else {
            this.storageItem = 'assets/img/storage_100.png'
        }
    }
    /**
      * Use 'convertChargePower' to convert/map a value, to be only showed when not negative
      * 
      * @param value takes @Input value or channelAddress for chargePower
      * @returns only positive value
      */
    public convertChargePower = (value: any): string => {
        let thisValue = (value / 1000 * -1).toFixed(1);
        if (value <= 0) {
            if (thisValue.endsWith('0')) {
                return parseInt(thisValue).toString() + ' kW';
            } else {
                return thisValue + ' kW';
            }
        } else {
            return '-'
        }
    }
    /**
     *  Use 'convertDischargePower' to convert/map a value, to be only showed when not negative
      * 
      * @param value takes @Input value or channelAddress for dischargePower
      * @returns only positive value
      */
    public convertDischargePower = (value: any): string => {
        let thisValue = (value / 1000).toFixed(1);
        if (value > 0) {
            if (thisValue.endsWith('0')) {
                return parseInt(thisValue).toString() + ' kW';
            } else {
                return thisValue + ' kW';
            }
        } else {
            return '-'
        }
    }
    async presentModal() {
        const modal = await this.modalController.create({
            component: StorageModalComponent,
            componentProps: {
                edge: this.edge,
                config: this.config,
                essComponents: this.essComponents,
                chargerComponents: this.chargerComponents,
            }
        });
        return await modal.present();
    }
}
