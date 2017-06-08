package com.badlogic.gdx.backends.android;

import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

/** Convenience implementation of {@link CardBoardScreen}. Derive from this and only override
 * what you need. */
public abstract class CardBoardScreenAdapter implements CardBoardScreen {
    @Override
    public void render (float delta) {
    }

    @Override
    public void resize (int width, int height) {
    }

    @Override
    public void show () {
    }

    @Override
    public void hide () {
    }

    @Override
    public void pause () {
    }

    @Override
    public void resume () {
    }

    @Override
    public void dispose () {
    }

    @Override
    public void onNewFrame(HeadTransform paramHeadTransform) {

    }

    @Override
    public void onDrawEye(Eye paramEye) {

    }

    @Override
    public void onFinishFrame(Viewport paramViewport) {

    }

    @Override
    public void onRendererShutdown() {

    }

    @Override
    public void onCardboardTrigger() {

    }
}
