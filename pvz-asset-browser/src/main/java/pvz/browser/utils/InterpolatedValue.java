package pvz.browser.utils;

import com.badlogic.gdx.scenes.scene2d.Actor;

public class InterpolatedValue{
    public float value;
    private float target;
    private float rate;
    private float targetMin, targetMax;

    

    public InterpolatedValue(float value, float rate, float targetMin, float targetMax) {
        this.value = value;
        this.rate = rate;
        this.targetMin = targetMin;
        this.targetMax = targetMax;
        target = value;
    }

    public void act(float delta) {
        float dist = target - value;
        value += dist * rate;
    }

    public void setTarget(float target) {
        this.target = Math.min(targetMax, Math.max(target, targetMin));
    }

    public float getTarget() {
        return target;
    }

    public void setRate(float rate) {
        this.rate = Math.min(1, Math.max(rate, 0));;
    }

    public void setMin(float targetMin) {
        this.targetMin = targetMin;
    }

    public void setMax(float targetMax) {
        this.targetMax = targetMax;
    }

    public float getValue() {
        return value;
    }
}
