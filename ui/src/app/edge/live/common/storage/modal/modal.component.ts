import { Component, Input } from '@angular/core';
import { EdgeConfig, Edge, ChannelAddress, Utils, CurrentData } from 'src/app/shared/shared';
import { AbstractFlatWidget } from '../../../flat/abstract-flat-widget';

@Component({
    selector: 'storage-modal',
    templateUrl: './modal.component.html',
})
export class StorageModalComponent extends AbstractFlatWidget {

    @Input() edge: Edge;
    @Input() config: EdgeConfig;
    @Input() essComponents: EdgeConfig.Component[];
    @Input() chargerComponents: EdgeConfig.Component[];

    // reference to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    public outputChannel: ChannelAddress[] = null;
    public state: string[] = []
    public totalStateOfCharge: any;
    public totalCapacity: any;
    public totalChargePower: any;
    public totalDischargePower: any;
    public effectiveActivePowerL1: any;
    public effectiveActivePowerL2: any;
    public effectiveActivePowerL3: any;
    public isHybridEss: boolean[] = [];

    public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;
    public readonly CONVERT_TO_PERCENT = Utils.CONVERT_TO_PERCENT;
    public readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;
    public readonly CONVERT_TO_WATTHOURS = Utils.CONVERT_TO_WATTHOURS;
    public readonly CONVERT_CHARGE_POWER_TO_WATT = Utils.CONVERT_CHARGE_POWER_TO_WATT;
    public readonly CONVERT_DISCHARGE_POWER_TO_WATT = Utils.CONVERT_DISCHARGE_POWER_TO_WATT;

    protected getChannelAddresses() {
        let channelAddresses = [
            new ChannelAddress('_sum', 'EssActivePowerL1'),
            new ChannelAddress('_sum', 'EssActivePowerL2'),
            new ChannelAddress('_sum', 'EssActivePowerL3'),
            new ChannelAddress('_sum', 'EssCapacity'),
            new ChannelAddress('_sum', 'EssSoc'),
            new ChannelAddress('_sum', 'EssEffectivePowerL1'),
            new ChannelAddress('_sum', 'EssEffectivePowerL2'),
            new ChannelAddress('_sum', 'EssEffectivePowerL3'),

        ]
        for (let component of this.essComponents) {
            channelAddresses.push(ChannelAddress.fromString(component.id + '/ActivePower'));
        }
        for (let component of this.config
            .getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss")
            .filter(component => component.isEnabled && !this.config
                .getNatureIdsByFactoryId(component.factoryId)
                .includes("io.openems.edge.ess.api.MetaEss"))) {

            // Check if essComponent is HybridEss
            this.isHybridEss[component.id] = this.config
                .getNatureIdsByFactoryId(component.factoryId)
                .includes("io.openems.edge.ess.api.HybridEss");
        }

        return channelAddresses
    }
    protected onCurrentData(currentData: CurrentData) {
        this.totalStateOfCharge = currentData.allComponents['_sum/EssSoc'];
        this.totalCapacity = currentData.allComponents['_sum/EssCapacity'];
        this.edge.currentData.subscribe(currentData => {
            this.totalDischargePower = currentData.summary.storage.effectivePower;
            this.totalChargePower = this.totalDischargePower;

            if (this.chargerComponents != null && this.chargerComponents.length != 0) {
                if (currentData.summary.storage.effectiveActivePowerL1 <= 0) {
                    this.state['effectiveActivePowerL1'] = 'General.chargePower'
                    this.effectiveActivePowerL1 = Utils.multiplySafely(currentData.summary.storage.effectiveActivePowerL1, -1);

                } else {
                    this.state['effectiveActivePowerL1'] = 'General.dischargePower';
                    this.effectiveActivePowerL1 = currentData.summary.storage.effectiveActivePowerL1
                }

                if (currentData.summary.storage.effectiveActivePowerL2 <= 0) {
                    this.state['effectiveActivePowerL2'] = 'General.chargePower'
                    this.effectiveActivePowerL2 = Utils.multiplySafely(currentData.summary.storage.effectiveActivePowerL2, -1);
                } else {
                    this.state['effectiveActivePowerL2'] = 'General.dischargePower';
                    this.effectiveActivePowerL2 = currentData.summary.storage.effectiveActivePowerL2
                }

                if (currentData.summary.storage.effectiveActivePowerL3 <= 0) {
                    this.state['effectiveActivePowerL3'] = 'General.chargePower'
                    this.effectiveActivePowerL3 = Utils.multiplySafely(currentData.summary.storage.effectiveActivePowerL3, -1);
                } else {
                    this.state['effectiveActivePowerL3'] = 'General.dischargePower'
                    this.effectiveActivePowerL3 = currentData.summary.storage.effectiveActivePowerL3
                }
            } else {
                this.effectiveActivePowerL1 = currentData.summary.storage.activePowerL1;
                this.effectiveActivePowerL2 = currentData.summary.storage.activePowerL2;
                this.effectiveActivePowerL3 = currentData.summary.storage.activePowerL3;
            }
        })
    }
}