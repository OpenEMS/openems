// @ts-strict-ignore
import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/components/flat/abstract-flat-widget';
import { EdgeConfig } from 'src/app/shared/shared';

import { Io_Api_DigitalInput_ModalComponent } from './modal/modal.component';

@Component({
    selector: 'Io_Api_DigitalInput',
    templateUrl: './Io_Api_DigitalInput.html',
})

export class Io_Api_DigitalInputComponent extends AbstractFlatWidget {

    public ioComponents: EdgeConfig.Component[] = null;
    public ioComponentCount = 0;

    protected override afterIsInitialized(): void {
        this.service.getConfig().then(config => {
            this.ioComponents = config.getComponentsImplementingNature("io.openems.edge.io.api.DigitalInput").filter(component => component.isEnabled);
            this.ioComponentCount = this.ioComponents.length;
        });
    }

    async presentModal() {
        const modal = await this.modalController.create({
            component: Io_Api_DigitalInput_ModalComponent,
            componentProps: {
                edge: this.edge,
                ioComponents: this.ioComponents,
            },
        });
        return await modal.present();
    }

}
