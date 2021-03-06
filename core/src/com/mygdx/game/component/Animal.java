package com.mygdx.game.component;

import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mygdx.game.behaviourtree.BlackBoard;
import com.mygdx.game.component.collider.Collider;
import com.mygdx.game.entity.Entity;
import com.mygdx.game.interfaces.Functional;
import com.mygdx.game.interfaces.IInteractable;
import com.mygdx.game.util.Constants;
import com.mygdx.game.util.DataBuilder;
import com.mygdx.game.util.managers.DataManager;
import com.mygdx.game.util.managers.MessageEventSystem;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * Created by Paha on 2/26/2015.
 */
public class Animal extends Component implements IInteractable{
    @JsonIgnore
    private BehaviourManagerComp behComp;
    @JsonIgnore
    private Stats stats;
    @JsonIgnore
    private DataBuilder.JsonAnimal animalRef;
    @JsonProperty
    private String animalRefName = "";
    @JsonIgnore
    private Collider collider;
    @JsonIgnore
    private Group group;
    @JsonIgnore
    private LinkedList<Entity> attackList = new LinkedList<>();
    @JsonIgnore
    private Fixture attackSensor;
    //The Consumer function to call when I collide with something.
    @JsonIgnore
    private Consumer<Object[]> onCollideStart = args -> {
        Fixture me = (Fixture)args[0];
        Fixture other = (Fixture)args[1];

        Collider.ColliderInfo otherInfo = (Collider.ColliderInfo) other.getUserData();
        Collider.ColliderInfo myInfo = (Collider.ColliderInfo) me.getUserData();

        //If it is not a detector, the other is a bullet, hurt me! and kill the bullet!
        if (!myInfo.tags.hasTag(Constants.COLLIDER_DETECTOR) && otherInfo.owner.getTags().hasTag("projectile")) {
            this.getComponent(Stats.class).getStat("health").addToCurrent(-20);

            //If not aggressive, flee. Otherwise, attack!
            if(!animalRef.aggressive) behComp.changeTaskImmediate("fleeTarget");
            else attackTarget(otherInfo.owner.getComponent(Projectile.class).projOwner);
            otherInfo.owner.setToDestroy();

        //If I am a detector and the other is a colonist, we must attack it!
        }else if (myInfo.tags.hasTag(Constants.COLLIDER_DETECTOR) && otherInfo.tags.hasTag("entity") && otherInfo.owner.getTags().hasTag("colonist") && animalRef.aggressive) {
            if(otherInfo.owner.getTags().hasTag("alive")) attackList.add(otherInfo.owner);
        }
    };
    //The Consumer function to call when I stop colliding with something.
    @JsonIgnore
    private Consumer<Object[]> onCollideEnd = args -> {
        Fixture me = (Fixture) args[0];
        Fixture other = (Fixture) args[1]; //Get the other entity.

        Collider.ColliderInfo otherInfo = (Collider.ColliderInfo) other.getUserData();
        Collider.ColliderInfo myInfo = (Collider.ColliderInfo) me.getUserData();

        if (myInfo.tags.hasTag(Constants.COLLIDER_DETECTOR) && otherInfo.owner.getTags().hasTag("colonist")) {
            attackList.remove(otherInfo.owner);
        }
    };
    //The Consume function to call when I take damage.
    @JsonIgnore
    private Consumer<Object[]> onDamage = args -> {
        Entity other = (Entity) args[0];
        float damage = (float) args[1];

        Stats.Stat stat = stats.getStat("health");
        if (stat == null) return;
        stat.addToCurrent(damage);
    };

    public Animal() {
        super();
    }

    @Override
    public void save() {

    }

    @Override
    public void load(TLongObjectHashMap<Entity> entityMap, TLongObjectHashMap<Component> compMap) {
        if(this.animalRef == null) this.animalRef = DataManager.getData(this.animalRefName, DataBuilder.JsonAnimal.class);

        if(animalRef == null) System.out.println("Loading animal with anme: "+this.animalRefName);
        if(animalRef.boss) this.getEntityOwner().getTags().addTag("boss");
        this.getEntityOwner().name = animalRef.displayName;

        this.stats = this.getComponent(Stats.class);
        this.behComp = this.getComponent(BehaviourManagerComp.class);
        this.collider = this.getComponent(Collider.class);

        if(stats.getStat("health") == null) stats.addStat("health", 100, 100);
        if(stats.getStat("food") == null) stats.addStat("food", 100, 100);
        if(stats.getStat("water") == null) stats.addStat("water", 100, 100);

        //Remove its animal properties and make it a resource.
        stats.getStat("health").onZero = onDeath();

        this.behComp.getBlackBoard().attackRange = 15f;
        this.behComp.getBlackBoard().moveSpeed = 250f;

        MessageEventSystem.onEntityEvent(this.owner, "collide_start", onCollideStart);
        MessageEventSystem.onEntityEvent(this.owner, "collide_end", onCollideEnd);
        MessageEventSystem.onEntityEvent(this.owner, "damage", onDamage);

        if(animalRef.aggressive) addCircleSensor();
        this.group = this.getComponent(Group.class);

        this.makeBehaviourStuff();
    }

