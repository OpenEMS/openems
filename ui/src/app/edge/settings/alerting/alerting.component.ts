import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, Service, Utils, Websocket } from '../../../shared/shared';
import { SetAlertingConfigRequest } from './setAlertingConfigRequest';
import { GetAlertingConfigRequest } from './getAlertingConfigRequest';
import { GetAlertingConfigResponse, AlertingState } from './getAlertingConfigResponse';
import { TranslateService } from '@ngx-translate/core';
import { FormGroup } from '@angular/forms';
import { FormlyFormOptions, FormlyFieldConfig } from '@ngx-formly/core';

type option = { value: number, label: string }

@Component({
  selector: AlertingComponent.SELECTOR,
  templateUrl: './alerting.component.html'
})
export class AlertingComponent implements OnInit {
  private static readonly SELECTOR = "alerting";

  public static readonly OPTIONS = [15, 60, 1440];

  public edge: Edge = null;

  public form = new FormGroup({});
  public model: AlertingSetting = null;
  public options: FormlyFormOptions = {};
  public fields: FormlyFieldConfig[] = [
    {
      key: 'isOn',
      type: 'checkbox',
      templateOptions: {
        label: this.translate.instant('Edge.Config.Alerting.activate'),
      },
    },
    {
      key: 'delay',
      type: 'radio',
      templateOptions: {
        label: this.translate.instant('Edge.Config.Alerting.delay'),
        type: 'number',
        required: true,
        options: this.getOptions(),
      },
      hideExpression: model => !model.isOn,
    }
  ]

  public constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
    private translate: TranslateService,
  ) { }

  private getOptions(): option[] {
    let options: option[] = [];
    for (var val of AlertingComponent.OPTIONS) {
      options.push({ value: val, label: this.translate.instant('Edge.Config.Alerting.options.' + (val)) });
    }
    return options;
  }

  public ngOnInit(): void {
    this.service.setCurrentComponent(this.translate.instant('Edge.Config.Index.alerting'), this.route).then(edge => {
      this.edge = edge;

      this.options = {
        formState: {
          awesomeIsForced: false,
        },
      };

      this.refreshState(new GetAlertingConfigRequest(this.edge.id));
    });
  }

  protected ionViewWillEnter(): void {
    if (this.options.resetModel) {
      this.options.resetModel();
    }
  }

  private refreshState(request: GetAlertingConfigRequest | SetAlertingConfigRequest): void {
    this.service.startSpinner(AlertingComponent.SELECTOR);

    this.websocket.sendRequest(request)
      .then(response => {
        let result = (response as GetAlertingConfigResponse).result;

        this.model = new AlertingSetting(result);

        if (this.options.updateInitialValue) {
          this.options.updateInitialValue();
        }

        if (request instanceof SetAlertingConfigRequest) {
          this.service.toast(this.translate.instant('Edge.Config.Alerting.toast.success'), 'success')
        }

        this.service.stopSpinner(AlertingComponent.SELECTOR);
      }).catch(reason => {
        if (reason.error.message.startsWith('settings_err:')) {
          this.model = null;
        } else {
          console.error(reason.error);
          this.service.toast(this.translate.instant('Edge.Config.Alerting.toast.error') + ': ' + reason.error.message, 'danger');
        }
      });
  }

  public updateAlerting(): void {
    var setConfig = {
      edgeId: this.edge.id,
      timeToWait: this.model.timeToWait
    }
    var request = new SetAlertingConfigRequest(setConfig);
    this.refreshState(request);
  }
}

class AlertingSetting {
  private _isOn: boolean = false;
  private _delay: number = 15;
  private _isDirty: boolean = false;

  constructor(state?: AlertingState) {
    if (state != null) {
      let val = state.timeToWait
      this._isOn = val > 0

      val = Math.abs(val);
      if (AlertingComponent.OPTIONS.find(x => x === val) === undefined) {
        val = AlertingComponent.OPTIONS[0];
        this.isDirty = true;
      }
      this._delay = val
    }
  }

  public get isDirty(): boolean {
    return this._isDirty;
  }
  public set isDirty(val: boolean) {
    this._isDirty = val;
  }

  public get isOn(): boolean {
    return this._isOn;
  }
  public set isOn(val: boolean) {
    this.isDirty = true
    this._isOn = val;
  }

  public get delay(): number {
    return this._delay;
  }
  public set delay(val: number) {
    this.isDirty = true;
    this._delay = Math.abs(val);
  }

  public get timeToWait(): number {
    if (this._isOn) {
      return this.delay;
    } else {
      return -this.delay;
    }
  }
}