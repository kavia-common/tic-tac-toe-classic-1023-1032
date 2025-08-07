package org.example.app

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat

// PUBLIC_INTERFACE
/**
 * MainActivity displays the Tic Tac Toe UI, handles player moves, AI logic, score tracking and game/reset controls.
 */
class MainActivity : Activity() {

    // Enums for player type and mode
    enum class Player { X, O }
    enum class GameMode { LOCAL, AI }

    // Board state: 3x3 array with null, X, or O
    private var board = Array(3) { arrayOfNulls<Player?>(3) }
    private var currentPlayer = Player.X
    private var gameActive = true
    private var mode = GameMode.LOCAL
    private var scoreX = 0
    private var scoreO = 0
    private lateinit var gridButtons: Array<Array<Button>>
    private lateinit var statusText: TextView
    private lateinit var scoreText: TextView
    private lateinit var modeToggle: Button
    private lateinit var resetButton: Button

    // Colors from project configuration
    private val colorPrimary: Int by lazy { ContextCompat.getColor(this, R.color.primaryColor) }
    private val colorAccent: Int by lazy { ContextCompat.getColor(this, R.color.accentColor) }
    private val colorSecondary: Int by lazy { ContextCompat.getColor(this, R.color.secondaryColor) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        gridButtons = Array(3) { row ->
            Array(3) { col ->
                val buttonId =
                    resources.getIdentifier("cell_${row}_${col}", "id", packageName)
                findViewById<Button>(buttonId)
            }
        }
        statusText = findViewById(R.id.statusText)
        scoreText = findViewById(R.id.scoreText)
        modeToggle = findViewById(R.id.modeToggle)
        resetButton = findViewById(R.id.resetButton)

        // Grid cell click listeners
        for (row in 0..2) {
            for (col in 0..2) {
                gridButtons[row][col].setOnClickListener { onCellClicked(row, col) }
            }
        }

        // Mode toggle
        modeToggle.setOnClickListener {
            mode = if (mode == GameMode.LOCAL) GameMode.AI else GameMode.LOCAL
            modeToggle.text = getString(if (mode == GameMode.LOCAL) R.string.mode_two_player else R.string.mode_ai)
            resetGame()
        }

        // Reset button
        resetButton.setOnClickListener {
            resetGame()
        }

        resetGame()
    }

    // PUBLIC_INTERFACE
    /**
     * Handles a table cell (grid button) click: updates board, checks winner, triggers AI if needed.
     */
    private fun onCellClicked(row: Int, col: Int) {
        if (!gameActive || board[row][col] != null) return

        // Set move
        board[row][col] = currentPlayer
        updateBoardUI()
        if (checkWin(currentPlayer)) {
            endGame("${playerName(currentPlayer)} ${getString(R.string.wins)}")
            updateScore()
            return
        }
        if (isDraw()) {
            endGame(getString(R.string.draw))
            return
        }

        switchPlayer()

        // AI move if in AI mode and O's turn
        if (mode == GameMode.AI && currentPlayer == Player.O && gameActive) {
            val (aiRow, aiCol) = getAIMove()
            board[aiRow][aiCol] = Player.O
            updateBoardUI()
            if (checkWin(Player.O)) {
                endGame("${playerName(Player.O)} ${getString(R.string.wins)}")
                updateScore()
                return
            }
            if (isDraw()) {
                endGame(getString(R.string.draw))
                return
            }
            switchPlayer()
        }
    }

    // PUBLIC_INTERFACE
    /**
     * Switch current player.
     */
    private fun switchPlayer() {
        currentPlayer = if (currentPlayer == Player.X) Player.O else Player.X
        updateStatus()
    }

    // PUBLIC_INTERFACE
    /**
     * Update board UI according to board state.
     */
    private fun updateBoardUI() {
        for (r in 0..2) {
            for (c in 0..2) {
                val btn = gridButtons[r][c]
                when (board[r][c]) {
                    Player.X -> {
                        btn.text = "X"
                        btn.setTextColor(colorPrimary)
                    }
                    Player.O -> {
                        btn.text = "O"
                        btn.setTextColor(colorAccent)
                    }
                    else -> {
                        btn.text = ""
                        btn.setTextColor(colorSecondary)
                    }
                }
                btn.isEnabled = gameActive && board[r][c] == null
            }
        }
    }

    // PUBLIC_INTERFACE
    /**
     * Reset the game (does NOT reset scores).
     */
    private fun resetGame() {
        board = Array(3) { arrayOfNulls<Player?>(3) }
        currentPlayer = Player.X
        gameActive = true
        updateBoardUI()
        updateStatus()
    }

    // PUBLIC_INTERFACE
    /**
     * End the game: show result, disable board.
     */
    private fun endGame(message: String) {
        gameActive = false
        updateBoardUI()
        statusText.text = message
    }

    // PUBLIC_INTERFACE
    /**
     * Returns true if the board is full and nobody has won.
     */
    private fun isDraw(): Boolean {
        for (row in board) {
            for (cell in row) {
                if (cell == null) return false
            }
        }
        return true
    }

    // PUBLIC_INTERFACE
    /**
     * Checks for a win condition for the given player.
     */
    private fun checkWin(player: Player): Boolean {
        // Check rows, columns, diagonals
        for (i in 0..2) {
            if (board[i].all { it == player }) return true
            if ((0..2).all { board[it][i] == player }) return true
        }
        if ((0..2).all { board[it][it] == player }) return true
        if ((0..2).all { board[it][2 - it] == player }) return true
        return false
    }

    // PUBLIC_INTERFACE
    /**
     * Get a move for the AI (very simple: random available cell).
     */
    private fun getAIMove(): Pair<Int, Int> {
        val available = mutableListOf<Pair<Int, Int>>()
        for (r in 0..2) for (c in 0..2) if (board[r][c] == null) available.add(Pair(r, c))
        // Simple AI: block win, win if possible, else random
        // 1. Win if possible
        for ((r, c) in available) {
            board[r][c] = Player.O
            if (checkWin(Player.O)) {
                board[r][c] = null
                return Pair(r, c)
            }
            board[r][c] = null
        }
        // 2. Block X win
        for ((r, c) in available) {
            board[r][c] = Player.X
            if (checkWin(Player.X)) {
                board[r][c] = null
                return Pair(r, c)
            }
            board[r][c] = null
        }
        // 3. Otherwise random
        if (available.isNotEmpty()) {
            return available.random()
        }
        return Pair(1, 1) // Should not reach
    }

    // PUBLIC_INTERFACE
    /**
     * Update the player scores UI.
     */
    private fun updateScore() {
        if (currentPlayer == Player.X) scoreX++ else scoreO++
        scoreText.text = getString(R.string.score_display, scoreX, scoreO)
    }

    // PUBLIC_INTERFACE
    /**
     * Update the status line to indicate who's turn it is.
     */
    private fun updateStatus() {
        statusText.text = getString(R.string.player_turn, playerName(currentPlayer))
    }

    // PUBLIC_INTERFACE
    /**
     * Utility for player name.
     */
    private fun playerName(player: Player): String {
        return when (player) {
            Player.X -> getString(R.string.player_x)
            Player.O -> if (mode == GameMode.AI) getString(R.string.player_ai) else getString(R.string.player_o)
        }
    }
}
