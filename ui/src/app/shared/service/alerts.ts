import { Injectable } from '@angular/core';



import { AlertController } from '@ionic/angular';
import { Service } from '../shared';
import { TranslateService, LangChangeEvent } from '@ngx-translate/core';

@Injectable()
export class Alerts {



    constructor(
        private alertCtrl: AlertController,
        private service: Service,
        public translate: TranslateService
    ) {

    }


    async defaultAlert() {

        const alert: HTMLIonAlertElement = await this.alertCtrl.create({
            buttons: ['Ok'],
            message: this.translate.instant('Alerts.Default'),
            header: this.translate.instant('Alerts.Error')

        });
        await alert.present();
    }
    async showError(message: string) {
        const alert: HTMLIonAlertElement = await this.alertCtrl.create({
            buttons: ['Ok'],
            message: message,
            header: this.translate.instant('Alerts.Error')

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
            header: this.translate.instant('Alerts.RetrievePwdHeader'),
            message: this.translate.instant('Alerts.RetrievePwdMsg'),
            inputs: [
                {
                    name: 'user_login',
                    placeholder: this.translate.instant('Alerts.RetrievePwdPlaceholder')
                }
            ],
            buttons: [
                {
                    text: this.translate.instant('Alerts.Cancel'),
                    role: 'cancel'

                },
                {
                    text: this.translate.instant('Alerts.Send'),
                    handler: async data => {
                        let response = await this.service.sendWPPasswordRetrieve(data.user_login);
                        if (response['status'] === 'ok') {
                            this.showMsg(this.translate.instant('Alerts.RetrievePwdSent'));
                        } else {
                            this.showError(this.translate.instant('Alerts.RetrievePwdError'));
                        }
                    }
                }
            ]
        });
        await alert.present();
    }

    async confirmLoginDelete() {
        const alert: HTMLIonAlertElement = await this.alertCtrl.create({
            header: this.translate.instant('Alerts.ClearLoginHeader'),
            message: this.translate.instant('Alerts.ClearLoginMsg'),
            buttons: [
                {
                    text: this.translate.instant('Alerts.Cancel'),
                    role: 'cancel'

                },
                {
                    text: "Ok",
                    handler: () => {
                        localStorage.removeItem("username");
                        localStorage.removeItem("password");
                        this.showMsg(this.translate.instant('Alerts.ClearLoginDone'));
                    }
                }
            ]
        });
        await alert.present();
    }

    async updateConfirm(message: string, restart: boolean) {
        const alert: HTMLIonAlertElement = await this.alertCtrl.create({
            header: "Updates",
            message: message,
            buttons: [
                {
                    text: "Ok",
                    role: 'cancel',
                    handler: () => {
                        if (restart) {
                            this.service.forceRestart();
                        }
                    }
                }
            ]
        });
        await alert.present();
    }

    load() {
        return true;
    }

}
