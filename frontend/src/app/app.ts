import { Component, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { PokerDecisionService } from '../services/poker-decision'; // Corrected path assuming 'services' folder is in 'app'
import { Round } from '../round.model'; // Corrected path assuming 'round.model.ts' is in 'app'

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {

  private pokerService = inject(PokerDecisionService);

  // --- Input polja za Setup Igre ---
  numberOfPlayersInput: number = 4;
  startingChipsInput: number = 1000;
  playerPositionInput: number = 2; // Naš igrač je p2 (index 2)
  bigBlindSizeInput: number = 10;
  cardRanks: string[] = ['A', 'K', 'Q', 'J', 'T', '9', '8', '7', '6', '5', '4', '3', '2'];
  selectedCard1Rank: string = 'A';
  selectedCard2Rank: string = 'K';
  cardsAreSuited: boolean = false;

  // --- Stanje Igre ---
  roundNum: number = 0;
  players: string[] = [];
  playerChipsMap: { [key: string]: number } = {};
  playerActionsMap: { [key: string]: number[] } = {};
  ourPlayerName: string = '';
  pot: number = 0;
  currentRaise: number = 0;
  bigBlindSize: number = 0;
  currentHand: string = '';
  betsThisRound: { [key: string]: number } = {};

  // --- Praćenje toka igre ---
  currentPlayerIndex: number = -1;
  lastAggressorIndex: number = -1;
  foldedPlayers: Set<string> = new Set();
  roundOver: boolean = false;
  winner: string | null = null;

  // --- Za Prikaz ---
  gameStarted: boolean = false;
  currentDecision: string | null = null;
  errorMessage: string | null = null;
  logMessage: string | null = null; // For displaying messages from CEP logging

  overallWinner: string | null = null;
  // --- Input za akcije igrača ---
  selectedAction: 'END' | 'FOLD' | 'CALL' | 'RAISE' = 'CALL'; // Removed GET_DECISION and DECLARE_WINNER here
  raiseAmountInput: number | null = null;
  selectedWinner: string = '';

  constructor() { }

  ngOnInit() {
    console.log('App Component Initialized');
  }

  startGame() {
    // --- Validation ---
     if (this.numberOfPlayersInput < 2) { this.errorMessage = "Min 2 players."; return; }
     // Use 1-based input for user-friendliness, check against number of players
     if (this.playerPositionInput < 1 || this.playerPositionInput > this.numberOfPlayersInput) {
       this.errorMessage = `Your position must be between 1 and ${this.numberOfPlayersInput}.`;
       return;
     }
     if (!this.bigBlindSizeInput || this.bigBlindSizeInput <= 0) { this.errorMessage = "BB must be positive."; return; }

    // --- State Initialization ---
    this.roundNum = 1;
    this.bigBlindSize = this.bigBlindSizeInput;
    this.pot = 0;
    this.currentRaise = this.bigBlindSize; // Amount to reach
    this.roundOver = false;
    this.winner = null;
    this.currentDecision = null;
    this.errorMessage = null; // Clear previous errors
    this.logMessage = null; // Clear log message

    // --- Player Setup ---
    this.players = [];
    this.playerChipsMap = {}; // Reset chips only if needed (e.g., first round)
    this.playerActionsMap = {};
    this.betsThisRound = {};
    this.foldedPlayers.clear();
    for (let i = 0; i < this.numberOfPlayersInput; i++) {
      const playerIndex = i + 1; // 1-based index for name p1, p2...
      const playerName = `p${playerIndex}`;
      this.players.push(playerName);
       // Use existing chips if round > 1, otherwise starting chips
      this.playerChipsMap[playerName] = (this.roundNum > 1 && this.playerChipsMap[playerName] !== undefined)
                                          ? this.playerChipsMap[playerName]
                                          : this.startingChipsInput;
      this.playerActionsMap[playerName] = [];
      this.betsThisRound[playerName] = 0;
    }
    this.ourPlayerName = `p${this.playerPositionInput}`; // Our name uses 1-based input

    // --- Posting Blinds ---
    const sbAmount = Math.floor(this.bigBlindSize / 2);
    const bbAmount = this.bigBlindSize;
    let smallBlindPlayerIndex = -1; // 0-based array index
    let bigBlindPlayerIndex = -1;   // 0-based array index

    // Standard blind positions (adjust if needed for < 3 players based on rules)
     if (this.players.length >= 2) {
         smallBlindPlayerIndex = 0; // p1 is SB
     }
     if (this.players.length >= 2) { // Allow BB even in HU
        bigBlindPlayerIndex = 1; // p2 is BB
     }
     // Adjust for fewer players if needed (e.g., HU logic might differ)

    // Post SB
    if (smallBlindPlayerIndex !== -1 && this.players[smallBlindPlayerIndex]) {
      const sbPlayer = this.players[smallBlindPlayerIndex];
      const sbActual = Math.min(sbAmount, this.playerChipsMap[sbPlayer]);
      this.playerChipsMap[sbPlayer] -= sbActual;
      this.pot += sbActual;
      this.betsThisRound[sbPlayer] = sbActual;
      console.log(`${sbPlayer} posts Small Blind: ${sbActual}`);
    }

    // Post BB
    if (bigBlindPlayerIndex !== -1 && this.players[bigBlindPlayerIndex]) {
      const bbPlayer = this.players[bigBlindPlayerIndex];
      const bbActual = Math.min(bbAmount, this.playerChipsMap[bbPlayer]);
      this.playerChipsMap[bbPlayer] -= bbActual;
      this.pot += bbActual;
      this.betsThisRound[bbPlayer] = bbActual;
      this.lastAggressorIndex = bigBlindPlayerIndex; // BB is the last aggressor initially
      console.log(`${bbPlayer} posts Big Blind: ${bbActual}`);
    } else if (smallBlindPlayerIndex !== -1) {
      // If no BB (e.g., if logic prevented it), SB is last aggressor
      this.lastAggressorIndex = smallBlindPlayerIndex;
    }

    // --- Determine Starting Player ---
     // Generally player after BB. Adjust index logic carefully.
     // Standard: Player after BB acts first (UTG). Index = (BB index + 1) % numPlayers
     let startIndex = -1;
     if (bigBlindPlayerIndex !== -1) {
        startIndex = (bigBlindPlayerIndex + 1) % this.players.length;
     } else if (smallBlindPlayerIndex !== -1) {
        // Edge case: if only SB posted, player after SB starts
        startIndex = (smallBlindPlayerIndex + 1) % this.players.length;
     } else if (this.players.length > 0) {
        startIndex = 0; // Fallback if no blinds somehow
     }

     // Heads Up (2 players) specific: Button/SB (index 0) acts first pre-flop
     if (this.players.length === 2) {
         startIndex = 0;
     }

    this.currentPlayerIndex = startIndex;
    if (this.currentPlayerIndex !== -1) {
        console.log(`Action starts with player at index ${this.currentPlayerIndex} (${this.players[this.currentPlayerIndex]})`);
        // Skip folded players if blinds caused folds (unlikely but possible)
        while(this.foldedPlayers.has(this.getCurrentPlayerName())) {
            this.currentPlayerIndex = (this.currentPlayerIndex + 1) % this.players.length;
            // Add safety break if somehow all fold instantly
        }
        console.log(`Actual start player after skips: ${this.getCurrentPlayerName()}`);

    } else {
         console.error("Could not determine starting player index.");
         this.errorMessage = "Error determining starting player.";
         return; // Don't start game if index is invalid
    }


    // --- Hand String ---
    if (this.selectedCard1Rank === this.selectedCard2Rank) {
        this.currentHand = `${this.selectedCard1Rank}${this.selectedCard2Rank}`; // npr. "AA"
    } else {
        const rank1Index = this.cardRanks.indexOf(this.selectedCard1Rank);
        const rank2Index = this.cardRanks.indexOf(this.selectedCard2Rank);
        const firstRank = rank1Index < rank2Index ? this.selectedCard1Rank : this.selectedCard2Rank;
        const secondRank = rank1Index < rank2Index ? this.selectedCard2Rank : this.selectedCard1Rank;
        this.currentHand = `${firstRank}${secondRank}${this.cardsAreSuited ? 's' : 'o'}`; // npr. "AKs" ili "KQo"
    }

    // --- Finalize Start ---
    this.gameStarted = true;
    console.log('--- Round Start ---'); /* ... logs ... */
    console.log('--------------------');
  }

  startNextRound() {
      this.errorMessage = null;
      this.logMessage = null;

      if (this.playerChipsMap[this.ourPlayerName] <= 0) {
        this.errorMessage = `You have 0 chips and cannot continue. Game Over for you.`;
        console.log(`Cannot start next round: ${this.ourPlayerName} has 0 chips.`);
        return;
    }

      // 1. Izbaci igrače sa 0 čipova
      const remainingPlayerNames = this.players.filter(p => this.playerChipsMap[p] > 0);

      // 2. Proveri da li imamo pobednika partije
      if (remainingPlayerNames.length <= 1) {
          this.overallWinner = remainingPlayerNames.length === 1 ? remainingPlayerNames[0] : "Draw/Error?";
          console.log(`GAME OVER! Winner is ${this.overallWinner}`);
          this.errorMessage = `GAME OVER! Winner: ${this.overallWinner}`;
          this.gameStarted = false;
          return;
      }

      // 3. Rotiraj pozicije igrača koji su ostali
      const rotatedPlayers = [...remainingPlayerNames]; // Napravi kopiju
      const firstPlayer = rotatedPlayers.shift(); // Uzmi prvog
      if (firstPlayer) {
          rotatedPlayers.push(firstPlayer); // Stavi ga na kraj
      }
      this.players = rotatedPlayers; // Postavi novi redosled igrača

      console.log("Starting next round. Keeping player array order, rotating blinds/button.");
      this.setupNewRound(); // Prosledićemo true da signaliziramo da je sledeća runda
  }

  private setupNewRound() {
    this.roundNum++;
    this.bigBlindSize = this.bigBlindSizeInput; 
    this.pot = 0;
    this.currentRaise = this.bigBlindSize;
    this.roundOver = false;
    this.winner = null;
    this.currentDecision = null;
    this.errorMessage = null;
    this.logMessage = null;

    // Resetuj uloge i foldovane igrače za novu rundu
    this.betsThisRound = {};
    this.foldedPlayers.clear();
    this.players.forEach(playerName => {
        if (this.playerChipsMap[playerName] > 0) {
            this.betsThisRound[playerName] = 0;
        } else {
            // Igrači sa 0 čipova se tehnički foldovani od starta? Ili ih ignorišemo?
             this.foldedPlayers.add(playerName); // Dodaj igrače sa 0 čipova u foldovane
        }
    });

    // --- Postavljanje Blindova (sa rotacijom) ---
    const sbAmount = Math.floor(this.bigBlindSize / 2);
    const bbAmount = this.bigBlindSize;
    let smallBlindPlayerIndex = -1;
    let bigBlindPlayerIndex = -1;

     const activePlayerIndices = this.players
         .map((_, index) => index)
         .filter(index => this.playerChipsMap[this.players[index]] > 0);

     if (activePlayerIndices.length >= 2) {
         smallBlindPlayerIndex = activePlayerIndices[0];
         bigBlindPlayerIndex = activePlayerIndices[1];
     } else if (activePlayerIndices.length === 1) {
         // Samo jedan igrač ostao - ovo je trebalo da uhvati startNextRound
         console.error("SetupNewRound called with only one active player.");
         this.overallWinner = this.players[activePlayerIndices[0]];
         this.roundOver = true;
         this.gameStarted = false;
         return;
     }
     // Ako nema aktivnih igrača, greška

    // Post SB
    if (smallBlindPlayerIndex !== -1) {
      const sbPlayer = this.players[smallBlindPlayerIndex];
      const sbActual = Math.min(sbAmount, this.playerChipsMap[sbPlayer]);
      this.playerChipsMap[sbPlayer] -= sbActual;
      this.pot += sbActual;
      this.betsThisRound[sbPlayer] = sbActual;
      console.log(`${sbPlayer} posts Small Blind: ${sbActual}`);
    }

    // Post BB
    if (bigBlindPlayerIndex !== -1) {
      const bbPlayer = this.players[bigBlindPlayerIndex];
      const bbActual = Math.min(bbAmount, this.playerChipsMap[bbPlayer]);
      this.playerChipsMap[bbPlayer] -= bbActual;
      this.pot += bbActual;
      this.betsThisRound[bbPlayer] = bbActual;
      this.lastAggressorIndex = bigBlindPlayerIndex; // BB je poslednji agresor
      console.log(`${bbPlayer} posts Big Blind: ${bbActual}`);
    } else if (smallBlindPlayerIndex !== -1) {
      this.lastAggressorIndex = smallBlindPlayerIndex; // SB je agresor ako nema BB
    }

    // --- Određivanje ko počinje akciju ---
    let startIndex = -1;
    if (bigBlindPlayerIndex !== -1) {
      startIndex = bigBlindPlayerIndex; // Počni od BB indexa
      // Vrti dok ne nađeš sledećeg aktivnog igrača
      do {
          startIndex = (startIndex + 1) % this.players.length;
      } while (!activePlayerIndices.includes(startIndex)); // Traži sledećeg u listi aktivnih indeksa
    } else if (smallBlindPlayerIndex !== -1) {
         startIndex = smallBlindPlayerIndex; // Počni od SB indexa
         do {
             startIndex = (startIndex + 1) % this.players.length;
         } while (!activePlayerIndices.includes(startIndex));
    } else if (activePlayerIndices.length > 0) {
        startIndex = activePlayerIndices[0]; // Ako nema blindova, počinje prvi aktivni
    }

    // Heads Up: Button/SB (prvi aktivni) počinje
    if (activePlayerIndices.length === 2) {
        startIndex = activePlayerIndices[0]; // Button uvek prvi igra HU preflop
    }


    this.currentPlayerIndex = startIndex;
    if (this.currentPlayerIndex !== -1) {
        console.log(`Action starts with player ${this.players[this.currentPlayerIndex]} (index ${this.currentPlayerIndex})`);
    } else {
         console.error("Could not determine starting player index for new round.");
         this.errorMessage = "Error determining starting player.";
         // Možda zaustaviti igru?
         this.roundOver = true; // Označi kraj da se može resetovati
         return;
    }

    // --- Generisanje NOVE ruke za našeg igrača ---
    if (this.selectedCard1Rank === this.selectedCard2Rank) {
      this.currentHand = `${this.selectedCard1Rank}${this.selectedCard2Rank}`;
    } else {
      const rank1Index = this.cardRanks.indexOf(this.selectedCard1Rank);
      const rank2Index = this.cardRanks.indexOf(this.selectedCard2Rank);
      const firstRank = rank1Index < rank2Index ? this.selectedCard1Rank : this.selectedCard2Rank;
      const secondRank = rank1Index < rank2Index ? this.selectedCard2Rank : this.selectedCard1Rank;
      this.currentHand = `${firstRank}${secondRank}${this.cardsAreSuited ? 's' : 'o'}`;
    }
    console.log(`Generated Hand String for round ${this.roundNum}: ${this.currentHand}`);

    // --- Finalize ---
    this.gameStarted = true; // Osiguraj da je true ako je ovo pozvano iz startNextRound
    console.log(`--- Round ${this.roundNum} Start ---`);
    console.log('Players:', this.players.filter(p => this.playerChipsMap[p] > 0)); // Prikaži samo aktivne
    console.log('Chips After Blinds:', this.playerChipsMap);
    console.log('Bets This Round:', this.betsThisRound);
    console.log('Pot:', this.pot);
    console.log(`Our Player (${this.ourPlayerName}) Hand: ${this.currentHand}`);
    console.log('--------------------');
  }


   isOurTurn(): boolean {
       // Pronađi TRENUTNI index našeg igrača u nizu `players`
       const ourCurrentIndex = this.players.indexOf(this.ourPlayerName);
       // Ako naš igrač nije nađen (npr. izbačen), nikad nije naš red
       if (ourCurrentIndex === -1) return false;

       return this.gameStarted && !this.roundOver && this.currentPlayerIndex === ourCurrentIndex;
   }

  minRaiseAmount(): number {
       return this.bigBlindSize;
   }

  getCurrentPlayerName(): string {
    return this.players[this.currentPlayerIndex] || 'N/A';
  }

  // isOurTurn(): boolean {
  //   const ourPlayerIndex = this.playerPositionInput - 1; // Convert 1-based input to 0-based index
  //   return this.gameStarted && !this.roundOver && this.currentPlayerIndex === ourPlayerIndex;
  // }

  didWeFold(): boolean {
    return this.foldedPlayers.has(this.ourPlayerName);
  }

  getActivePlayers(): string[] {
    return this.players.filter(p => !this.foldedPlayers.has(p));
  }

  nextPlayer() {
    if (this.roundOver) return;

    const activePlayers = this.getActivePlayers();
    if (activePlayers.length <= 1) {
      this.endRound();
      return;
    }

    let nextIndex = this.currentPlayerIndex;
    let loopSafety = 0;
    do {
      nextIndex = (nextIndex + 1) % this.players.length;
      loopSafety++;
      if (loopSafety > this.players.length * 2) {
          console.error("Infinite loop safety break in nextPlayer!");
          this.roundOver = true;
          this.errorMessage = "Error finding next player.";
          return;
      }
    // Continue looping IF the player folded OR has 0 chips
    } while (this.foldedPlayers.has(this.players[nextIndex]) ||
             (this.playerChipsMap[this.players[nextIndex]] === 0) );

    this.currentPlayerIndex = nextIndex;
    console.log(`Next player is index ${this.currentPlayerIndex} (${this.getCurrentPlayerName()})`);

    if (this.isOurTurn()) {
      this.currentDecision = null;
    }
  }


  getCurrentMaxBet(): number {
    // Ensure betsThisRound is not empty and values are numbers
      const bets = Object.values(this.betsThisRound).filter(bet => typeof bet === 'number');
      return Math.max(0, ...bets);
  }

  performFold() {
    if (this.roundOver || this.foldedPlayers.has(this.getCurrentPlayerName())) return;
    const player = this.getCurrentPlayerName();
    console.log(`${player} FOLDS`);
    this.foldedPlayers.add(player);
    this.nextPlayer(); // Move to next player after fold
  }

  performCall() {
      if (this.roundOver || this.foldedPlayers.has(this.getCurrentPlayerName()) || this.playerChipsMap[this.getCurrentPlayerName()] === 0) return;

      const player = this.getCurrentPlayerName();
      const maxBet = this.getCurrentMaxBet();
      const currentBet = this.betsThisRound[player] || 0;
      const amountToCall = maxBet - currentBet;
      const isClosingAction = this.currentPlayerIndex === this.lastAggressorIndex; // Da li je ovo igrač koji može da zatvori akciju?

      if (amountToCall <= 0) {
        console.log(`${player} CHECKS`);
        this.nextPlayer();
        return;
      }

      // Ostatak performCall logike (za stvarni call) ostaje isti...
      const playerChips = this.playerChipsMap[player];
      const actualCallAmount = Math.min(amountToCall, playerChips);
      console.log(`${player} CALLS ${actualCallAmount}`);
      this.playerChipsMap[player] -= actualCallAmount;
      this.betsThisRound[player] += actualCallAmount;
      this.pot += actualCallAmount;

      // Proveri da li ovaj CALL zatvara akciju
      // Akcija se zatvara ako SVI aktivni igrači sada imaju isti ulog (ili su all-in)
      const newMaxBet = this.getCurrentMaxBet(); // Max bet je možda promenjen ako je neko bio all-in za manje
      const allActivePlayersNowMatched = this.getActivePlayers()
          .every(p => this.betsThisRound[p] === newMaxBet || this.playerChipsMap[p] === 0);

      if (allActivePlayersNowMatched) {
          console.log("Player called. Checking next player...");
          this.nextPlayer();

      } else {
          this.nextPlayer();
      }
  }

  performRaise() {
    if (this.roundOver || this.foldedPlayers.has(this.getCurrentPlayerName()) || this.playerChipsMap[this.getCurrentPlayerName()] === 0) return;
    if (this.raiseAmountInput === null || this.raiseAmountInput <= 0) {
      this.errorMessage = "Invalid raise amount.";
      return;
    }

    const player = this.getCurrentPlayerName();
    const maxBet = this.getCurrentMaxBet(); // The current highest total bet this round
    const currentBet = this.betsThisRound[player] || 0; // How much this player already put in
    const amountToCall = maxBet - currentBet; // How much more to match the current max bet
    const requestedRaiseAmount = this.raiseAmountInput; // How much MORE than maxBet player wants to make it

    // Total amount player needs to put into the pot *in this action*
    const totalPutInPotThisAction = amountToCall + requestedRaiseAmount;
    const playerChips = this.playerChipsMap[player];

     // Calculate minimum legal raise amount (usually double the previous bet/raise, or BB if first raise)
     // Simplified: minimum raise is at least the big blind
     const minRaiseAmount = this.bigBlindSize;
     if(requestedRaiseAmount < minRaiseAmount && (playerChips > totalPutInPotThisAction) ){ // Allow smaller raise only if all-in
         this.errorMessage = `Minimum raise amount is ${minRaiseAmount}.`;
         return;
     }

    // Check if player has enough chips
    if (totalPutInPotThisAction > playerChips) {
      this.errorMessage = `${player} doesn't have enough chips. Max possible bet/raise is ${playerChips}. Performing all-in.`;
      // Adjust to all-in amount
       const allInAmount = playerChips;
       const actualRaiseAmount = allInAmount - amountToCall; // How much the raise *actually* is above the call

       console.log(`${player} goes ALL-IN for ${allInAmount} (raising by ${actualRaiseAmount})`);
       this.playerChipsMap[player] = 0; // No chips left
       this.betsThisRound[player] += allInAmount;
       this.pot += allInAmount;
       this.currentRaise = this.betsThisRound[player]; // New amount to call is their total bet
       this.lastAggressorIndex = this.currentPlayerIndex; // They are the last aggressor
       this.logRaiseAutomatically(player, actualRaiseAmount); // Log the actual raise amount

    } else {
        // Standard Raise
        console.log(`${player} RAISES by ${requestedRaiseAmount} (total bet this round: ${currentBet + totalPutInPotThisAction})`);
        this.playerChipsMap[player] -= totalPutInPotThisAction;
        this.betsThisRound[player] += totalPutInPotThisAction;
        this.pot += totalPutInPotThisAction;
        this.currentRaise = this.betsThisRound[player]; // New amount to call is their total bet
        this.lastAggressorIndex = this.currentPlayerIndex; // They are the last aggressor
        this.logRaiseAutomatically(player, requestedRaiseAmount); // Log the requested raise amount
    }

    this.raiseAmountInput = null;
    this.errorMessage = null;
    this.nextPlayer();
  }

  handleAction() {
    this.errorMessage = null; // Clear previous errors

    // *** ISPRAVKA: Uklonjena provera if(this.isOurTurn()) ***
    // Sada dozvoljavamo direktnu akciju i za našeg igrača

    switch (this.selectedAction) {
      case 'FOLD':
        this.performFold();
        break;
      case 'CALL':
        this.performCall();
        break;
      case 'RAISE':
        this.performRaise();
        break;
      case 'END':
        //kod gde ce se zavrsiti runda i izabrati pobednik
        this.manualEndRound();
        break;
    }
    // Resetuj selekciju na default NAKON akcije
    this.selectedAction = 'CALL';
  }

  manualEndRound() {
    if (this.roundOver) {
        console.warn("Manual end round called when round already over.");
        return;
    }

    console.log("User manually ended the betting round via select option.");
    this.roundOver = true;
    this.currentDecision = null;
    this.errorMessage = null; 
    console.log("Betting round ended manually. Please declare a winner.");
  }

  getDecisionForOurPlayer() {
    if (!this.isOurTurn()) return;
    this.currentDecision = "Getting suggestion...";
    this.errorMessage = null;

    try {
      const ourPlayerIndex = this.playerPositionInput - 1; // 0-based index
      const roundToSend = new Round(
        this.currentHand, this.roundNum, ourPlayerIndex, // Use 0-based index
        this.players,
        this.players.map(p => this.playerChipsMap[p]),
        // Send the amount needed TO CALL the current raise, relative to player's current bet
         Math.max(0, this.currentRaise - (this.betsThisRound[this.ourPlayerName] || 0)),
        //this.currentRaise, // What is the current *total* bet level
        this.pot, this.bigBlindSize
      );

      console.log('Sending current state for decision:', roundToSend);

      this.pokerService.getDecision(roundToSend).subscribe({
        next: (response) => {
          console.log('Received suggestion:', response.suggestedAction);
          this.currentDecision = response.suggestedAction || 'Error: No suggestion received';
        },
        error: (err) => {
          console.error('Error getting decision:', err);
          this.errorMessage = `Error getting suggestion: ${err.message}`;
          this.currentDecision = 'Error';
           if (err.error && typeof err.error === 'string') {
               this.errorMessage += ` Server says: ${err.error}`;
           } else if (err.error?.message) {
               this.errorMessage += ` Server error: ${err.error.message}`;
           }
        }
      });
    } catch (e: any) {
      console.error('Error creating Round object for decision:', e);
      this.errorMessage = `Error preparing data for backend: ${e.message}`;
      this.currentDecision = 'Error';
    }
  }

  endRound() {
    if (this.roundOver) return;
    const activePlayers = this.getActivePlayers();
    if (activePlayers.length === 1) {
      this.winner = activePlayers[0];
      console.log(`Round Over. ${this.winner} wins the pot of ${this.pot}.`);
      this.playerChipsMap[this.winner] += this.pot;
      this.roundOver = true;
    } else {
      console.warn("endRound called but more than one player is active.");
      this.endBettingRound(); // Treat as end of betting if called early
    }
  }

  endBettingRound() {
    console.log("--- Betting Round Ends ---");
    // In a real game, deal next street or show down. Here, we end the round.
    this.roundOver = true;
    // Check if winner was already determined by folds during this function call
     if (!this.winner && this.getActivePlayers().length === 1) {
         this.winner = this.getActivePlayers()[0];
         console.log(`Winner by default (last active): ${this.winner}. Awarding pot ${this.pot}`);
          this.playerChipsMap[this.winner] += this.pot;
     } else if (!this.winner) {
         console.log("Betting round ended with multiple players. Please declare a winner.");
     }
  }

  declareWinnerManually() {
     if (this.roundOver && this.winner) {
        this.errorMessage = `Winner (${this.winner}) already determined.`;
        return;
     }
    if (!this.selectedWinner) { this.errorMessage = "Please select a winner."; return; }
    if (this.foldedPlayers.has(this.selectedWinner)) { this.errorMessage = "Folded player cannot win."; return; }

    this.winner = this.selectedWinner;
    console.log(`Manually declared winner: ${this.winner}. Awarding pot of ${this.pot}.`);
    this.playerChipsMap[this.winner] += this.pot;
    this.roundOver = true;
    this.errorMessage = null;
  }

  private logRaiseAutomatically(playerName: string, raiseAmount: number) {
    console.log(`Automatically logging raise for ${playerName} amount ${raiseAmount}`);
    this.pokerService.logRaise(playerName, raiseAmount).subscribe({
      next: (response) => {
        console.log('CEP Log Response:', response);
        this.logMessage = response; // Show confirmation/message from backend
      },
      error: (err) => {
        console.error('Error automatically logging raise for CEP:', err);
        // Display error subtly, don't block game flow
        this.logMessage = `CEP Log Error: ${err.message}`;
      }
    });
  }

  resetGame() {
      // 1. Vrati prikaz na setup formu
      this.gameStarted = false;

      // 2. Resetuj brojač rundi
      this.roundNum = 0;

      // 3. (Opciono) Vrati setup input polja na default vrednosti
      this.numberOfPlayersInput = 4;
      this.startingChipsInput = 1000;
      this.playerPositionInput = 2; // Tvoja default pozicija (1-based)
      this.bigBlindSizeInput = 10;
      this.selectedCard1Rank = 'A'; // Default karta 1
      this.selectedCard2Rank = 'K'; // Default karta 2
      this.cardsAreSuited = false; // Default: offsuit

      // 4. Obriši sve podatke o trenutnoj igri/rundi
      this.players = []; // Isprazni listu igrača
      this.playerChipsMap = {}; // Obriši stanje čipova
      this.betsThisRound = {}; // Obriši uloge iz runde
      this.foldedPlayers.clear(); // Obriši listu foldovanih
      this.pot = 0; // Resetuj pot
      this.currentRaise = 0; // Resetuj raise
      this.bigBlindSize = 0; // Biće postavljeno iz inputa u sledećem startGame
      this.currentHand = ''; // Obriši trenutnu ruku
      this.ourPlayerName = ''; // Obriši ime našeg igrača

      // 5. Resetuj promenljive za praćenje toka
      this.currentPlayerIndex = -1;
      this.lastAggressorIndex = -1;

      // 6. Resetuj statusne flag-ove
      this.roundOver = false;
      this.winner = null; // Obriši pobednika runde
      this.overallWinner = null; // Obriši pobednika cele partije

      // 7. Obriši poruke za korisnika
      this.currentDecision = null; // Obriši sugestiju
      this.errorMessage = null; // Obriši poruke o grešci
      this.logMessage = null; // Obriši CEP log poruke

      // 8. Resetuj inpute za akcije
      this.selectedAction = 'CALL'; // Vrati na default akciju
      this.raiseAmountInput = null; // Obriši uneti raise iznos
      this.selectedWinner = ''; // Obriši selektovanog pobednika

      // 9. Loguj akciju (opciono)
      console.log('Game state fully reset. Ready for new setup.');
  }

}