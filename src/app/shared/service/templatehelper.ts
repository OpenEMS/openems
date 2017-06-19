import { Injectable } from '@angular/core';


@Injectable()
export class TemplateHelper {

  constructor(
  ) { }

  /**
   * Helps to use an object inside an *ngFor loop. Returns the object keys.
   * Source: https://stackoverflow.com/a/39896058
   */
  keys(object: {}) {
    return Object.keys(object);
  }

  /**
   * Helps to use an object inside an *ngFor loop. Returns the object key value pairs.
   */
  keyvalues(object: {}) {
    if (!object) {
      return object;
    }
    let keyvalues = [];
    for (let key in object) {
      keyvalues.push({ key: key, value: object[key] });
    }
    return keyvalues;
  }
}