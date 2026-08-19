# Chess-Like Game

A JavaFX strategy board game featuring territory control, piece capture, and
multiple actions per turn.

## Overview

Chess-Like Game is a two-player strategy game played on an 11x11 board.

Players move pieces around the board, capture enemy pieces, capture neutral
pieces, and claim territory.

The game has two possible victory conditions:

1. Eliminate all of the opposing player's pieces.
2. Claim a majority of the board's cells.

A majority on an 11x11 board requires 61 of the 121 cells.

## Features

- 11x11 game board
- Two players
- Player-specific piece sprites
- Three neutral pieces
- Neutral pieces change allegiance when captured
- Captured neutral pieces remain on the board
- Captured enemy pieces are removed
- Permanent territory claiming
- Two actions per turn
- Player 1 has one action on the opening turn
- Individual pieces can only move once per turn
- Players can skip a move on a turn
- Captured neutral pieces cannot move during the turn they are captured
- Territory-based victory
- Elimination-based victory
- Resizable game window
- Reset Game button
- Turn indicator
- Territory counters

## Game Rules

### Starting Positions

Player 1 starts in the southwest corner.

Player 2 starts in the northeast corner.

Three neutral pieces begin at:

- Northwest corner
- Center of the board
- Southeast corner

### Turns

Player 1 begins the game with one action.

After the opening turn, each player receives two actions per turn.

Each piece can only be used once during a turn.

A player may pass an action instead of moving a piece.

### Neutral Pieces

Neutral pieces behave differently from enemy pieces.

When a player captures a neutral piece:

- The neutral piece remains on its current cell.
- The neutral piece changes allegiance to the capturing player.
- The neutral piece cannot move during the turn in which it was captured.
- The cell occupied by the neutral piece becomes claimed by the capturing player.

The neutral piece can then move normally on future turns.

If the opposing player captures that formerly-neutral piece, it changes allegiance again.

### Enemy Pieces

When a player captures an enemy piece:

- The enemy piece is removed from the board.
- The capturing piece moves onto the enemy's cell.
- The cell's existing territory ownership does not change.

### Territory

Moving a piece onto an unclaimed cell permanently claims that cell for
the player's side.

Once a cell has been claimed, its ownership cannot change.

Starting cells are automatically claimed by the player that begins there.

### Victory

A player wins by either:

#### Elimination

All pieces belonging to the opposing player have been captured.

#### Territory Claimed

The player claims a majority of the board.

The 11x11 board contains 121 cells, so a player needs:

**61 claimed cells**

to win by territory.

## Controls

- **Click a piece** to select it.
- **Click a highlighted cell** to move or capture.
- **Pass Move** gives up the current action.
- **Reset Game** starts a new game.

## Requirements

- Java 26
- Maven
- JavaFX 26

## Running the Game

### Using Maven

From the project directory:

```powershell
mvn javafx:run
