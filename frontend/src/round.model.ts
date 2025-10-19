export class Round {
  hand: string;
  roundNum: number;
  playerPosition: number;
  playerName: string;
  playerActions: { [key: string]: number[] };
  playerChips: { [key: string]: number };
  currentRaise: number;
  pot: number;
  bigBlindSize: number;
  suggestedAction: string | null;

  constructor(
    hand: string,
    currentRoundNum: number,
    playerPosition: number,
    players: string[],
    playerChipCounts: number[],
    currentRaise: number,
    pot: number,
    bigBlindSize: number
  ) {
    this.hand = hand;
    this.roundNum = currentRoundNum + 1;
    this.playerPosition = playerPosition;
    this.playerName = players[playerPosition];
    this.currentRaise = currentRaise;
    this.pot = pot;
    this.bigBlindSize = bigBlindSize;
    this.suggestedAction = null;

    this.playerActions = {};
    this.playerChips = {};

    if (players.length !== playerChipCounts.length) {
      throw new Error("Players and chips arrays must have the same length");
    }

    players.forEach((player, index) => {
      this.playerActions[player] = [];
      this.playerChips[player] = playerChipCounts[index];
    });
  }
}