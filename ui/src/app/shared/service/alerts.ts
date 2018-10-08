import { Injectable } from '@angular/core';



import { AlertController } from '@ionic/angular';
import { Service } from '../shared';
import { TranslateService, LangChangeEvent } from '@ngx-translate/core';

@Injectable()
export class Alerts {

    translation;

    constructor(
        private alertCtrl: AlertController,
        private service: Service,
        public translate: TranslateService
    ) {

    }

    ngOnInit() {
        this.translate.get('Alerts').subscribe(res => this.translation = res);

        this.translate.onLangChange.subscribe((event: LangChangeEvent) => {
            this.translate.get('Alerts').subscribe(res => this.translation = res);
        });
    }
    async defaultAlert() {

        const alert: HTMLIonAlertElement = await this.alertCtrl.create({
            buttons: ['Ok'],
            message: this.translation.Default,
            header: this.translation.Error

        });
        await alert.present();
    }
    async showError(message: string) {
        const alert: HTMLIonAlertElement = await this.alertCtrl.create({
            buttons: ['Ok'],
            message: message,
            header: this.translation.Error

        });
        await alert.present();
    }

    async showMsg(message: string) {
        const alert: HTMLIonAlertElement = await this.alertCtrl.create({
            buttons: ['Ok'],
            message: message,
            header: ""

        });
        await alert.present();
    }

    async retrievePwd() {
        const alert: HTMLIonAlertElement = await this.alertCtrl.create({
            header: this.translation.RetrievePwdHeader,
            message: this.translation.RetrievePwdMsg,
            inputs: [
                {
                    name: 'user_login',
                    placeholder: this.translation.RetrievePwdPlaceholder
                }
            ],
            buttons: [
                {
                    text: this.translation.Cancel,
                    role: 'cancel'

                },
                {
                    text: this.translation.Send,
                    handler: async data => {
                        let response = await this.service.sendWPPasswordRetrieve(data.user_login);
                        if (response['status'] === 'ok') {
                            this.showMsg(this.translation.RetrievePwdSent);
                        } else {
                            this.showError(this.translation.RetrievePwdError);
                        }
                    }
                }
            ]
        });
        await alert.present();
    }

}
