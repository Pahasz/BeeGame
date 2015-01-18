package com.mygdx.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.mygdx.game.ColonyGame;
import com.mygdx.game.component.Component;
import com.mygdx.game.component.Interactable;
import com.mygdx.game.entity.Entity;
import com.mygdx.game.helpers.Profiler;
import com.mygdx.game.helpers.gui.GUI;
import com.mygdx.game.helpers.ListHolder;
import com.mygdx.game.helpers.worldgeneration.WorldGen;
import com.mygdx.game.helpers.timer.RepeatingTimer;
import com.mygdx.game.helpers.timer.Timer;
import com.mygdx.game.interfaces.Functional;
import com.mygdx.game.interfaces.IGUI;
import javafx.scene.input.KeyCode;

/**
 * Created by Bbent_000 on 12/25/2014.
 */
public class PlayerInterface extends UI implements IGUI, InputProcessor {
    private SpriteBatch batch;
    private Texture background;
    private World world;
    private boolean drawingInfo = false;
    private boolean drawingProfiler = false;

    private Rectangle buttonRect = new Rectangle();
    private Rectangle infoRect = new Rectangle();
    private float FPS = 0;

    private Interactable interactable = null;

    private Timer FPSTimer;

    private Vector2 testPoint = new Vector2();
    private Entity selected = null;
    private QueryCallback callback = fixture -> {
        if(fixture.testPoint(testPoint.x, testPoint.y)){
            this.selected = (Entity)fixture.getBody().getUserData();
            this.interactable = this.selected.getComponent(Interactable.class);
            System.out.println(this.selected.name+" was selected");
            return false;
        }

        return true;
    };

    /**
     * A player interface Component that will display information on the screen.
     * @param batch The SpriteBatch for drawing to the screen.
     * @param world The Box2D world. We need to know about this for clicking on objects.
     */
    public PlayerInterface(SpriteBatch batch, ColonyGame game, World world) {
        super(batch, game);
        this.batch = batch;
        this.world = world;

        this.background = new Texture("img/background.png");

        this.buttonRect.set(0, Gdx.graphics.getHeight() - 100, 200, 100);
        this.infoRect.set(0,0,Gdx.graphics.getWidth(), 0.1f*Gdx.graphics.getHeight());

        Functional.Callback callback = () -> this.FPS = 1/Gdx.graphics.getDeltaTime();
        this.FPSTimer = new RepeatingTimer(0.5d, callback);

        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void drawGUI(float delta) {
        super.drawGUI(delta);
        int height = Gdx.graphics.getHeight();
        FPSTimer.update(delta);

        GUI.Texture(this.infoRect, this.background, this.batch);

        this.drawInfo(height);

        if(this.drawingProfiler)
            Profiler.drawDebug(ColonyGame.batch, 200, height - 20);

        if(this.selected != null && this.interactable != null){
            this.displaySelected(this.infoRect);
        }

    }

    private void drawInfo(int height){
        if(this.drawingInfo) {
            GUI.Text("FPS: " + FPS, this.batch, 0, height - 20);
            GUI.Text("Zoom: " + ColonyGame.camera.zoom, this.batch, 0, height - 40);
            GUI.Text("Resolution: " + Gdx.graphics.getDesktopDisplayMode().width + "X" + Gdx.graphics.getDesktopDisplayMode().height, this.batch, 0, height - 60);
            GUI.Text("NumTrees: " + WorldGen.numTrees(), this.batch, 0, height - 80);
            GUI.Text("NumTiles: " + WorldGen.numTiles(), this.batch, 0, height - 100);
        }
    }

    private void displaySelected(Rectangle rect){
        float x = rect.getX() + 20;
        float y = rect.getY() + rect.getHeight() - 10;

        GUI.Text("Name: "+this.interactable.getEntityOwner().name, this.batch, x, y);
        y-=20;
        GUI.Text("Type: "+this.interactable.type, this.batch, x, y);
        y-=20;

        if(this.interactable.type == "resource"){
            GUI.Text("ResourceType: "+this.interactable.resource.getResourceType(), this.batch, x, y);
            y-=20;
            GUI.Text("MaxResources: "+this.interactable.resource.getMaxResources(), this.batch, x, y);
            y-=20;
            GUI.Text("CurrResources: "+this.interactable.resource.getCurrResources(), this.batch, x, y);
            y-=20;
        }else if(this.interactable.type == "humanoid"){
            GUI.Text("MaxHealth: "+this.interactable.health.getMaxHealth(), this.batch, x, y);
            y-=20;
            GUI.Text("CurrHealth: "+this.interactable.health.getCurrHealth(), this.batch, x, y);
            y-=20;
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        this.background.dispose();
        this.batch = null;
        this.interactable = null;
        this.world = null;
        this.buttonRect = null;
        this.infoRect = null;
    }

    @Override
    public void resize(int width, int height) {
        this.buttonRect.set(0, Gdx.graphics.getHeight() - 100, 200, 100);
        this.infoRect.set(0,0,Gdx.graphics.getWidth(), 0.1f*Gdx.graphics.getHeight());
    }

    @Override
    public void addToList() {
        ListHolder.addGUI(this);
    }

    @Override
    public boolean keyDown(int keycode) {
        if(keycode == Input.Keys.F1)
            Profiler.enabled = this.drawingInfo = !this.drawingInfo;
        else if(keycode == Input.Keys.F2)
            Profiler.enabled = this.drawingProfiler = !this.drawingProfiler;
        else
            return false;

        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        this.selected = null;
        this.interactable = null;

        Vector3 worldCoords = ColonyGame.camera.unproject(new Vector3(screenX, screenY, 0));


//      System.out.println("There was a click: "+button);
        if(button == Input.Buttons.LEFT){

//            System.out.println("Inside, X/Y: "+screenX+" "+fixedY);
            this.testPoint.set(worldCoords.x, worldCoords.y);
            this.world.QueryAABB(this.callback, worldCoords.x - 1f, worldCoords.y - 1f, worldCoords.x + 1f, worldCoords.y + 1f);
            return true;
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        ColonyGame.camera.zoom += amount*Gdx.graphics.getDeltaTime();
        if(ColonyGame.camera.zoom < 0) ColonyGame.camera.zoom = 0;
        return false;
    }
}