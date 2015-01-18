package com.mygdx.game.behaviourtree.control;

import com.mygdx.game.behaviourtree.Task;

import java.util.ArrayList;

/**
 * Created by Bbent_000 on 12/31/2014.
 */
public class ParentTaskController extends TaskController{
    protected ArrayList<Task> subTasks = new ArrayList<>();
    public Task currTask = null;
    public int currIndex = 0;


    public ParentTaskController(Task task) {
        super(task);
    }

    @Override
    public void reset() {
        super.reset();

        for(Task task : this.subTasks)
            task.getControl().reset();

        this.currIndex = 0;
        this.currTask = subTasks.get(this.currIndex);
    }

    /**
     * Gets the ArrayList of sub tasks in this controller.
     * @return An ArrayList of Tasks that are the sub tasks of this controller.
     */
    public ArrayList<Task> getSubTasks(){
        return this.subTasks;
    }

    /**
     * Adds a Task to this controller.
     * @param task The Task to add to this controller.
     */
    public void addTask(Task task) {
        this.subTasks.add(task);
    }

    public Task getCurrTask(){
        return this.currTask;
    }
}