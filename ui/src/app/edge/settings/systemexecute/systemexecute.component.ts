import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
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

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
    private translate: TranslateService
  ) {
  }

  ngOnInit() {
    this.service.setCurrentComponent(this.translate.instant('Edge.Config.Index.systemExecute'), this.route);
  }

  public loading: boolean = false;
  public stdout: string[] = [];
  public stderr: string[] = [];
  public commandLogs: { username: string, password: string, timeoutSeconds: number; runInBackground: boolean, command: string }[] = [];

  public send(username: string, password: string, timeout: number, background: boolean, command: string) {
    this.service.getCurrentEdge().then(edge => {
      this.loading = true;
      this.stdout = [];
      this.stderr = [];
      edge.sendRequest(this.websocket,
        new ComponentJsonApiRequest({
          componentId: "_host",
          payload: new ExecuteSystemCommandRequest(
            { username: username, password: password, timeoutSeconds: timeout, runInBackground: background, command: command }
          )
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
      this.commandLogs.unshift({ username: username, password: password, timeoutSeconds: timeout, runInBackground: background, command: command });
    });
  }

}