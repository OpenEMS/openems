import { Component, OnInit, Input } from '@angular/core';

export abstract class CommonThingComponent {
  protected thing: string;

  protected init(data: any) {
    this.thing = data.key;
  }

  constructor() { }
}
