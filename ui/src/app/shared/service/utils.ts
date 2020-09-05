import { Injectable } from '@angular/core';

@Injectable()
export class Utils {

  /**
   * Promise with timeout
   * Source: https://italonascimento.github.io/applying-a-timeout-to-your-promises/
   */
  public static timeoutPromise = function (ms: number, promise: any) {
    // Create a promise that rejects in <ms> milliseconds
    let timeout = new Promise((resolve, reject) => {
      let id = setTimeout(() => {
        clearTimeout(id);
        reject('Timed out in ' + ms + 'ms.')
      }, ms)
    })

    // Returns a race between our timeout and the passed in promise
    return Promise.race([
      promise,
      timeout
    ])
  }

  constructor() { }

  /**
   * Helps to use an object inside an *ngFor loop. Returns the object keys.
   * Source: https://stackoverflow.com/a/39896058
   */
  public keys(object: {}): string[] {
    return Object.keys(object);
  }

  /**
   * Helps to use an object inside an *ngFor loop. Returns the object key value pairs.
   */
  public keyvalues(object: {}): any[] | {} {
    if (!object) {
      return object;
    }
    let keyvalues = [];
    for (let key in object) {
      keyvalues.push({ key: key, value: object[key] });
    }
    return keyvalues;
  }

  /**
   * Helps to use an object inside an *ngFor loop. Returns the object values.
   * Source: https://stackoverflow.com/a/39896058
   */
  public values(object: {}): any[] {
    let values = [];
    for (let key in object) {
      values.push(object[key]);
    }
    return values;
  }

  /**
   * Returns true if an object has a property
   */
  public has(object: {}, property: string): boolean {
    if (property in object) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Returns a sorted array
   */
  public sort(obj: any[], ascending: boolean = true, property?: string) {
    if (obj == null) {
      return obj;
    }
    return obj.sort((a, b) => {
      if (property) {
        a = a[property];
        b = b[property];
      }
      let result = 0;
      if (a > b) {
        result = 1;
      } else if (a < b) {
        result = -1;
      }
      if (!ascending) {
        result *= -1;
      }
      return result;
    })
  }

  /**
   * Returns true for last element of array
   * @param element
   * @param array
   */
  public static isLastElement(element: any, array: any[]) {
    return element == array[array.length - 1];
  }

  /**
   * Returns the short classname
   */
  public classname(value: string): string {
    let parts = value.split(".");
    return parts[parts.length - 1];
  }

  /**
   * Creates a deep copy of the object
   */
  public static deepCopy(obj: any, target?: any) {
    let copy: any;

    // Handle the 3 simple types, and null or undefined
    if (null == obj || "object" != typeof obj) return obj;

    // Handle Date
    if (obj instanceof Date) {
      if (target) {
        copy = target;
      } else {
        copy = new Date();
      }
      copy.setTime(obj.getTime());
      return copy;
    }

    // Handle Array
    if (obj instanceof Array) {
      if (target) {
        copy = target;
      } else {
        copy = [];
      }
      for (let i = 0, len = obj.length; i < len; i++) {
        copy[i] = this.deepCopy(obj[i]);
      }
      return copy;
    }

    // Handle Object
    if (obj instanceof Object) {
      if (target) {
        copy = target;
      } else {
        copy = {};
      }
      for (let attr in obj) {
        if (obj.hasOwnProperty(attr)) {
          copy[attr] = this.deepCopy(obj[attr], copy[attr]);
        }
      }
      return copy;
    }

    throw new Error("Unable to copy obj! Its type isn't supported.");
  }

  /**
   * Safely adds two - possibly 'null' - values: v1 + v2
   * 
   * @param v1 
   * @param v2 
   */
  public static addSafely(v1: number | null, v2: number | null): number | null {
    if (v1 == null && v2 != null) {
      return v2;
    } else if (v1 != null && v2 == null) {
      return v1;
    } else if (v1 != null && v2 != null) {
      return v1 + v2;
    } else {
      return null;
    }
  }

  /**
   * Safely subtracts two - possibly 'null' - values: v1 - v2
   * 
   * @param v1 
   * @param v2 
   */
  public static subtractSafely(v1: number | null, v2: number | null): number | null {
    if (v1 == null && v2 != null) {
      return v2;
    } else if (v1 != null && v2 == null) {
      return v1;
    } else if (v1 != null && v2 != null) {
      return v1 - v2;
    } else {
      return null;
    }
  }

  /**
   * Safely divides two - possibly 'null' - values: v1 / v2
   * 
   * @param v1 
   * @param v2 
   */
  public static divideSafely(v1: number | null, v2: number | null): number | null {
    if (v1 == null || v2 == null) {
      return null;
    } else if (v2 == 0) {
      return null; // divide by zero
    } else {
      return v1 / v2;
    }
  }

  /**
   * Safely multiplies two - possibly 'null' - values: v1 * v2
   * 
   * @param v1 
   * @param v2 
   */
  public static multiplySafely(v1: number | null, v2: number | null): number | null {
    if (v1 == null || v2 == null) {
      return null;
    } else {
      return v1 * v2;
    }
  }

  /**
   * Safely rounds a - possibly 'null' - value: Math.round(v)
   * 
   * @param v 
   */
  public static roundSafely(v: number): number {
    if (v == null) {
      return v;
    } else {
      return Math.round(v);
    }
  }

  /**
   * Gets the value; or if it is null, gets the 'orElse' value
   * 
   * @param v      the value or null
   * @param orElse the default value
   * @returns      the value or the default value
   */
  public static orElse(v: number | null, orElse: number): number {
    if (v == null) {
      return orElse;
    } else {
      return v;
    }
  }

  /**
   * Matches all filter-strings with all base-strings.
   * 
   * @param filters array of filter-strings
   * @param bases   array of base-strings
   * @returns       true if all filter strings exist in any base-strings
   */
  public static matchAll(filters: string[], bases: string[]): Boolean {
    for (let filter of filters) {
      let filterMatched = false;
      for (let base of bases) {
        if (base.includes(filter)) {
          filterMatched = true;
        }
      }
      if (!filterMatched) {
        return false;
      }
    }
    return true;
  }
}