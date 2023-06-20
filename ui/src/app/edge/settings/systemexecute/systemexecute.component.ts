import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { FormlyFieldConfig, FormlyFormOptions } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { ExecuteSystemCommandRequest } from 'src/app/shared/jsonrpc/request/executeCommandRequest';
import { ExecuteSystemCommandResponse } from 'src/app/shared/jsonrpc/response/executeSystemCommandResponse';
import { Service, Utils, Websocket } from '../../../shared/shared';

@Component({
  selector: SystemExecuteComponent.SELECTOR,
  templateUrl: './systemexecute.component.html'
})
export class SystemExecuteComponent implements OnInit {

  private static readonly SELECTOR = "systemExecute";

  public form: FormGroup;

  public model: any = {};
  public options: FormlyFormOptions = {};
  public fields: FormlyFieldConfig[] = [{
    key: 'predefined',
    type: 'radio',
    templateOptions: { options: [{ value: 'ping', label: 'Ping device in network' }] }
  }, {
    key: 'ping',
    hideExpression: (model: any, formState: any) => this.model['predefined'] !== 'ping',
    fieldGroup: [{
      key: 'ip',
      type: 'input',
      templateOptions: {
        label: 'IP-Address', placeholder: "192.168.0.1", required: true, pattern: /(\d{1,3}\.){3}\d{1,3}/
      },
      validation: {
        messages: {
          pattern: (error, field: FormlyFieldConfig) => `"${field.formControl.value}" is not a valid IP Address`
        }
      }
    }]
  }, {
    key: 'predefined',
    type: 'radio',
    templateOptions: {
      options: [
        { value: 'openems-restart', label: 'Restart OpenEMS Edge service' },
        { value: 'pagekite-log', label: 'Show Pagekite log' },
        { value: 'pagekite-restart', label: 'Restart Pagekite' }
      ]
    }
  }];

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
    private translate: TranslateService,
    private formBuilder: FormBuilder
  ) {
  }

  ngOnInit() {
    this.service.setCurrentComponent({ languageKey: 'Edge.Config.Index.systemExecute' }, this.route);
    this.form = this.formBuilder.group({
      username: new FormControl("root"),
      password: new FormControl(""),
      timeoutSeconds: new FormControl(5),
      runInBackground: new FormControl(false),
      command: new FormControl("")
    });
  }

  public loading: boolean = false;
  public stdout: string[] = [];
  public stderr: string[] = [];
  public commandLogs: ExecuteSystemCommandRequest[] = [];

  public updatePredefined() {
    let command;
    if (!this.form.valid) {
      command = "";
    } else {
      let m = this.model;
      switch (m.predefined) {
        case "ping":
          command = "ping -c4 " + m.ping.ip;
          break;
        case "openems-restart":
          command = "which at || DEBIAN_FRONTEND=noninteractive apt-get -y install at; echo 'systemctl restart openems' | at now";
          break;
        case "pagekite-log":
          command = "journalctl -lu fems-pagekite --since=\"2 minutes ago\"";
          break;
        case "pagekite-restart":
          command = "systemctl restart fems-pagekite";
          break;
      }
    }
    this.form.controls['command'].setValue(command);
  }

  public submit() {
    let username = this.form.controls['username'];
    let password = this.form.controls['password'];
    let timeoutSeconds = this.form.controls['timeoutSeconds'];
    let runInBackground = this.form.controls['runInBackground'];
    let command = this.form.controls['command'];

    this.service.getCurrentEdge().then(edge => {
      this.loading = true;
      this.stdout = [];
      this.stderr = [];
      let executeSystemCommandRequest = new ExecuteSystemCommandRequest({
        username: username.value,
        password: password.value,
        timeoutSeconds: timeoutSeconds.value,
        runInBackground: runInBackground.value,
        command: command.value
      });

      edge.sendRequest(this.websocket,
        new ComponentJsonApiRequest({
          componentId: "_host",
          payload: executeSystemCommandRequest
        })).then(response => {
          let result = (response as ExecuteSystemCommandResponse).result;
          this.loading = false;
          if (result.stdout.length == 0) {
            this.stdout = [""];
          } else {
            this.stdout = result.stdout;
          }
          this.stderr = result.stderr;

        }).catch(reason => {
          this.loading = false;
          this.stderr = ["Error executing system command:", reason.error.message];
        });
      this.commandLogs.unshift(executeSystemCommandRequest);
    });
  }

}