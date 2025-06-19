import java.util.*;

public class GomokuBot {
    private static final int BOARD_SIZE = 15;
    private static final int SEARCH_DEPTH = 6;
    private static final int BOT = 2;
    private static final int HUMAN = 1;
    private static final int EMPTY = 0;
    
    // point values for different patterns
    private static final int INSTANT_WIN = 1000000;
    private static final int FIVE_IN_ROW = 100000;
    private static final int OPEN_FOUR_SCORE = 50000;
    private static final int FOUR_SCORE = 10000;
    private static final int OPEN_THREE_SCORE = 5000;
    private static final int THREE_SCORE = 1000;
    private static final int OPEN_TWO_SCORE = 500;
    private static final int TWO_SCORE = 100;
    private static final int ONE_SCORE = 10;
    
    // threat levels for blocking
    private static final int THREAT_FIVE = 5;
    private static final int THREAT_OPEN_FOUR = 4;
    private static final int THREAT_FOUR = 3;
    private static final int THREAT_OPEN_THREE = 2;
    private static final int THREAT_THREE = 1;
    
    private int[][] gameBoard;
    private Random rng;
    private HashMap<String, Integer> boardCache;
    
    public GomokuBot() {
        this.rng = new Random();
        this.boardCache = new HashMap<>();
    }
    
    // main method to find best move
    public int[] findBestMove(int[][] currentBoard) {
        this.gameBoard = makeBoardCopy(currentBoard);
        
        // clear cache if getting too big
        if (boardCache.size() > 10000) {
            boardCache.clear();
        }
        
        // check if we can win immediately
        int[] winMove = findInstantWin(BOT);
        if (winMove != null) return winMove;
        
        // check if we need to block opponent win
        int[] blockMove = findInstantWin(HUMAN);
        if (blockMove != null) return blockMove;
        
        // look for critical threats to handle
        int[] threatMove = handleCriticalThreats();
        if (threatMove != null) return threatMove;
        
        // use minimax to find best move
        return useMinimaxSearch();
    }
    
