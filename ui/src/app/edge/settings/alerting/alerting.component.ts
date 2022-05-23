import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { environment } from '../../../../environments';
import { Edge, Service, Utils, Websocket } from '../../../shared/shared';
import { SetAlertingConfigRequest } from './setAlertingConfigRequest';
import { GetAlertingConfigRequest } from './getAlertingConfigRequest';
import { GetAlertingConfigResponse, AlertingState } from './getAlertingConfigResponse';
import { User } from '../../../shared/jsonrpc/shared';
import { TranslateService } from '@ngx-translate/core';
import { HostListener } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFormOptions, FormlyFieldConfig } from '@ngx-formly/core';
import { RaceSubscriber } from 'rxjs/internal/observable/race';

@Component({
  selector: AlertingComponent.SELECTOR,
  templateUrl: './alerting.component.html'
})
export class AlertingComponent implements OnInit {
  static readonly MIN = 3;
  static readonly MAX = 60;
  private static readonly SELECTOR = "alerting";

  public readonly environment = environment;

  public alertingState: AlertingSetting = new AlertingSetting;
  public alertingOdoo: AlertingSetting = null;

  public readonly spinnerId: string = AlertingComponent.SELECTOR;

  public edge: Edge = null;
  public user: User = null;

  form = new FormGroup({});
  model = this.alertingState;
  options: FormlyFormOptions = {};
  fields: FormlyFieldConfig[] = [
    {
      key: 'isOn',
      type: 'checkbox',
      templateOptions: {
        label: this.translate.instant('Edge.Config.Alerting.activate'),
      },
      parsers: [
        (value) => {
          if (value) {
            if (this.alertingState.delay > AlertingComponent.MAX) {
              this.alertingState.delay = AlertingComponent.MAX
            } else if (this.alertingState.delay < AlertingComponent.MIN) {
              this.alertingState.delay = AlertingComponent.MIN
            }
          }
          return value
        }
      ],
    },
    {
      key: 'delay',
      type: 'input',
      templateOptions: {
        label: this.translate.instant('Edge.Config.Alerting.delay'),
        description: "[" + AlertingComponent.MIN + ";" + AlertingComponent.MAX + "]",
        type: 'number',
        required: true,
        min: AlertingComponent.MIN,
        max: AlertingComponent.MAX
      },
      parsers: [
        (value) => {
          if (this.alertingState.isOn) {
            if (value != "") {
              if (value < 0) {
                return 0;
              } else {
                return value
              }
              //value = Math.abs(value)
            }
          }
          return value;
        }
      ],
      hideExpression: '!model.isOn',
    },
  ]

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
    private translate: TranslateService,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent(this.translate.instant('Edge.Config.Index.alerting'), this.route).then(edge => {
      this.edge = edge;
      this.user = this.service.metadata.getValue().user;

      this.options = {
        formState: {
          awesomeIsForced: false,
        },
      };

      this.refreshState(new GetAlertingConfigRequest(this.edge.id));
    });
  }

  ionViewWillEnter() {
    if (this.options.resetModel) {
      this.options.resetModel();
    }
  }

  public IsOnChanged(event: any) {
    this.alertingState.isOn = event.detail.checked;
  }

  public get noChanges() {
    return this.alertingState.equals(this.alertingOdoo);
  }

  public set noChanges(value: boolean) {
    return;
  }

  private refreshState(request: GetAlertingConfigRequest | SetAlertingConfigRequest) {
    this.service.startSpinner(this.spinnerId);

    this.websocket.sendRequest(request)
      .then(response => {
        let result = (response as GetAlertingConfigResponse).result;

        this.alertingOdoo = new AlertingSetting(result);
        this.alertingState.timeToWait = this.alertingOdoo.timeToWait;

        if (this.options.updateInitialValue) {
          this.options.updateInitialValue();
        }

        this.service.stopSpinner(this.spinnerId);

        if (request instanceof SetAlertingConfigRequest) {
          this.service.toast("Einstellungen übernommen", 'success')
        }
      }).catch(reason => {
        if (reason.error.message.startsWith('settings_err:')) {
          this.alertingState = null;
        } else {
          console.error(reason.error);
          this.service.toast("Error while loading Alerting state: " + reason.error.message, 'danger');
        }
      });
  }

  public updateAlerting() {
    if (this.noChanges || this.form.invalid) {
      return;
    } else {
      var setConfig = {
        edgeId: this.edge.id,
        timeToWait: this.alertingState.timeToWait
      }
      var request = new SetAlertingConfigRequest(setConfig);

      this.refreshState(request);
    }
  }

  @HostListener('keypress', ['$event'])
  onInput(event: any) {
    const pattern = /[0-9]/; // without ., for integer only
    let inputChar = String.fromCharCode(event.which ? event.which : event.keyCode);

    if (!pattern.test(inputChar)) {
      // invalid character, prevent input
      event.preventDefault();
      return false;
    }
    return true;
  }

}

class AlertingSetting {
  private _isOn: boolean = false;
  private _delay: number = AlertingComponent.MIN;

  constructor(state?: AlertingState) {
    if (state != null) {
      this.timeToWait = state.timeToWait;
    }
  }

  public get isOn(): boolean {
    return this._isOn;
  }
  public set isOn(val: boolean) {
    this._isOn = val;
  }
  public get delay(): number {
    return this._delay;
  }
  public set delay(val: number) {
    this._delay = val;
  }

  public get timeToWait(): number {
    if (this._isOn) {
      return this.delay;
    } else {
      return -this.delay;
    }
  }
  public set timeToWait(val: number) {
    this.isOn = val > 0;
    this.delay = val;
  }

  public equals(other: AlertingSetting): boolean {
    if (this.isOn) {
      if (other.isOn) {
        return this.delay == other.delay;
      }
    } else {
      if (other.isOn) {
        return false;
      } else {
        return true;
      }
    }
  }
}