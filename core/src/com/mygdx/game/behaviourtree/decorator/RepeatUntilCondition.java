package com.mygdx.game.behaviourtree.decorator;

import com.mygdx.game.behaviourtree.Task;
import com.mygdx.game.component.BlackBoard;

/**
 * Created by Paha on 4/6/2015.
 */
public class RepeatUntilCondition extends TaskDecorator{
    public RepeatUntilCondition(String name, BlackBoard bb, Task task) {
        super(name, bb, task);
    }

    @Override
    public boolean check() {
        return super.check();
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void update(float delta) {
        if(control.callbacks.successCriteria != null && control.callbacks.successCriteria.criteria(this.task)){
            this.control.finishWithSuccess();
        }else {
            if(this.task.getControl().hasFinished())
                this.task.getControl().reset();
            this.task.update(delta);
        }
    }

    @Override
    public void end() {
        super.end();
    }
}