    @Override
    public void start() {
        super.start();

        load(null, null);
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        //If our attack list is not empty and our current target is invalid, attack stuff!
        boolean validTarget = behComp.getBlackBoard().target != null && behComp.getBlackBoard().target.isValid() && behComp.getBlackBoard().target.getTags().hasTag("alive");
        if(attackList.size() > 0 && !validTarget) {
            Entity target = attackList.poll(); //Get the next target off of the list.
            attackTarget(target);
            MessageEventSystem.notifyEntityEvent(target, "attacking", this.owner);
        }
    }

    private void attackTarget(Entity target){
        //If we are already attacking something, give up.
        if(this.behComp.getBehaviourStates().getCurrState().stateName.equals("attackTarget")) return;

        //If the target is alive (valid), use it. Otherwise, return.
        if(target.getTags().hasTag("alive")) behComp.getBlackBoard().target = target;
        else return;

        //Attack single or group attack.
        if(this.group != null) groupAttack(behComp.getBlackBoard().target);
        else this.behComp.changeTaskImmediate("attackTarget");
    }

    private void groupAttack(Entity target){
        if(group.getLeader() == null) return;

        //Let's get the leader of the group and set his target as our target and change the task to attacking it.
        BehaviourManagerComp leaderComp = group.getLeader().getComponent(BehaviourManagerComp.class);
        leaderComp.getBlackBoard().target = target;
        leaderComp.changeTaskImmediate("attackTarget");
        MessageEventSystem.notifyEntityEvent(target, "attacking_group", this.group);

        //Then, tell each unit in the group to attack our target.
        this.group.getGroupList().forEach(ent ->{
            System.out.println("Group member attacking");
            BehaviourManagerComp entComp = ent.getComponent(BehaviourManagerComp.class);
            entComp.getBlackBoard().target = target;
            entComp.changeTaskImmediate("attackTarget");
        });
    }

    @Override
    public void destroy(Entity destroyer) {
        super.destroy(destroyer);
        if(attackSensor != null) this.collider.getBody().destroyFixture(attackSensor);
    }

    //The callback to be called when I die!
    @JsonIgnore
    private Functional.Callback onDeath(){
        return () -> {
            Interactable interactable = this.owner.getComponent(Interactable.class);

            //If we don't have a resource to turn into, simply die.
            if(animalRef.resourceName == null)
                this.owner.setToDestroy();

            //Otherwise, prepare to be a resource!
            else {
                MessageEventSystem.unregisterEntity(this.owner); //Unregister for events.
                this.owner.getTransform().setRotation(180); //Flip me over
                this.collider.getBody().setLinearVelocity(0, 0); //0 velocity!
                this.owner.getTags().clearTags(); //Clear all tags
                this.owner.getTags().addTag("resource"); //Add the resource tag
                this.owner.destroyComponent(BehaviourManagerComp.class); //Destroy the BehaviourManagerComp
                this.owner.destroyComponent(Stats.class); //Destroy the Stats component.
                this.owner.destroyComponent(Animal.class); //Destroy this (Animal) Component.
                this.owner.destroyComponent(Group.class); //Destroy this (Animal) Component.

                Resource res = this.owner.addComponent(new Resource()); //Add a Resource Component.
                res.copyResource(DataManager.getData(animalRef.resourceName, DataBuilder.JsonResource.class));
                if (interactable != null) interactable.setInterType("resource");
            }
        };
    }

    //Adds a circle sensor to this animal.
    public void addCircleSensor(){
        CircleShape circle = new CircleShape();
        circle.setRadius(10f);
        attackSensor = collider.getBody().createFixture(circle, 1f);
        attackSensor.setSensor(true);
        Collider.ColliderInfo fixtureInfo = new Collider.ColliderInfo(this.owner);
        fixtureInfo.tags.addTag(Constants.COLLIDER_DETECTOR);
        attackSensor.setUserData(fixtureInfo);
        circle.dispose();
    }

    private void makeBehaviourStuff(){
        this.behComp.getBehaviourStates().addState("attackTarget").setRepeat(false);

        BlackBoard bb = this.getBehManager().getBlackBoard();
        bb.idleDistance = 3;
        bb.baseIdleTime = 0.3f;
        bb.randomIdleTime = 1f;
    }

    @JsonIgnore
    public void setGroup(Group group){
        this.group = group;
    }

    @JsonIgnore
    public void setAnimalRef(String refName){
        this.animalRef = DataManager.getData(refName, DataBuilder.JsonAnimal.class);
        this.animalRefName = refName;
    }

    @JsonIgnore
    public DataBuilder.JsonAnimal getAnimalRef() {
        return animalRef;
    }

    @JsonIgnore
    public void setAnimalRef(DataBuilder.JsonAnimal animalRef){
        this.animalRef = animalRef;
        this.animalRefName = animalRef.name;
    }

    @Override
    @JsonIgnore
    public Inventory getInventory() {
        return null;
    }

    @Override
    @JsonIgnore
    public Stats getStats() {
        return this.stats;
    }

    @Override
    @JsonIgnore
    public String getStatsText() {
        return null;
    }

    @Override
    @JsonIgnore
    public String getName() {
        return this.getEntityOwner().name;
    }

    @Override
    @JsonIgnore
    public BehaviourManagerComp getBehManager() {
        return this.behComp;
    }

    @Override
    @JsonIgnore
    public Component getComponent() {
        return this;
    }

    @Override
    @JsonIgnore
    public Constructable getConstructable() {
        return null;
    }

    @Override
    public CraftingStation getCraftingStation() {
        return null;
    }

    @Override
    public Building getBuilding() {
        return null;
    }

    @Override
    public Enterable getEnterable() {
        return null;
    }
}
