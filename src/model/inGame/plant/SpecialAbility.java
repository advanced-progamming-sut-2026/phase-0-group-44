package model.inGame.plant;

import model.GameEngine;

interface SpecialAbility {
    void activate(Plant plant, GameEngine engine);
}