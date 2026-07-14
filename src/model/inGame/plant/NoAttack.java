package model.inGame.plant;

import model.GameEngine;

class NoAttack implements AttackBehavior {
    @Override
    public void attack(Plant plant, GameEngine engine) {
        // this plant does not attack
    }
}
