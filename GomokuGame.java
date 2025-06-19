import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

public class GomokuGame {
    // board setup stuff
    private static final int ROWS = 15;
    private static final int COLS = 15;
    private static final int CELL_SIZE = 40;
    private static final int OFFSET_X = 100;
    private static final int OFFSET_Y = 50;

    // gui components
    private JFrame frame;
    private CardLayout screenManager;
    private JPanel mainPanel;
    private GraphicsPanel gamePanel;

    // game state tracking
    private int[][] board = new int[ROWS][COLS];
    private boolean blackTurn = true;
    private boolean gameOver = false;
    private boolean vsBot = false;

    // visual and audio assets
    private Image blackStone;
    private Image whiteStone;
    private Clip blackSound, whiteSound, winSound;

    // timer related stuff
    private static final int TURN_TIME_SECONDS = 60;
    private Timer turnTimer;
    private int timeLeft;
    private JLabel timerLabel;
    private JLabel winLabel;

    // ai opponent
    private GomokuBot bot;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(() -> {
            try {
                new GomokuGame().start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void start() throws Exception {
        loadResources();
        setupUI();
        bot = new GomokuBot();
    }

    // load images and sounds
    private void loadResources() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        blackStone = ImageIO.read(loader.getResource("assets/black.png"));
        whiteStone = ImageIO.read(loader.getResource("assets/white.png"));
        blackSound = loadSound(loader.getResource("assets/black.wav"));
        whiteSound = loadSound(loader.getResource("assets/white.wav"));
        winSound = loadSound(loader.getResource("assets/win.wav"));
    }

    // helper for loading audio clips
    private Clip loadSound(java.net.URL resource) throws Exception {
        if (resource == null) return null;
        AudioInputStream stream = AudioSystem.getAudioInputStream(resource);
        Clip clip = AudioSystem.getClip();
        clip.open(stream);
        return clip;
    }

    // build the entire interface
    private void setupUI() {
        frame = new JFrame("Gomoku Game");
        frame.setSize(800, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        screenManager = new CardLayout();
        mainPanel = new JPanel(screenManager);

        // start screen with buttons
        JPanel startPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 40, 40));

        JButton localButton = new JButton("Local 1v1");
        localButton.setFont(new Font("Arial", Font.BOLD, 36));
        localButton.addActionListener(e -> {
            vsBot = false;
            screenManager.show(mainPanel, "game");
            startTurnTimer();
        });

        JButton botButton = new JButton("vs Computer");
        botButton.setFont(new Font("Arial", Font.BOLD, 36));
        botButton.addActionListener(e -> {
            vsBot = true;
            screenManager.show(mainPanel, "game");
            startTurnTimer();
        });

        buttonPanel.add(localButton);
        buttonPanel.add(botButton);
        startPanel.add(buttonPanel, BorderLayout.CENTER);

        // labels for timer and winner
        timerLabel = new JLabel("Time left: 60", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        timerLabel.setForeground(Color.BLUE);

        winLabel = new JLabel("", SwingConstants.CENTER);
        winLabel.setFont(new Font("Arial", Font.BOLD, 28));
        winLabel.setForeground(Color.RED);

        // game screen layout
        JPanel gameContainer = new JPanel(new BorderLayout());
        gamePanel = new GraphicsPanel();
        gamePanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                handleMouseClick(e.getX(), e.getY());
            }
        });
        gameContainer.add(timerLabel, BorderLayout.NORTH);
        gameContainer.add(gamePanel, BorderLayout.CENTER);
        gameContainer.add(winLabel, BorderLayout.SOUTH);

        mainPanel.add(startPanel, "start");
        mainPanel.add(gameContainer, "game");

