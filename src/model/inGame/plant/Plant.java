package model.inGame.plant;

import model.GameEngine;
import model.Position;
import model.enums.PlantType;

import java.util.HashSet;
import java.util.Set;

public class Plant {
    private PlantType type;
    private int hp;
    private int rechargeTime;
    private int actionTime;
    private int cost;
    private PlantCategory category;
    private Position position;
    private Set<PlantTag> tags = new HashSet<>();
    private SunProduceBehavior sunProduceBehavior;
    private AttackBehavior attackBehavior;
    private SpecialAbility specialAbility;

    public Plant(PlantType type, int hp, int rechargeTime, int actionTime, int cost,
                 PlantCategory category, SunProduceBehavior sunProduceBehavior,
                 AttackBehavior attackBehavior, SpecialAbility specialAbility) {
        this.type = type;
        this.hp = hp;
        this.rechargeTime = rechargeTime;
        this.actionTime = actionTime;
        this.cost = cost;
        this.category = category;
        this.sunProduceBehavior = sunProduceBehavior;
        this.attackBehavior = attackBehavior;
        this.specialAbility = specialAbility;
    }

    public void act(GameEngine engine) {
        if (sunProduceBehavior != null) {
            sunProduceBehavior.produce(this, engine);
        }
        if (attackBehavior != null) {
            attackBehavior.attack(this, engine);
        }
    }

    public void takeDamage(int amount) {
        this.hp -= amount;
        if (this.hp < 0) {
            this.hp = 0;
        }
    }

    public void usePlantFood(GameEngine engine) {
        if (specialAbility != null) {
            specialAbility.activate(this, engine);
        }
    }

    public boolean isDead() {
        return hp <= 0;
    }

    public PlantType getType() {
        return type;
    }

    public int getHp() {
        return hp;
    }

    public int getRechargeTime() {
        return rechargeTime;
    }

    public int getActionTime() {
        return actionTime;
    }

    public int getCost() {
        return cost;
    }

    public PlantCategory getCategory() {
        return category;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Set<PlantTag> getTags() {
        return tags;
    }
}