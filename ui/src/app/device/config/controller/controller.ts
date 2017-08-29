import { FormGroup, FormBuilder } from '@angular/forms';

import { Thing } from '../thing';
import { Websocket, Config, Meta, ThingMeta } from '../../../shared/shared';
import { Device } from '../../../shared/device/device';

export class Controller extends Thing {
    public readonly meta: ThingMeta;

    public static getControllers(config: Config, formBuilder: FormBuilder): Controller[] {
        let controllers: Controller[] = [];
        for (let controllerConfig of config.scheduler.controllers) {
            controllers.push(new Controller(controllerConfig, config._meta, formBuilder));
        }
        return controllers;
    }

    constructor(config: any, meta: Meta, formBuilder: FormBuilder) {
        super(formBuilder);
        // get meta for controller
        if ('class' in config && config.class in meta.availableControllers) {
            this.meta = meta.availableControllers[config.class];
        } else {
            this.meta = {
                title: config.class,
                class: config.class,
                text: '',
                channels: {}
                // TODO add channels from config
            }
        }
        // add missing channels
        for (let channelName in this.meta.channels) {
            if (!(channelName in config)) {
                //let channel = this.meta.channels[channelName];
                config[channelName] = null;
                // TODO set default value
            }
        }
        // build form
        this.form = this.buildFormGroup(config);
    }

    /**
     * Save all changes to this controller
     */
    public save(device: Device): void {
        let requests = this.getConfigUpdateRequests(this.form);
        this.send(requests, device);
        this.form.markAsPristine();
    }
}