package com.juankysoriano.materiallife.world.life;

import android.view.MotionEvent;

import com.juankysoriano.materiallife.ContextRetriever;
import com.juankysoriano.materiallife.R;
import com.juankysoriano.rainbow.core.drawing.RainbowDrawer;
import com.juankysoriano.rainbow.core.event.RainbowInputController;
import com.juankysoriano.rainbow.core.graphics.RainbowImage;
import com.openca.bi.OnCellUpdatedCallback2D;
import com.openca.bi.discrete.AutomataDiscrete2D;

public class GameOfLife implements RainbowInputController.RainbowInteractionListener, OnCellUpdatedCallback2D, RainbowDrawer.PointDetectedListener {
    public static final int ALIVE = 1;
    public static final int DEAD = 0;
    private static final int CELL_SIZE = ContextRetriever.INSTANCE.getResources().getInteger(R.integer.cell_size);
    private final AutomataDiscrete2D gameOfLife;
    private final RainbowInputController rainbowInputController;
    private final GameOfLifeDrawer gameOfLifeDrawer;
    private boolean editing;
    private int[][] cellsBackup;

    public static GameOfLife newInstance(RainbowDrawer rainbowDrawer,
                                         RainbowInputController rainbowInputController) {
        int width = rainbowDrawer.getWidth() / CELL_SIZE;
        int height = rainbowDrawer.getHeight() / CELL_SIZE;
        GameOfLifeCreator gameOfLifeCreator = GameOfLifeCreator.newInstance();
        AutomataDiscrete2D automata = gameOfLifeCreator.createGameOfLife(width, height);
        GameOfLifeDrawer gameOfLifeDrawer = GameOfLifeDrawer.newInstance(rainbowDrawer);
        GameOfLife gameOfLife = new GameOfLife(automata, gameOfLifeDrawer, rainbowInputController);
        rainbowInputController.setRainbowInteractionListener(gameOfLife);

        return gameOfLife;
    }

    protected GameOfLife(AutomataDiscrete2D gameOfLife, GameOfLifeDrawer gameOfLifeDrawer, RainbowInputController rainbowInputController) {
        this.gameOfLife = gameOfLife;
        this.gameOfLifeDrawer = gameOfLifeDrawer;
        this.rainbowInputController = rainbowInputController;
    }

    public void doStep() {
        gameOfLifeDrawer.paintBackground();
        if (editing) {
            paintCellsWithoutEvolution();
        } else {
            paintCellsAndEvolve();
        }
    }

    private void paintCellsWithoutEvolution() {
        for (int i = 0; i < gameOfLife.getWidth(); i++) {
            for (int j = 0; j < gameOfLife.getHeight(); j++) {
                onCellDetected(i, j, gameOfLife.getCells()[i][j]);
            }
        }
    }

    private void paintCellsAndEvolve() {
        gameOfLife.evolve(this);
    }

    @Override
    public void onCellDetected(int x, int y, int state) {
        if (isCellAlive(state)) {
            gameOfLifeDrawer.paintCellAt(x, y);
        }
    }

    private boolean isCellAlive(int state) {
        return state == ALIVE;
    }

    @Override
    public void onSketchTouched(MotionEvent motionEvent, RainbowDrawer rainbowDrawer) {
        onPointDetected(rainbowInputController.getPreviousX(),
                rainbowInputController.getPreviousY(),
                rainbowInputController.getX(),
                rainbowInputController.getY(), rainbowDrawer);
    }

    @Override
    public void onSketchReleased(MotionEvent motionEvent, RainbowDrawer rainbowDrawer) {
        //no-op
    }

    @Override
    public void onFingerDragged(MotionEvent motionEvent, RainbowDrawer rainbowDrawer) {
        int x = (int) rainbowInputController.getX();
        int y = (int) rainbowInputController.getY();
        int previousX = (int) rainbowInputController.getPreviousX();
        int previousY = (int) rainbowInputController.getPreviousY();

        rainbowDrawer.exploreLine(previousX, previousY, x, y, RainbowDrawer.Precision.HIGH, this);
    }

    @Override
    public void onPointDetected(float px, float py, float x, float y, RainbowDrawer rainbowDrawer) {
        int cellX = (int) (x / CELL_SIZE);
        int cellY = (int) (y / CELL_SIZE);

        if (cellX >= 0 && cellX < gameOfLife.getWidth()
                && cellY >= 0 && cellY < gameOfLife.getHeight()) {
            gameOfLife.getCells()[cellX][cellY] = ALIVE;
        }
    }

    @Override
    public void onMotionEvent(MotionEvent motionEvent, RainbowDrawer rainbowDrawer) {
        //no-op
    }

    public void startEdition() {
        if (!editing) {
            editing = true;
            doCellsBackup();
        }
    }

    private void doCellsBackup() {
        cellsBackup = new int[gameOfLife.getWidth()][];
        for (int i = 0; i < gameOfLife.getWidth(); i++) {
            int[] row = gameOfLife.getCells()[i];
            cellsBackup[i] = new int[row.length];
            System.arraycopy(row, 0, cellsBackup[i], 0, row.length);
        }
    }

    public void endEdition() {
        editing = false;
    }

    public void clear() {
        gameOfLifeDrawer.clearBackground();

        for (int i = 0; i < gameOfLife.getWidth(); i++) {
            for (int j = 0; j < gameOfLife.getHeight(); j++) {
                gameOfLife.getCells()[i][j] = DEAD;
            }
        }
    }

    public void restoreLastWorld() {
        for (int i = 0; i < gameOfLife.getWidth(); i++) {
            System.arraycopy(cellsBackup[i], 0, gameOfLife.getCells()[i], 0, gameOfLife.getHeight());
        }
    }

    public void loadWorldFrom(RainbowImage image) {
        for (int i = 0; i < gameOfLife.getWidth(); i++) {
            for (int j = 0; j < gameOfLife.getHeight(); j++) {
                gameOfLife.getCells()[i][j] = gameOfLifeDrawer.getCellStateFrom(image, i, j);
            }
        }
    }
}
