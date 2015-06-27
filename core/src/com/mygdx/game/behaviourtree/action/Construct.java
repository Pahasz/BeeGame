package com.mygdx.game.behaviourtree.action;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.mygdx.game.ColonyGame;
import com.mygdx.game.behaviourtree.LeafTask;
import com.mygdx.game.component.Constructable;
import com.mygdx.game.util.BlackBoard;
import com.mygdx.game.util.timer.RepeatingTimer;
import com.mygdx.game.util.timer.Timer;

/**
 * Created by Paha on 6/22/2015.
 */
public class Construct extends LeafTask{
    private Constructable constructable;
    private Timer buildTimer;
    private Sound hammerSound;

    public Construct(String name, BlackBoard blackBoard) {
        super(name, blackBoard);
    }

    @Override
    public boolean check() {
        return super.check();
    }

    @Override
    public void start() {
        super.start();

        this.constructable = this.blackBoard.target.getComponent(Constructable.class);
        this.buildTimer = new RepeatingTimer(0.1, false, () -> {
            int rand = MathUtils.random(3) + 1;
            ColonyGame.assetManager.get("hammer_"+rand, Sound.class).play();
            int result = this.constructable.build();
            if(this.constructable.isComplete())
                this.control.finishWithSuccess();
            else if(result == -1) {
                this.control.finishWithFailure();
            }
        });
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        this.buildTimer.update(delta);
    }
}