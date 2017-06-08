package com.badlogic.gdx.backends.android;

import com.badlogic.gdx.Gdx;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

/** <p>
 * An {@link CardBoardApplicationListener} that delegates to a {@link CardBoardScreen}. This allows
 * an application to easily have multiple screens.
 * </p>
 * <p>
 * Screens are not disposed automatically. You must handle whether you want to keep screens around
 * or dispose of them when another screen is set.
 * </p> */
public abstract class CardBoardGame implements CardBoardApplicationListener {
    protected CardBoardScreen screen;

    @Override
    public void dispose () {
        if (screen != null) screen.hide();
    }

    @Override
    public void pause () {
        if (screen != null) screen.pause();
    }

    @Override
    public void resume () {
        if (screen != null) screen.resume();
    }

    @Override
    public void render () {
        if (screen != null) screen.render(Gdx.graphics.getDeltaTime());
    }

    @Override
    public void resize (int width, int height) {
        if (screen != null) screen.resize(width, height);
    }

    @Override
    public void onNewFrame(HeadTransform paramHeadTransform) {
        if (screen != null) screen.onNewFrame(paramHeadTransform);
    }

    @Override
    public void onDrawEye(Eye paramEye) {
        if (screen != null) screen.onDrawEye(paramEye);
    }

    @Override
    public void onFinishFrame(Viewport paramViewport) {
        if (screen != null) screen.onFinishFrame(paramViewport);
    }

    @Override
    public void onRendererShutdown() {
        if (screen != null) screen.onRendererShutdown();
    }

    @Override
    public void onCardboardTrigger() {
        if (screen != null) screen.onCardboardTrigger();
    }

    /** Sets the current screen. {@link CardBoardScreen#hide()} is called on any old screen, and
     * {@link CardBoardScreen#show()} is called on the new screen, if any.
     * @param cardBoardScreen may be {@code null} */
    public void setScreen (CardBoardScreen cardBoardScreen) {
        if (this.screen != null) this.screen.hide();
        this.screen = cardBoardScreen;
        if (this.screen != null) {
            this.screen.show();
            this.screen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
    }

    /** @return the currently active {@link CardBoardScreen}. */
    public CardBoardScreen getScreen() {
        return this.screen;
    }
}
