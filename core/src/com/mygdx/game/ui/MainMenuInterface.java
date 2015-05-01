package com.mygdx.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mygdx.game.ColonyGame;
import com.mygdx.game.helpers.DataBuilder;
import com.mygdx.game.helpers.ListHolder;
import com.mygdx.game.helpers.gui.GUI;
import com.mygdx.game.helpers.worldgeneration.WorldGen;
import com.mygdx.game.screens.LoadingScreen;

/**
 * A UI that controls the main menu. This is responsible for all buttons, images, music, and sounds of the main menu.
 */
public class MainMenuInterface extends UI{
    public static Texture mainMenuTexture = ColonyGame.assetManager.get("Space2", Texture.class);
    public static Music music = Gdx.audio.newMusic(Gdx.files.internal("music/Karkarakacrrot.ogg"));

    GUI.GUIStyle startButtonStyle = new GUI.GUIStyle();
    GUI.GUIStyle quitButtonStyle = new GUI.GUIStyle();
    GUI.GUIStyle blank1Style = new GUI.GUIStyle();
    GUI.GUIStyle blank2Style = new GUI.GUIStyle();
    GUI.GUIStyle blank3Style = new GUI.GUIStyle();

    Rectangle[] buttonRects = new Rectangle[5];

    private Texture titleTexture;
    private BitmapFont titleFont = new BitmapFont(Gdx.files.internal("fonts/titlefont.fnt"));
    private GUI.GUIStyle changeLogStyle = new GUI.GUIStyle();
    private Rectangle startRect = new Rectangle(), quitRect = new Rectangle(), blank1Rect = new Rectangle();
    private Rectangle blank2Rect = new Rectangle(), blank3Rect = new Rectangle(), changelogRect = new Rectangle(), titleRect = new Rectangle();
    private Container<ScrollPane> outsideScrollContainer;

    private Stage stage;

    public MainMenuInterface(SpriteBatch batch, ColonyGame game) {
        super(batch, game);

        titleTexture = ColonyGame.assetManager.get("Auroris", Texture.class);

        //Set the states for the start button
        startButtonStyle.normal = ColonyGame.assetManager.get("startbutton_normal", Texture.class);
        startButtonStyle.moused = ColonyGame.assetManager.get("startbutton_moused", Texture.class);
        startButtonStyle.clicked = ColonyGame.assetManager.get("startbutton_clicked", Texture.class);

        //States for quit button.
        quitButtonStyle.normal = ColonyGame.assetManager.get("quitbutton_normal", Texture.class);
        quitButtonStyle.moused = ColonyGame.assetManager.get("quitbutton_moused", Texture.class);
        quitButtonStyle.clicked = ColonyGame.assetManager.get("quitbutton_clicked", Texture.class);

        //For blank buttons
        blank1Style.normal = blank1Style.moused = blank1Style.clicked = blank2Style.normal = blank2Style.moused = ColonyGame.assetManager.get("blankbutton_normal", Texture.class);
        blank3Style.normal = blank3Style.moused = blank3Style.clicked = ColonyGame.assetManager.get("blankbutton_normal", Texture.class);

        //Assign all these to an array for easy displaying and resizing.
        buttonRects[0] = startRect;
        buttonRects[1] = blank1Rect;
        buttonRects[2] = blank2Rect;
        buttonRects[3] = blank3Rect;
        buttonRects[4] = quitRect;

        //Plays the main menu music.
        music.play();
        music.setLooping(true);

        //Sets up the font for the changelog area. Fancy!
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Roboto-Bold.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 16;
        parameter.borderWidth = 1f;
        changeLogStyle.font = generator.generateFont(parameter);
        generator.dispose();

        //Create a new stage and configure it!
        stage = new Stage(new ScreenViewport(ColonyGame.UICamera), this.batch);
        //stage.setDebugAll(true);
        Gdx.input.setInputProcessor(stage);

        this.makeVersionHistoryScrollbar();
    }

