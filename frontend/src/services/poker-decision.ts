import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Round } from '../round.model'; 

@Injectable({
  providedIn: 'root'
})
export class PokerDecisionService {

  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/rule-example';

  constructor() { }

  getDecision(roundData: Round): Observable<Round> {
    const url = `${this.apiUrl}/get-decision`;
    console.log('PokerDecisionService: Sending POST to', url, roundData);
    return this.http.post<Round>(url, roundData);
  }

  
  logRaise(playerName: string, amount: number): Observable<string> {
    const url = `${this.apiUrl}/log-raise`;
    let params = new HttpParams()
      .set('playerName', playerName)
      .set('amount', amount.toString());

    console.log('PokerDecisionService: Sending GET to', url, 'with params:', params.toString());
    return this.http.get(url, { params: params, responseType: 'text' });
  }

}