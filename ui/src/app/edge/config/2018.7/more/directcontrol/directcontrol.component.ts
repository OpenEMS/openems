import { Component, Input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { Websocket, Service } from '../../../../../shared/shared';
import { Edge } from '../../../../../shared/edge/edge';
import { DefaultMessages } from '../../../../../shared/service/defaultmessages';

@Component({
  selector: 'directcontrol',
  templateUrl: './directcontrol.component.html'
})
export class DirectControlComponent {

  public edge: Edge;
  public forms: FormGroup[] = [];

  private stopOnDestroy: Subject<void> = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private websocket: Websocket,
    private formBuilder: FormBuilder
  ) { }

  ngOnInit() {
    this.websocket.setCurrentEdge(this.route)
      .pipe(takeUntil(this.stopOnDestroy))
      .subscribe(edge => {
        this.edge = edge;
      });
    this.addLine();
  }

  public send() {
    for (let form of this.forms) {
      let thing = form.value["thing"];
      let channel = form.value["channel"];
      let value = form.value["value"];
      this.edge.send(DefaultMessages.configUpdate(this.edge.edgeId, thing, channel, value));
    }
  }

  public addLine() {
    this.forms.push(this.formBuilder.group({
      "thing": this.formBuilder.control(''),
      "channel": this.formBuilder.control(''),
      "value": this.formBuilder.control('')
    }));
  }

  public removeLine(index: number) {
    this.forms.splice(index - 1, 1);
  }
}