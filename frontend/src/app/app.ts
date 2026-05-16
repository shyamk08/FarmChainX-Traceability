import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Navbar } from './components/navbar/navbar';
import { TranslateService } from '@ngx-translate/core';
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, Navbar],
  templateUrl: './app.html',
  styleUrls: ['./app.scss']
})
export class App {
  constructor(private translate: TranslateService) {
    this.translate.addLangs(['en', 'ta', 'hi']);
    this.translate.setDefaultLang('en');
    this.translate.use('en');
  }
}
