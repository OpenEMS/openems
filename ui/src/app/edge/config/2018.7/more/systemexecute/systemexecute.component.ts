import { Component, Input, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { Websocket, Service } from '../../../../../shared/shared';
import { Edge } from '../../../../../shared/edge/edge';

@Component({
  selector: 'systemexecute',
  templateUrl: './systemexecute.component.html'
})
export class SystemExecuteComponent implements OnInit {

  private stopOnDestroy: Subject<void> = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private websocket: Websocket,
  ) { }

  ngOnInit() {
    this.websocket.setCurrentEdge(this.route)
      .pipe(takeUntil(this.stopOnDestroy))
      .subscribe(edge => {
        this.edge = edge;
      });
  }

  public edge: Edge;
  public output: string = "";
  public commandLogs: { command: string, background: boolean, timeout: number }[] = [];

  public send(password: string, command: string, background: boolean, timeout: number) {
    this.edge.systemExecute(password, command, background, timeout).then(output => {
      this.output = output;
    });
    this.commandLogs.unshift({ command, background, timeout });
  }
}