# Gomoku Game (with Bot!)

👋 Welcome to my Gomoku Game project. It's a fun desktop game where you can play Gomoku (aka Five in a Row) either against a friend or challenge a computer bot. The game has a cool GUI, some sound effects, and a timer to keep things fast-pace!

---

## How to Run and Play

1. **Requirements:**  
   - You need Java installed (Java 8 or higher should work).
   - Make sure you have all the files in the same folder, including the `assets` folder with all the images and sounds.

2. **Starting the Game:**  
   - Open a terminal or command prompt in the project folder.
   - Compile the code:
     ```
     javac GomokuGame.java GomokuBot.java
     ```
   - Run the game:
     ```
     java GomokuGame
     ```
   - The game window should pop up!

3. **How to Play:**  
   - When the game starts, you’ll see two big buttons:
     - **Local 1v1:** Play with a friend on the same computer (take turns clicking).
     - **vs Computer:** Play against the bot (you go first as black, bot is white).
   - Click on the board to place your stone.  
   - Each player has 60 seconds per turn, so don’t take too long!
   - The first to get five in a row (horizontally, vertically, or diagonally) wins.
   - If you want to return to the main menu, hit the "Back" button at the top left.

---

## Extra Notes

- If you get any errors about missing images or sounds, make sure the `assets` folder is in the same place as your `.java` files and nothing is missing from it.
- The game doesn’t auto-quit after a win, so you can admire your victory (or defeat) as long as you want.
- If you want to play again, just hit "Back" and start a new game.

---

Have fun playing! 😆