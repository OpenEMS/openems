// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { FormlyFieldConfig, FormlyFormOptions } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { ExecuteSystemCommandRequest } from "src/app/shared/jsonrpc/request/executeCommandRequest";
import { ExecuteSystemCommandResponse } from "src/app/shared/jsonrpc/response/executeSystemCommandResponse";
import { Service, Utils, Websocket } from "../../../shared/shared";

type CommandFunction = (...args: (string | boolean | number)[]) => string;

const COMMANDS: { [key: string]: CommandFunction; } = {
  "ping": (ip: string) => `ping -c4 ${ip}`,
  "openems-restart": () => "which at || DEBIAN_FRONTEND=noninteractive apt-get -y install at; echo 'systemctl restart openems' | at now",
};

@Component({
  selector: SYSTEM_EXECUTE_COMPONENT.SELECTOR,
  templateUrl: "./SYSTEMEXECUTE.COMPONENT.HTML",
  standalone: false,
})
export class SystemExecuteComponent implements OnInit {

  private static readonly SELECTOR = "systemExecute";

  public loading: boolean = false;
  public stdout: string[] = [];
  public stderr: string[] = [];
  public commandLogs: ExecuteSystemCommandRequest[] = [];

  public form: FormGroup;

  public model: any = {};
  public options: FormlyFormOptions = {};
  public fields: FormlyFieldConfig[] = [{
    key: "predefined",
    type: "radio",
    templateOptions: { options: [{ value: "ping", label: "Ping device in network" }] },
  }, {
    key: "ping",
    hideExpression: (model: any, formState: any) => THIS.MODEL["predefined"] !== "ping",
    fieldGroup: [{
      key: "ip",
      type: "input",
      templateOptions: {
        label: "IP-Address", placeholder: "192.168.0.1", required: true, pattern: /(\d{1,3}\.){3}\d{1,3}/,
      },
      validation: {
        messages: {
          pattern: (error, field: FormlyFieldConfig) => `"${FIELD.FORM_CONTROL.VALUE}" is not a valid IP Address`,
        },
      },
    }],
  }, {
    key: "predefined",
    type: "radio",
    templateOptions: {
      options: [
        { value: "openems-restart", label: "Restart OpenEMS Edge service" },
      ],
    },
  }];

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
    private translate: TranslateService,
    private formBuilder: FormBuilder,
  ) {
  }

  ngOnInit() {
    THIS.FORM = THIS.FORM_BUILDER.GROUP({
      username: new FormControl("root"),
      password: new FormControl(""),
      timeoutSeconds: new FormControl(5),
      runInBackground: new FormControl(false),
      command: new FormControl(""),
    });
  }

  public updatePredefined() {
    let command;
    if (!THIS.FORM.VALID) {
      command = "";
    } else {
      const m = THIS.MODEL;
      const cmd = COMMANDS[M.PREDEFINED];
      switch (M.PREDEFINED) {
        case "ping":
          command = cmd(M.PING.IP);
          break;
        case "openems-restart":
        default:
          command = cmd();
      }
    }
    THIS.FORM.CONTROLS["command"].setValue(command);
  }

  public submit() {
    const username = THIS.FORM.CONTROLS["username"];
    const password = THIS.FORM.CONTROLS["password"];
    const timeoutSeconds = THIS.FORM.CONTROLS["timeoutSeconds"];
    const runInBackground = THIS.FORM.CONTROLS["runInBackground"];
    const command = THIS.FORM.CONTROLS["command"];

    THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
      THIS.LOADING = true;
      THIS.STDOUT = [];
      THIS.STDERR = [];
      const executeSystemCommandRequest = new ExecuteSystemCommandRequest({
        username: USERNAME.VALUE,
        password: PASSWORD.VALUE,
        timeoutSeconds: TIMEOUT_SECONDS.VALUE,
        runInBackground: RUN_IN_BACKGROUND.VALUE,
        command: COMMAND.VALUE,
      });

      EDGE.SEND_REQUEST(THIS.WEBSOCKET,
        new ComponentJsonApiRequest({
          componentId: "_host",
          payload: executeSystemCommandRequest,
        })).then(response => {
          const result = (response as ExecuteSystemCommandResponse).result;
          THIS.LOADING = false;
          if (RESULT.STDOUT.LENGTH == 0) {
            THIS.STDOUT = [""];
          } else {
            THIS.STDOUT = RESULT.STDOUT;
          }
          THIS.STDERR = RESULT.STDERR;

        }).catch(reason => {
          THIS.LOADING = false;
          THIS.STDERR = ["Error executing system command:", REASON.ERROR.MESSAGE];
        });
      THIS.COMMAND_LOGS.UNSHIFT(executeSystemCommandRequest);
    });
  }

}
