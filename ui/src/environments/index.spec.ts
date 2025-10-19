import { getWebsocketScheme } from './index';

describe('getWebsocketScheme', () => {

  it('should return "wss" for "https:"', () => {
    expect(getWebsocketScheme("https:")).toBe('wss');
  });
  
  it('should return "ws" for "http:" protocol', () => {
    expect(getWebsocketScheme("http:")).toBe('ws');
  });
  
  it('should return "ws" for "file:" protocol', () => {
    expect(getWebsocketScheme("file:")).toBe('ws');
  });

  it('should return "ws" for empty strings', () => {
    expect(getWebsocketScheme("")).toBe('ws');
  });

  it('should work without parameter', () => {
    expect(() => getWebsocketScheme()).not.toThrow();
  });
});