package com.badlogic.gdx.backends.android;

import com.badlogic.gdx.ApplicationListener;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

public interface CardBoardApplicationListener extends ApplicationListener{

   public abstract void onNewFrame(HeadTransform paramHeadTransform);

   public abstract void onDrawEye(Eye paramEye);

   public abstract void onFinishFrame(Viewport paramViewport);

   public abstract void onRendererShutdown();
}