        frame.setContentPane(mainPanel);
        screenManager.show(mainPanel, "start");
        frame.setVisible(true);
    }

    // handle player clicking board
    private void handleMouseClick(int x, int y) {
        if (gameOver) return;
        if (vsBot && !blackTurn) return;

        // convert pixel coords to grid
        int row = (y - OFFSET_Y + CELL_SIZE / 2) / CELL_SIZE;
        int col = (x - OFFSET_X + CELL_SIZE / 2) / CELL_SIZE;

        if (row >= 0 && row < ROWS && col >= 0 && col < COLS && board[row][col] == 0) {
            board[row][col] = blackTurn ? 1 : 2;
            playMoveSound();
            gamePanel.repaint();

            if (checkWin(row, col)) {
                gameOver = true;
                stopTurnTimer();
                String winner = blackTurn ? (vsBot ? "You" : "Black") : (vsBot ? "Computer" : "White");
                showWinAndExit(winner + " wins!");
            } else {
                blackTurn = !blackTurn;
                if (vsBot && !blackTurn && !gameOver) {
                    startTurnTimer();
                    botMove();
                } else {
                    startTurnTimer();
                }
            }
        }
    }

    // computer makes its move
    private void botMove() {
        if (gameOver) return;
        
        String originalText = winLabel.getText();
        winLabel.setText("Computer is computing...");
        gamePanel.repaint();
        
        // add small delay for drama
        Timer botDelay = new Timer(800, evt -> {
            int[] move = bot.findBestMove(board);
            board[move[0]][move[1]] = 2;
            playMoveSound();
            winLabel.setText(originalText);
            gamePanel.repaint();
            
            if (checkWin(move[0], move[1])) {
                gameOver = true;
                stopTurnTimer();
                showWinAndExit("Computer wins!");
            } else {
                blackTurn = !blackTurn;
                startTurnTimer();
            }
        });
        botDelay.setRepeats(false);
        botDelay.start();
    }

    // begin countdown for turn
    private void startTurnTimer() {
        stopTurnTimer();
        timeLeft = TURN_TIME_SECONDS;
        updateTimerLabel();
        turnTimer = new Timer(1000, e -> {
            timeLeft--;
            updateTimerLabel();
            if (timeLeft <= 0) {
                stopTurnTimer();
                gameOver = true;
                String loser = blackTurn ? (vsBot ? "You" : "Black") : (vsBot ? "Computer" : "White");
                String winner = blackTurn ? (vsBot ? "Computer" : "White") : (vsBot ? "You" : "Black");
                showWinAndExit(loser + " ran out of time! " + winner + " wins!");
            }
        });
        turnTimer.start();
    }

    // stop the countdown
    private void stopTurnTimer() {
        if (turnTimer != null) {
            turnTimer.stop();
            turnTimer = null;
        }
    }

    // refresh timer display text
    private void updateTimerLabel() {
        String player = blackTurn ? (vsBot ? "Your" : "Black") : (vsBot ? "Computer's" : "White");
        timerLabel.setText(player + " turn - Time left: " + timeLeft + "s");
    }

    // play stone placement sound
    private void playMoveSound() {
        Clip sound = blackTurn ? blackSound : whiteSound;
        if (sound != null) {
            sound.setFramePosition(0);
            sound.start();
        }
    }

    // display winner and quit
    private void showWinAndExit(String message) {
        playWinSound();
        winLabel.setText(message);
        gamePanel.repaint();
        int delay = winSound != null ? (int) (winSound.getMicrosecondLength() / 1000) : 2000;
        new Timer(delay, evt -> System.exit(0)).start();
    }

    // play victory sound effect
    private void playWinSound() {
        if (winSound != null) {
            winSound.setFramePosition(0);
            winSound.start();
        }
    }

    // check if last move won
    private boolean checkWin(int row, int col) {
        int player = board[row][col];
        int[][] directions = {{0,1},{1,0},{1,1},{1,-1}};

        for (int[] dir : directions) {
            int count = 1;
            for (int d = -1; d <= 1; d += 2) {
                int r = row + d * dir[0];
                int c = col + d * dir[1];
                while (r >= 0 && r < ROWS && c >= 0 && c < COLS && board[r][c] == player) {
                    count++;
                    r += d * dir[0];
                    c += d * dir[1];
                }
            }
            if (count >= 5) return true;
        }
        return false;
    }

    // custom panel for drawing board
    class GraphicsPanel extends JPanel {
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // tan board background
            g.setColor(new Color(210, 180, 140));
            g.fillRect(0, 0, getWidth(), getHeight());

            // draw grid lines
            g.setColor(Color.BLACK);
            for (int i = 0; i < ROWS; i++)
                g.drawLine(OFFSET_X, OFFSET_Y + i * CELL_SIZE, OFFSET_X + (COLS - 1) * CELL_SIZE, OFFSET_Y + i * CELL_SIZE);
            for (int j = 0; j < COLS; j++)
                g.drawLine(OFFSET_X + j * CELL_SIZE, OFFSET_Y, OFFSET_X + j * CELL_SIZE, OFFSET_Y + (ROWS - 1) * CELL_SIZE);

            // draw all placed stones
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    int x = OFFSET_X + col * CELL_SIZE - CELL_SIZE / 2 + 1;
                    int y = OFFSET_Y + row * CELL_SIZE - CELL_SIZE / 2 + 1;
                    if (board[row][col] == 1 && blackStone != null)
                        g.drawImage(blackStone, x, y, CELL_SIZE - 2, CELL_SIZE - 2, this);
                    else if (board[row][col] == 2 && whiteStone != null)
                        g.drawImage(whiteStone, x, y, CELL_SIZE - 2, CELL_SIZE - 2, this);
                }
            }
        }
    }
}