    // minimax with iterative deepening
    private int[] useMinimaxSearch() {
        ArrayList<MoveWithScore> possibleMoves = getPossibleMoves();
        if (possibleMoves.isEmpty()) {
            return getBackupMove();
        }
        
        MoveWithScore bestSoFar = null;
        
        // try different depths
        for (int depth = 2; depth <= SEARCH_DEPTH; depth++) {
            int bestValue = Integer.MIN_VALUE;
            MoveWithScore currentBest = null;
            
            // only check top moves to save time
            for (int i = 0; i < Math.min(12, possibleMoves.size()); i++) {
                MoveWithScore move = possibleMoves.get(i);
                int row = move.row;
                int col = move.col;
                
                gameBoard[row][col] = BOT;
                int value = minimax(depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
                gameBoard[row][col] = EMPTY;
                
                if (value > bestValue) {
                    bestValue = value;
                    currentBest = move;
                }
                
                // if we found a winning move, take it
                if (value >= INSTANT_WIN / 2) break;
            }
            
            if (currentBest != null) {
                bestSoFar = currentBest;
            }
        }
        
        return bestSoFar != null ? new int[]{bestSoFar.row, bestSoFar.col} : possibleMoves.get(0).getMove();
    }
    
    // recursive minimax with alpha-beta pruning
    private int minimax(int depth, int alpha, int beta, boolean maximizing) {
        // base case - evaluate position
        if (depth == 0) {
            return evaluateBoard();
        }
        
        // check if we've seen this position before
        String boardKey = getBoardAsString();
        if (boardCache.containsKey(boardKey)) {
            return boardCache.get(boardKey);
        }
        
        ArrayList<MoveWithScore> moves = getPossibleMoves();
        if (moves.isEmpty()) {
            int score = evaluateBoard();
            boardCache.put(boardKey, score);
            return score;
        }
        
        int bestValue;
        
        if (maximizing) {
            bestValue = Integer.MIN_VALUE;
            
            // try top moves only
            for (MoveWithScore move : moves.subList(0, Math.min(10, moves.size()))) {
                gameBoard[move.row][move.col] = BOT;
                
                // check for immediate win
                if (checkWinAt(move.row, move.col, BOT)) {
                    gameBoard[move.row][move.col] = EMPTY;
                    boardCache.put(boardKey, INSTANT_WIN);
                    return INSTANT_WIN;
                }
                
                int value = minimax(depth - 1, alpha, beta, false);
                gameBoard[move.row][move.col] = EMPTY;
                
                bestValue = Math.max(bestValue, value);
                alpha = Math.max(alpha, value);
                
                // alpha-beta cutoff
                if (beta <= alpha) break;
            }
        } else {
            bestValue = Integer.MAX_VALUE;
            
            for (MoveWithScore move : moves.subList(0, Math.min(10, moves.size()))) {
                gameBoard[move.row][move.col] = HUMAN;
                
                // check for immediate loss
                if (checkWinAt(move.row, move.col, HUMAN)) {
                    gameBoard[move.row][move.col] = EMPTY;
                    boardCache.put(boardKey, -INSTANT_WIN);
                    return -INSTANT_WIN;
                }
                
                int value = minimax(depth - 1, alpha, beta, true);
                gameBoard[move.row][move.col] = EMPTY;
                
                bestValue = Math.min(bestValue, value);
                beta = Math.min(beta, value);
                
                if (beta <= alpha) break;
            }
        }
        
        boardCache.put(boardKey, bestValue);
        return bestValue;
    }
    
    // generate moves near existing stones
    private ArrayList<MoveWithScore> getPossibleMoves() {
        HashMap<String, MoveWithScore> candidateSet = new HashMap<>();
        
        // look around existing stones
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (gameBoard[row][col] != EMPTY) {
                    // check 2-step radius around each stone
                    for (int deltaRow = -2; deltaRow <= 2; deltaRow++) {
                        for (int deltaCol = -2; deltaCol <= 2; deltaCol++) {
                            int newRow = row + deltaRow;
                            int newCol = col + deltaCol;
                            
                            if (isValidSpot(newRow, newCol) && gameBoard[newRow][newCol] == EMPTY) {
                                String key = newRow + "," + newCol;
                                if (!candidateSet.containsKey(key)) {
                                    int score = scoreMoveAt(newRow, newCol);
                                    candidateSet.put(key, new MoveWithScore(newRow, newCol, score));
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // if no moves found, play center
        if (candidateSet.isEmpty()) {
            int center = BOARD_SIZE / 2;
            candidateSet.put(center + "," + center, new MoveWithScore(center, center, 100));
        }
        
        ArrayList<MoveWithScore> candidates = new ArrayList<>(candidateSet.values());
        
        // sort by score descending
        candidates.sort((a, b) -> Integer.compare(b.score, a.score));
        
        return candidates;
    }
    
    // score a potential move
    private int scoreMoveAt(int row, int col) {
        int botPoints = scorePositionFor(row, col, BOT);
        int humanPoints = scorePositionFor(row, col, HUMAN);
        
        // give bonus for defensive moves
        int defenseBonus = 0;
        if (humanPoints >= OPEN_THREE_SCORE) {
            defenseBonus = humanPoints / 2;
        }
        
        return botPoints + defenseBonus;
    }
    
    // evaluate entire board position
    private int evaluateBoard() {
        int botTotal = 0;
        int humanTotal = 0;
        
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (gameBoard[row][col] == BOT) {
                    botTotal += scorePositionFor(row, col, BOT);
                } else if (gameBoard[row][col] == HUMAN) {
                    humanTotal += scorePositionFor(row, col, HUMAN);
                }
            }
        }
        
        // add position bonuses
        botTotal += getCenterBonus(BOT);
        humanTotal += getCenterBonus(HUMAN);
        
        return botTotal - humanTotal;
    }
    
    // score a position for a player
    private int scorePositionFor(int row, int col, int player) {
        int totalPoints = 0;
        int[][] directions = {{0,1}, {1,0}, {1,1}, {1,-1}};
        
        for (int[] dir : directions) {
            PatternInfo pattern = analyzeDirection(row, col, dir[0], dir[1], player);
            totalPoints += getScoreForPattern(pattern);
        }
        
        return totalPoints;
    }
    
    // analyze pattern in one direction
    private PatternInfo analyzeDirection(int row, int col, int deltaRow, int deltaCol, int player) {
        int count = 1;
        int openSides = 0;
        int blockedSides = 0;
        
        // check backward direction
        int r = row - deltaRow, c = col - deltaCol;
        boolean leftSideOpen = false;
        while (isValidSpot(r, c) && gameBoard[r][c] == player) {
            count++;
            r -= deltaRow;
            c -= deltaCol;
        }
        if (isValidSpot(r, c) && gameBoard[r][c] == EMPTY) {
            leftSideOpen = true;
            openSides++;
        } else {
            blockedSides++;
        }
        
        // check forward direction
        r = row + deltaRow;
        c = col + deltaCol;
        boolean rightSideOpen = false;
        while (isValidSpot(r, c) && gameBoard[r][c] == player) {
            count++;
            r += deltaRow;
            c += deltaCol;
        }
        if (isValidSpot(r, c) && gameBoard[r][c] == EMPTY) {
            rightSideOpen = true;
            openSides++;
        } else {
            blockedSides++;
        }
        
        return new PatternInfo(count, openSides, blockedSides, leftSideOpen && rightSideOpen);
    }
    
    // convert pattern to score
    private int getScoreForPattern(PatternInfo pattern) {
        int length = pattern.length;
        int openSides = pattern.openSides;
        boolean bothSidesOpen = pattern.bothSidesOpen;
        
        if (length >= 5) return FIVE_IN_ROW;
        
        switch (length) {
            case 4:
                if (openSides >= 1) return OPEN_FOUR_SCORE;
                return FOUR_SCORE;
            case 3:
                if (bothSidesOpen) return OPEN_THREE_SCORE;
                if (openSides >= 1) return THREE_SCORE;
                return 0;
            case 2:
                if (bothSidesOpen) return OPEN_TWO_SCORE;
                if (openSides >= 1) return TWO_SCORE;
                return 0;
            case 1:
                if (openSides >= 1) return ONE_SCORE;
                return 0;
            default:
                return 0;
        }
    }
    
    // bonus for center control
    private int getCenterBonus(int player) {
        int bonus = 0;
        int center = BOARD_SIZE / 2;
        
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (gameBoard[row][col] == player) {
                    int distance = Math.abs(row - center) + Math.abs(col - center);
                    bonus += Math.max(0, 10 - distance);
                }
            }
        }
        
        return bonus;
    }
    
    // find immediate winning move
    private int[] findInstantWin(int player) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (gameBoard[row][col] == EMPTY) {
                    gameBoard[row][col] = player;
                    if (checkWinAt(row, col, player)) {
                        gameBoard[row][col] = EMPTY;
                        return new int[]{row, col};
                    }
                    gameBoard[row][col] = EMPTY;
                }
            }
        }
        return null;
    }
    
    // find critical threats to block
    private int[] handleCriticalThreats() {
        ArrayList<int[]> urgentMoves = new ArrayList<>();
        
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (gameBoard[row][col] == EMPTY) {
                    int threatLevel = getThreatLevelAt(row, col, HUMAN);
                    if (threatLevel >= THREAT_OPEN_FOUR) {
                        urgentMoves.add(new int[]{row, col});
                    }
                }
            }
        }
        
        return urgentMoves.isEmpty() ? null : urgentMoves.get(0);
    }
    
    // calculate threat level of a move
    private int getThreatLevelAt(int row, int col, int player) {
        gameBoard[row][col] = player;
        int maxThreat = 0;
        
        int[][] directions = {{0,1}, {1,0}, {1,1}, {1,-1}};
        for (int[] dir : directions) {
            PatternInfo pattern = analyzeDirection(row, col, dir[0], dir[1], player);
            int threat = 0;
            
            if (pattern.length >= 5) threat = THREAT_FIVE;
            else if (pattern.length == 4 && pattern.openSides >= 1) threat = THREAT_OPEN_FOUR;
            else if (pattern.length == 4) threat = THREAT_FOUR;
            else if (pattern.length == 3 && pattern.bothSidesOpen) threat = THREAT_OPEN_THREE;
            else if (pattern.length == 3 && pattern.openSides >= 1) threat = THREAT_THREE;
            
            maxThreat = Math.max(maxThreat, threat);
        }
        
        gameBoard[row][col] = EMPTY;
        return maxThreat;
    }
    
    // check if move wins the game
    private boolean checkWinAt(int row, int col, int player) {
        int[][] directions = {{0,1},{1,0},{1,1},{1,-1}};
        
        for (int[] dir : directions) {
            int count = 1;
            for (int d = -1; d <= 1; d += 2) {
                int r = row + d * dir[0];
                int c = col + d * dir[1];
                while (isValidSpot(r, c) && gameBoard[r][c] == player) {
                    count++;
                    r += d * dir[0];
                    c += d * dir[1];
                }
            }
            if (count >= 5) return true;
        }
        return false;
    }
    
    // fallback move if no good options
    private int[] getBackupMove() {
        int center = BOARD_SIZE / 2;
        if (gameBoard[center][center] == EMPTY) {
            return new int[]{center, center};
        }
        
        ArrayList<int[]> allMoves = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (gameBoard[i][j] == EMPTY) {
                    allMoves.add(new int[]{i, j});
                }
            }
        }
        
        return allMoves.isEmpty() ? null : allMoves.get(rng.nextInt(allMoves.size()));
    }
    
    // convert board to string for caching
    private String getBoardAsString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                sb.append(gameBoard[i][j]);
            }
        }
        return sb.toString();
    }
    
    // check if position is on board
    private boolean isValidSpot(int row, int col) {
        return row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE;
    }
    
    // make copy of board
    private int[][] makeBoardCopy(int[][] original) {
        int[][] copy = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(original[i], 0, copy[i], 0, BOARD_SIZE);
        }
        return copy;
    }
    
    // helper class for moves with scores
    private static class MoveWithScore {
        int row, col, score;
        
        MoveWithScore(int row, int col, int score) {
            this.row = row;
            this.col = col;
            this.score = score;
        }
        
        int[] getMove() {
            return new int[]{row, col};
        }
    }
    
    // helper class for pattern analysis
    private static class PatternInfo {
        int length, openSides, blockedSides;
        boolean bothSidesOpen;
        
        PatternInfo(int length, int openSides, int blockedSides, boolean bothSidesOpen) {
            this.length = length;
            this.openSides = openSides;
            this.blockedSides = blockedSides;
            this.bothSidesOpen = bothSidesOpen;
        }
    }
}