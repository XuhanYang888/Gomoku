import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

public class GomokuGame {
    private static final int ROWS = 15;
    private static final int COLS = 15;
    private static final int CELL_SIZE = 40;
    private static final int OFFSET_X = 100;
    private static final int OFFSET_Y = 50;

    private JFrame frame;
    private CardLayout screenManager;
    private JPanel mainPanel;
    private GraphicsPanel gamePanel;

    private int[][] board = new int[ROWS][COLS];
    private boolean blackTurn = true;
    private boolean gameOver = false;

    private Image blackStone;
    private Image whiteStone;
    private Clip blackSound, whiteSound, winSound;

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
    }

    private void loadResources() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        blackStone = ImageIO.read(loader.getResource("black.png"));
        whiteStone = ImageIO.read(loader.getResource("white.png"));

        blackSound = loadSound(loader.getResource("black.wav"));
        whiteSound = loadSound(loader.getResource("white.wav"));
        winSound = loadSound(loader.getResource("win.wav"));
    }

    private Clip loadSound(java.net.URL resource) throws Exception {
        if (resource == null) return null;
        AudioInputStream stream = AudioSystem.getAudioInputStream(resource);
        Clip clip = AudioSystem.getClip();
        clip.open(stream);
        return clip;
    }

    private void setupUI() {
        frame = new JFrame("Gomoku Game");
        frame.setSize(800, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        screenManager = new CardLayout();
        mainPanel = new JPanel(screenManager);

        JPanel startPanel = new JPanel(new BorderLayout());
        JButton startButton = new JButton("Start Game");
        startButton.setFont(new Font("Arial", Font.BOLD, 36));
        startButton.addActionListener(e -> screenManager.show(mainPanel, "game"));
        startPanel.add(startButton, BorderLayout.CENTER);

        gamePanel = new GraphicsPanel();
        gamePanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                handleMouseClick(e.getX(), e.getY());
            }
        });

        mainPanel.add(startPanel, "start");
        mainPanel.add(gamePanel, "game");

        frame.setContentPane(mainPanel);
        screenManager.show(mainPanel, "start");
        frame.setVisible(true);
    }

    private void handleMouseClick(int x, int y) {
        if (gameOver) return;

        int row = (y - OFFSET_Y + CELL_SIZE / 2) / CELL_SIZE;
        int col = (x - OFFSET_X + CELL_SIZE / 2) / CELL_SIZE;

        if (row >= 0 && row < ROWS && col >= 0 && col < COLS && board[row][col] == 0) {
            board[row][col] = blackTurn ? 1 : 2;
            playMoveSound();

            if (checkWin(row, col)) {
                gameOver = true;
                playWinSoundAndExit();
            }
            blackTurn = !blackTurn;
            gamePanel.repaint();
        }
    }

    private void playMoveSound() {
        Clip sound = blackTurn ? blackSound : whiteSound;
        if (sound != null) {
            sound.setFramePosition(0);
            sound.start();
        }
    }

    private void playWinSoundAndExit() {
        if (winSound != null) {
            winSound.setFramePosition(0);
            winSound.start();
            int delay = (int) (winSound.getMicrosecondLength() / 1000);
            new Timer(delay, evt -> System.exit(0)).start();
        }
    }

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

    // Custom JPanel for rendering the game board and pieces
    class GraphicsPanel extends JPanel {
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            g.setColor(new Color(210, 180, 140));
            g.fillRect(0, 0, getWidth(), getHeight());

            g.setColor(Color.BLACK);
            for (int i = 0; i < ROWS; i++)
                g.drawLine(OFFSET_X, OFFSET_Y + i * CELL_SIZE, OFFSET_X + (COLS - 1) * CELL_SIZE, OFFSET_Y + i * CELL_SIZE);
            for (int j = 0; j < COLS; j++)
                g.drawLine(OFFSET_X + j * CELL_SIZE, OFFSET_Y, OFFSET_X + j * CELL_SIZE, OFFSET_Y + (ROWS - 1) * CELL_SIZE);

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

            if (gameOver) {
                g.setColor(Color.RED);
                g.setFont(new Font("Arial", Font.BOLD, 30));
                g.drawString((blackTurn ? "White" : "Black") + " Wins!", 350, 30);
            }
        }
    }
}