    private void makeVersionHistoryScrollbar(){
        Label.LabelStyle historyStyle = new Label.LabelStyle();
        historyStyle.font = changeLogStyle.font;

        //Create the table for this inside text.
        Table insideScrollTextTable = new Table();

        //The scrollpane that holds the container.
        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        scrollStyle.vScrollKnob = new TextureRegionDrawable(new TextureRegion(WorldGen.whiteTex));
        ScrollPane versionHistoryScroll = new ScrollPane(insideScrollTextTable, scrollStyle);

        //Create the outside table that will hold the scrollpane
        this.outsideScrollContainer = new Container<>(versionHistoryScroll).fill();

        StringBuilder str;
        for(int i = 0;i<DataBuilder.changelog.changes.length;i++){
            DataBuilder.JsonLog log = DataBuilder.changelog.changes[i];

            Label title = new Label("Version: " + log.version+"\nDate: "+log.date, historyStyle);
            title.setAlignment(Align.center);

            Label text = new Label("", historyStyle);
            text.setWrap(true);

            str = new StringBuilder();
            for(String txt : log.log)
                str.append(txt).append("\n");

            text.setText(str.toString());

            insideScrollTextTable.add(title).fillX().expandX();
            insideScrollTextTable.row();
            insideScrollTextTable.add(text).fillX().expandX();
            insideScrollTextTable.row().padTop(20f);
        }

        stage.addActor(outsideScrollContainer);

//        Table table = new Table();
//        table.add(versionHistoryScroll).width(500).height(Gdx.graphics.getHeight() * 0.8f);
//        table.setBounds(0, Gdx.graphics.getHeight() - Gdx.graphics.getHeight() * 0.8f, 500, Gdx.graphics.getHeight() * 0.8f);
    }

    @Override
    public void render(float delta, SpriteBatch batch) {
        super.render(delta, batch);

        this.batch.draw(mainMenuTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        //Set a new font, draw "Colony Game" to the screen, and reset the font.
        GUI.font = titleFont;
        GUI.Texture(titleTexture, titleRect, this.batch);
        GUI.ResetFont();

        //Start button.
        if(GUI.Button(startRect, "", this.batch, startButtonStyle)){
            this.destroy();
            this.game.setScreen(new LoadingScreen(this.game));
            return;
        }

        GUI.Button(blank1Rect, "", this.batch, blank1Style);
        GUI.Button(blank2Rect, "", this.batch, blank2Style);
        GUI.Button(blank3Rect, "", this.batch, blank3Style);

        //Quit button.
        if(GUI.Button(quitRect, "", this.batch, quitButtonStyle)){
            Gdx.app.exit();
        }

        this.batch.end();

        stage.act(delta);
        stage.draw();

        this.batch.begin();
    }

    @Override
    public void destroy() {
        super.destroy();

        music.stop();
        music.dispose();
        mainMenuTexture.dispose();
        titleFont.dispose();
        titleFont = null;
        startRect = null;
        quitRect = null;

        stage.dispose();
    }

    @Override
    public void resize(int width, int height) {
        this.changelogRect.set(width - width * 0.3f, 0, width * 0.3f, height - height * 0.1f);
        this.titleRect.set(width / 2 - titleTexture.getWidth() / 2, height - titleTexture.getHeight() - height * 0.02f, titleTexture.getWidth(), titleTexture.getHeight());

        this.outsideScrollContainer.setBounds(width * 0.66f, height * 0.1f, width * 0.3f, height * 0.8f);
        //this.outsideScrollContainer.getActor().setWidth(width * 0.33f);
        //this.outsideScrollContainer.getActor().setHeight(height * 0.8f);
        //this.outsideScrollContainer.getActor().invalidate();
        this.outsideScrollContainer.invalidate();
        //outsideScrollContainer.setClip(false);

        stage.getViewport().update(width, height);

        for(int i=0;i<buttonRects.length;i++){
            Rectangle rect = buttonRects[i];

            rect.set(width/2 - 115, height - height*0.2f - 75*(i+1), 250, 60);
        }
    }

    @Override
    public void addToList() {
        ListHolder.addGUI(this);
    }


}
