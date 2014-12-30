package com.mygdx.game.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.mygdx.game.helpers.GUI;
import com.mygdx.game.helpers.ListHolder;
import com.mygdx.game.helpers.WorldGen;
import com.mygdx.game.interfaces.IGUI;

/**
 * Created by Bbent_000 on 12/29/2014.
 */
public class LoadingInterface extends Component implements IGUI {
    private SpriteBatch batch;
    private float width = 200, height = 20;
    private static boolean done;

    private Rectangle loadingBar = new Rectangle(), square = new Rectangle();
    private Texture outline, bar, blackSquare;

    public LoadingInterface(SpriteBatch batch){
        this.batch = batch;
    }

    @Override
    public void start() {
        super.start();

        this.outline = new Texture("img/LoadingBarOutline.png");
        this.bar = new Texture("img/LoadingBar.png");
        this.blackSquare = new Texture("img/BlackSquare.png");

        this.loadingBar.set(Gdx.graphics.getWidth()/2 - width/2, Gdx.graphics.getHeight()/2 + height/2, width, height);
        this.square.set(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        this.addToList();
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        if(!done) {
            GUI.Texture(this.square, this.blackSquare, this.batch);
            GUI.Texture(this.loadingBar, this.outline, this.batch);
            GUI.Texture(this.loadingBar.x, this.loadingBar.y, this.loadingBar.width*WorldGen.percentageDone, this.loadingBar.height, this.bar, this.batch);
            GUI.Label("Loading Terrain", this.batch, Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2 + 50, true);

        }else
            this.owner.destroy();
    }

    public static void setDone(){
        done = true;
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void addToList() {
        ListHolder.addInterface(this);
    }
}
