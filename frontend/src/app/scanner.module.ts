

import { NgModule } from '@angular/core';
import { ZXingScannerModule } from '@zxing/ngx-scanner';

@NgModule({
  imports: [ZXingScannerModule],
  exports: [ZXingScannerModule]  
})
export class ScannerModule { }