import { TestBed } from '@angular/core/testing';

import { PokerDecision } from './poker-decision';

describe('PokerDecision', () => {
  let service: PokerDecision;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PokerDecision);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
