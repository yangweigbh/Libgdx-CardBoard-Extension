/*******************************************************************************
 * Copyright 2017 Markus Pettersson (markus.pettersson97@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

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
