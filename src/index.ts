import { registerPlugin } from '@capacitor/core';

import type { MyketPlugin } from './definitions';

const Myket = registerPlugin<MyketPlugin>('Myket', {
  web: () => import('./web').then(m => new m.MyketWeb()),
});

export * from './definitions';
export { Myket };