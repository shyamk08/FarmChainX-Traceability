import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RetailersInventory } from './retailers-inventory';

describe('RetailersInventory', () => {
  let component: RetailersInventory;
  let fixture: ComponentFixture<RetailersInventory>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RetailersInventory]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RetailersInventory);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
