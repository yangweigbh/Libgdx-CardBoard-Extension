package com.badlogic.gdx.backends.android;

import com.badlogic.gdx.Screen;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

/** <p>
 * Represents one of many Cardboard application screens, such as a main menu, a settings menu, the
 * game screen and so on.
 * </p>
 * <p>
 * Note that {@link #dispose()} is not called automatically.
 * </p>
 * @see CardBoardGame */
public interface CardBoardScreen extends Screen {

    public void onNewFrame(HeadTransform paramHeadTransform);

    public void onDrawEye(Eye paramEye);

    public void onFinishFrame(Viewport paramViewport);

    public void onRendererShutdown();

    public void onCardboardTrigger();
}
