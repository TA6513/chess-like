# Chess Like Game

A two-player strategy game built with Java and JavaFX.

Players move pieces across an 11×11 board, capture opposing pieces, and permanently claim territory.

## Game Modes

- **Offline** — Two players on the same computer
- **LAN** — Host or join a game on the same network
- **Online** — Create or join a room through the dedicated game server

## How to Play

Player 1 and Player 2 begin on opposite sides of the board along with three neutral pieces.

Each turn:

- Move pieces to adjacent cells.
- A piece can only move once per turn.
- Player 1 receives one move on the opening turn.
- Turns normally allow two moves.
- The second move can be passed.

Moving onto an unclaimed cell permanently claims it for your side.

### Capturing

- Moving onto an enemy piece removes it from the board.
- Capturing a neutral piece converts it to your side.
- A captured neutral piece cannot move again during the same turn.
- Neutral pieces can later be captured by the other player.

## Winning

A player wins by either:

- Eliminating all opposing pieces, or
- Claiming a majority of the board.

## Running the Game

### Windows Release

Download the latest Windows release, extract the ZIP, and run:

`Chess Like Game.exe`

The packaged release includes the required Java runtime.

### Development

Requires:

- JDK 26
- Maven
- JavaFX 26

Run from source with:

```powershell
mvn javafx:run

## License

Chess Like Game is source-available for personal use and modification.

You may play, study, and modify the game for your own personal use.
Redistribution of the original or modified game is not permitted without
permission from the copyright holder.

See [LICENSE](LICENSE) for details.
