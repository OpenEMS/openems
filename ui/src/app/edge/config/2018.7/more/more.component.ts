import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';
import { filter, first } from 'rxjs/operators';

import { Websocket, Service } from '../../../../shared/shared';
import { Edge } from '../../../../shared/edge/edge';

@Component({
  selector: 'more',
  templateUrl: './more.component.html'
})
export class MoreComponent implements OnInit {

  public edge: Edge;
  public manualMessageForm: FormGroup;

  constructor(
    private route: ActivatedRoute,
    private websocket: Websocket,
    private service: Service,
    private formBuilder: FormBuilder
  ) { }

  ngOnInit() {
    this.websocket.setCurrentEdge(this.route)
      .pipe(filter(edge => edge != null),
        first())
      .subscribe(edge => {
        this.edge = edge;
      });
    this.manualMessageForm = this.formBuilder.group({
      "message": this.formBuilder.control('')
    });
  }
}