package com.badlogic.gdx.backends.android;

import com.badlogic.gdx.ApplicationListener;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

public interface CardBoardApplicationListener extends ApplicationListener{

   public void onNewFrame(HeadTransform paramHeadTransform);

   public void onDrawEye(Eye paramEye);

   public void onFinishFrame(Viewport paramViewport);

   public void onRendererShutdown();

   public void onCardboardTrigger();
}
