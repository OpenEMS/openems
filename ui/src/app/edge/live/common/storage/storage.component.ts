import { Component, Inject } from '@angular/core';
import { CurrentData } from "src/app/shared/shared";
import { ChannelAddress, EdgeConfig, Utils } from '../../../../shared/shared';
import { AbstractFlatWidget } from '../../flat/abstract-flat-widget';
import { StorageModalComponent } from './modal/modal.component';
import { DecimalPipe, formatNumber } from '@angular/common';

@Component({
    selector: 'storage',
    templateUrl: './storage.component.html'
})
export class StorageComponent extends AbstractFlatWidget {

    public essComponents: EdgeConfig.Component[] = [];
    public chargerComponents: EdgeConfig.Component[] = [];
    public storageItem: string = null;
    public isHybridEss: boolean[] = [];

    protected getChannelAddresses() {
        let channelAddresses: ChannelAddress[] = [
            new ChannelAddress('_sum', 'EssSoc')
        ];

        // Get Chargers
        // TODO should be moved to Modal
        this.chargerComponents = this.config
            .getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
            .filter(component => component.isEnabled);
        for (let component of this.chargerComponents) {
            channelAddresses.push(
                new ChannelAddress(component.id, 'ActualPower'),
            )
        }

        // Get ESSs
        this.essComponents = this.config
            .getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss")
            .filter(component => component.isEnabled && !this.config
                .getNatureIdsByFactoryId(component.factoryId)
                .includes("io.openems.edge.ess.api.MetaEss"));

        for (let component of this.config
            .getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss")
            .filter(component => component.isEnabled && !this.config
                .getNatureIdsByFactoryId(component.factoryId)
                .includes("io.openems.edge.ess.api.MetaEss"))) {

            // Check if essComponent is HybridEss
            this.isHybridEss[component.id] = this.config
                .getNatureIdsByFactoryId(component.factoryId)
                .includes("io.openems.edge.ess.api.HybridEss");

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

    protected onCurrentData(currentData: CurrentData) {
        // Check total State_of_Charge for dynamical icon in widget-header
        let soc = currentData.allComponents['_sum/EssSoc'];
        this.storageItem = "assets/img/" + Utils.getStorageSocImage(soc);
    }

    /**
      * Use 'convertChargePower' to convert/map a value
      * 
      * @param value takes @Input value or channelAddress for chargePower
      * @returns value
      */
    public convertChargePower = (value: any): string => {
        return this.convertPower(Utils.multiplySafely(value, -1), true);
    }

    /**
     * Use 'convertDischargePower' to convert/map a value
     * 
     * @param value takes @Input value or channelAddress for dischargePower
     * @returns value
     */
    public convertDischargePower = (value: any): string => {
        return this.convertPower(value)
    }

    /**
     * Use 'convertPower' to check whether 'charge/discharge' and to be only showed when not negative
     * 
     * @param value takes passed value when called 
     * @returns only positive and 0
     */
    public convertPower(value: number, isCharge?: boolean) {
        if (value == null) {
            return '-';
        }

        let thisValue: number = (value / 1000);

        // Round thisValue to Integer when decimal place equals 0 
        if (thisValue > 0) {
            return formatNumber(thisValue, 'de', '1.0-1') + " kW"; // TODO get locale dynamically

        } else if (thisValue == 0 && isCharge) {
            // if thisValue is 0, then show only when charge and not discharge
            return '0 kW';

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