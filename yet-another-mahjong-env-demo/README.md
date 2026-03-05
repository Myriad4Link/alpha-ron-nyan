# Yet Another Mahjong Env - Demo Module

A command-line interface (CLI) demo application for the 3-player (Sanma) Mahjong game environment.

## Features

- **3-Player Sanma Mahjong (Ari-Ari)**: Play with 2 AI bots using traditional Ari-Ari rules
- **Nuki-Pei**: Declare North wind to draw replacement tile (each nuki = 1 han)
- **Full Hanchan**: Complete East 1-4 + South 1-4 rounds
- **Interactive Gameplay**: Choose actions and discards via console input
- **Smart Bots**: Random decision-making with win priority (always Ron/TsuMo/Nuki when available)
- **Full Information Display**: View all players' scores, discards, nuki counts, and game state

## Running the Demo

### Using Gradle

```bash
./gradlew :yet-another-mahjong-env-demo:run --no-daemon
```

### Using the Distribution

After building:

```bash
./gradlew :yet-another-mahjong-env-demo:installDist
```

Then run:

```bash
./yet-another-mahjong-env-demo/build/install/yet-another-mahjong-env-demo/bin/yet-another-mahjong-env-demo
```

## How to Play

1. **Start the Game**: Run the demo and enter your name when prompted

2. **Your Turn**: When it's your turn, you'll see:
   - Current round information (East/South round number, honba count)
   - All players' scores
   - Your hand (sorted)
   - All discard pools
   - Available actions

3. **Select Actions**: 
   - Actions are numbered (e.g., `[0] Discard`, `[1] Riichi`, `[2] TsuMo`)
   - Enter the number to select an action
   - If discarding, you'll be prompted to select which tile

4. **Bot Turns**: Bots will automatically:
   - Prioritize winning actions (Ron/TsuMo)
   - Randomly select from other available actions
   - Discard random tiles when needed

5. **Round End**: After each round, you'll see:
   - Winner and winning method
   - Points transferred
   - Updated scores

6. **Continue Playing**: Choose whether to continue to the next round or end the match

## Game Rules

### Ari-Ari Sanma (3-Player) Rules

- **No Chii**: You cannot claim tiles for sequences (chii)
- **North Wind Included**: All 4 winds (East, South, West, North) are used
- **Nuki-Pei Allowed**: 
  - Declare North wind tile after drawing to draw a replacement tile
  - North is exposed as an open meld
  - Each nuki counts as 1 han (like dora)
  - Can declare multiple nuki in the same turn
  - Bots always nuki immediately when holding North
- **3-Player Tsumo**: When winning by tsumo, payments are split between 2 other players
- **Starting Scores**: 25,000 points each

### Standard Riichi Mahjong Rules

- **Yaku Required**: Hands must have at least one yaku to win
- **Riichi**: Declare ready hand for 1000-point bet
- **Dora**: Red five tiles count as dora
- **Honba**: 300 points per counter stick on dealer repeats

## Available Actions

- **Discard**: Discard a tile from your hand (when it's your turn)
- **TsuMo**: Win by self-draw (when you have a winning hand)
- **Ron**: Win by claiming another player's discard
- **Riichi**: Declare ready hand (menzen only, 1000-point bet)
- **Pon**: Claim a tile for an open triplet
- **Ankan**: Declare a closed quad
- **Minkan**: Declare an open quad
- **Kakan**: Add a tile to complete a quad
- **Nuki-Pei**: Declare North wind to draw replacement tile (Ari-Ari sanma only)
- **Pass**: Pass on the current opportunity

## Architecture

### Key Components

- **`Main.kt`**: Application entry point
- **`GameLoop.kt`**: Main game loop and match orchestration
- **`RandomBotPlayer.kt`**: Bot AI decision-making logic
- **`ConsoleDisplay.kt`**: Console UI formatting and display
- **`UserInput.kt`**: Keyboard input handling
- **`HumanPlayer.kt`**: Human player implementation
- **`BotPlayer.kt`**: Bot player implementation
- **`DemoMatchListener.kt`**: Match event listener for logging

### Bot AI Logic

Bots use a simple priority system:

1. **Win Priority**: Always choose Ron or TsuMo if available
2. **Random Actions**: Randomly select from other available actions
3. **Random Discards**: Discard random non-winning tiles

## Troubleshooting

### "No line found" Error

This occurs when running in a non-interactive environment. Make sure you're running the demo in a terminal that supports user input.

### Build Errors

Ensure you have Java 25 installed and the project builds successfully:

```bash
./gradlew build
```

### Game Logic Issues

If you encounter unexpected game behavior, check the core module tests:

```bash
./gradlew :yet-another-mahjong-env-core:test
```

## Development

### Modifying the Demo

To change game settings, edit `GameLoop.kt`:

- **Initial Score**: Change `INITIAL_SCORE` constant
- **Round Count**: Modify `EAST_ROUNDS` and `SOUTH_ROUNDS` constants
- **Bot Behavior**: Edit `RandomBotPlayer.kt`

### Adding Features

Possible enhancements:

- Save/load game state
- Bot strategy improvements
- Yaku display during wins
- Detailed scoring breakdown
- Replay functionality

## License

Same license as the main alpha-ron-nyan project.
