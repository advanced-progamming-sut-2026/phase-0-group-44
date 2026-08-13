package pvz.browser.screens.animations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import pvz.browser.core.BrowserContext;
import pvz.browser.core.UiManager;

public class PamActor extends Table {
    private Rectangle animationBounds;
    private boolean fitToBounds = true;
    private boolean fitWholeDuration = false;

    private String pam;
    private float stateTime = 0;
    private boolean playing = true;

    private String currentClip;
    private float currentDuration;

    public PamActor(String pam) {
        setPam(pam);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (pam == null || animationBounds == null) return;
        
        if (!fitToBounds) {
            BrowserContext.player.draw(batch, pam, currentClip, stateTime, getX(), getY(), true);
            return;
        }
        float s = Math.min(getWidth() / animationBounds.width, getHeight() / animationBounds.height);
        s = Math.min(s, 1f);

        float localCenterX = animationBounds.x + animationBounds.width / 2f;
        float localCenterY = -(animationBounds.y + animationBounds.height / 2f);

        Matrix4 oldTransform = batch.getTransformMatrix().cpy();
        Matrix4 transform = new Matrix4(oldTransform);
        transform.translate(getX() + getWidth() / 2f, getY() + getHeight() / 2f, 0);
        transform.scale(s, s, 1f);
        transform.translate(-localCenterX, -localCenterY, 0);

        batch.setTransformMatrix(transform);
        BrowserContext.player.draw(batch, pam, currentClip, stateTime, 0, 0, true);
        batch.setTransformMatrix(oldTransform);
    }

    public void setClip(String clip) {
        if(pam == null) return;
        
        currentClip = clip;
        if(fitWholeDuration) animationBounds = BrowserContext.player.bounds(pam, null);
        else animationBounds = BrowserContext.player.bounds(pam, currentClip);
        currentDuration = BrowserContext.player.clipDurationSeconds(pam, currentClip);
        resetTime();
    }

    public void setPam(String pam) {
        if(pam == null) return;
        
        BrowserContext.player.loadAsync(pam, ()->{
            this.pam = pam;
            List<String> clips = BrowserContext.player.clips(pam);
            setClip(clips.isEmpty() ? null : clips.get(0));
        });
    }
    
    @Override
    public void act(float delta) {
        super.act(delta);
        if(pam == null) return;
        
        if(playing){
            stateTime += delta;
            if(stateTime > currentDuration) {
                onClipFinished(currentClip);
                stateTime = 0;
            }
        }
    }
    
    public void setFitToBounds(boolean fitToBounds) {
        this.fitToBounds = fitToBounds;
    }

    public void setFitWholeDuration(boolean fitWholeDuration) {
        this.fitWholeDuration = fitWholeDuration;
    }
    
    public void resetTime(){
        stateTime = 0;
    }

    public void onClipFinished(String clip) {

    }

    public float getStateTime() {
        return stateTime;
    }

    public void setStateTime(float stateTime) {
        this.stateTime = stateTime;
    }
}
