package pvz.browser.widgets;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;

public class TabButton extends Button {
        private final Drawable idle;
        private final Drawable active;
        private final Label label;
        private final float bodyWidth;
        private final float bodyHeight;

        TabButton(String text, Label.LabelStyle labelStyle, Drawable idle, Drawable active) {
            super(new ButtonStyle());
            this.idle = idle;
            this.active = active;
            this.bodyWidth = idle.getMinWidth();
            this.bodyHeight = idle.getMinHeight();
            this.label = new Label(text, labelStyle);
            // this.label.setFontScale(2);
            this.label.setAlignment(Align.center);
            setProgrammaticChangeEvents(false);
        }

        Label getLabel() {
            return label;
        }

        @Override public float getPrefWidth() { return bodyWidth; }
        @Override public float getPrefHeight() { return bodyHeight; }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            Drawable art = isChecked() ? active : idle;
            float topY = getY() + getHeight();
            float artH = art.getMinHeight();
            // Anchor the art to the top of the body; a taller (checked) page overflows below the button bounds.
            batch.setColor(1f, 1f, 1f, parentAlpha);
            art.draw(batch, getX(), topY - artH, getWidth(), artH);
            // Centre the label in the body band (the top bodyHeight), keeping it clear of the overflowing arrow.
            label.setSize(getWidth(), bodyHeight);
            label.setPosition(getX(), topY - bodyHeight);
            label.draw(batch, parentAlpha);
            batch.setColor(Color.WHITE);
        }
    }