package com.mygdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.math.MathUtils;
import com.mygdx.game.ColonyGame;
import com.mygdx.game.helpers.Constants;
import com.mygdx.game.helpers.ListHolder;
import com.mygdx.game.helpers.worldgeneration.WorldGen;
import com.mygdx.game.ui.LoadingInterface;
import com.mygdx.game.ui.UI;

import java.util.ArrayList;

/**
 * Created by Bbent_000 on 12/29/2014.
 * The Screen responsible for loading the game and displaying the progress of loading.
 */
public class LoadingScreen implements Screen {
    private ColonyGame game;
    private LoadingInterface loadingInterface;

    public LoadingScreen(final ColonyGame game){
        this.game = game;
    }

    @Override
    public void show() {
        WorldGen.getInstance().tileSize = Constants.GRID_SQUARESIZE;
        WorldGen.getInstance().init((long)(MathUtils.random()*Long.MAX_VALUE), game);
        loadingInterface = new LoadingInterface(ColonyGame.batch, this.game);
    }

    @Override
    public void render(float delta) {
        if(WorldGen.getInstance().generateWorld()) {
            loadingInterface.setDone();
            WorldGen.getInstance().clean();
            this.dispose();
            game.setScreen(new GameScreen(this.game));
        }
    }

    @Override
    public void resize(int width, int height) {
        Gdx.graphics.setDisplayMode(width, height, false);
        ColonyGame.camera.setToOrtho(false, width, height);
        ColonyGame.UICamera.setToOrtho(false, width, height);

        //Resizes all the GUI elements of the game (hopefully!)
        ArrayList<UI> list = ListHolder.getGUIList();
        for(int i=0;i< list.size();i++){
            list.get(i).resize(width, height);
        }
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }
}
