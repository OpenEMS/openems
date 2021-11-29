import { Component } from '@angular/core';
import { Io_Api_DigitalInput_ModalComponent } from './modal/modal.component';
import { ChannelAddress, EdgeConfig } from 'src/app/shared/shared';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';

@Component({
    selector: 'Io_Api_DigitalInput',
    templateUrl: './Io_Api_DigitalInput.html'
})

export class Io_Api_DigitalInputComponent extends AbstractFlatWidget {

    public ioComponents: EdgeConfig.Component[] = null;
    public ioComponentCount = 0;

    protected getChannelAddresses() {
        let channels: ChannelAddress[] = [];
        this.service.getConfig().then(config => {

            this.ioComponents = config.getComponentsImplementingNature("io.openems.edge.io.api.DigitalInput").filter(component => component.isEnabled);
            for (let component of this.ioComponents) {

                for (let channel in component.channels) {
                    channels.push(
                        new ChannelAddress(component.id, channel)
                    );
                }
            }
            this.ioComponentCount = this.ioComponents.length;
        })
        return channels
    }

    async presentModal() {
        const modal = await this.modalController.create({
            component: Io_Api_DigitalInput_ModalComponent,
            componentProps: {
                edge: this.edge,
                ioComponents: this.ioComponents,
            }
        });
        return await modal.present();
    }

}