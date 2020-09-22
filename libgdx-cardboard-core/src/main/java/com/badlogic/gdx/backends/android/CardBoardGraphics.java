/*******************************************************************************
 * Copyright 2015 wei yang(yangweigbh@hotmail.com)
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

import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowManager;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.backends.android.surfaceview.GLSurfaceView20;
import com.badlogic.gdx.backends.android.surfaceview.GdxEglConfigChooser;
import com.badlogic.gdx.backends.android.surfaceview.ResolutionStrategy;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLVersion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.WindowedMean;
import com.badlogic.gdx.utils.Array;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

public class CardBoardGraphics implements Graphics, GvrView.StereoRenderer{

   private static final String LOG_TAG = "AndroidGraphics";

   /** When {@link AndroidFragmentApplication#onPause()} or {@link AndroidApplication#onPause()} call
    * {@link AndroidGraphics#pause()} they <b>MUST</b> enforce continuous rendering. If not, {@link #onDrawEye(Eye)} will not
    * be called in the GLThread while {@link #pause()} is sleeping in the Android UI Thread which will cause the
    * {@link AndroidGraphics#pause} variable never be set to false. As a result, the {@link AndroidGraphics#pause()} method will
    * kill the current process to avoid ANR */
   static volatile boolean enforceContinuousRendering = false;

   final View view;
   int width;
   int height;
   int safeInsetLeft, safeInsetTop, safeInsetBottom, safeInsetRight;
   AndroidApplicationBase app;
   GL20 gl20;
   GL30 gl30;
   GLVersion glVersion;
   String extensions;
   private boolean disposed = false;

   protected long lastFrameTime = System.nanoTime();
   protected float deltaTime = 0;
   protected long frameStart = System.nanoTime();
   protected long frameId = -1;
   protected int frames = 0;
   protected int fps;
   protected WindowedMean mean = new WindowedMean(5);

   volatile boolean created = false;
   volatile boolean running = false;
   volatile boolean pause = false;
   volatile boolean resume = false;
   volatile boolean destroy = false;

   private float ppiX = 0;
   private float ppiY = 0;
   private float ppcX = 0;
   private float ppcY = 0;
   private float density = 1;

   protected final AndroidApplicationConfiguration config;
   private BufferFormat bufferFormat = new BufferFormat(5, 6, 5, 0, 16, 0, 0, false);
   private boolean isContinuous = true;

   public CardBoardGraphics (AndroidApplicationBase application, AndroidApplicationConfiguration config,
                             ResolutionStrategy resolutionStrategy) {
      this(application, config, resolutionStrategy, true);
   }

   public CardBoardGraphics (AndroidApplicationBase application, AndroidApplicationConfiguration config,
                             ResolutionStrategy resolutionStrategy, boolean focusableView) {
      this.config = config;
      this.app = application;
      view = createCardBoardView(application);
      preserveEGLContextOnPause();
      if (focusableView) {
         view.setFocusable(true);
         view.setFocusableInTouchMode(true);
      }
   }

   protected void preserveEGLContextOnPause () {
      if (view instanceof GLSurfaceView20) {
         try {
            view.getClass().getMethod("setPreserveEGLContextOnPause", boolean.class).invoke(view, true);
         } catch (Exception e) {
            Gdx.app.log(LOG_TAG, "Method GLSurfaceView.setPreserveEGLContextOnPause not found");
         }
      }
   }

   protected View createCardBoardView(AndroidApplicationBase application) {
      GvrView cardboardView = new GvrView(application.getContext());
      cardboardView.setRenderer(this);
      return cardboardView;
   }

   public void onPauseGLSurfaceView () {
      if (view != null) {
         if (view instanceof GLSurfaceView) ((GLSurfaceView)view).onPause();
      }
   }

   public void onResumeGLSurfaceView () {
      if (view != null) {
         if (view instanceof GLSurfaceView) ((GLSurfaceView)view).onResume();
      }
   }

   private void updatePpi () {
      DisplayMetrics metrics = new DisplayMetrics();
      app.getWindowManager().getDefaultDisplay().getMetrics(metrics);

      ppiX = metrics.xdpi;
      ppiY = metrics.ydpi;
      ppcX = metrics.xdpi / 2.54f;
      ppcY = metrics.ydpi / 2.54f;
      density = metrics.density;
   }

   /** {@inheritDoc} */
   @Override
   public GL20 getGL20 () {
      return gl20;
   }

   /** {@inheritDoc} */
   @Override
   public int getHeight () {
      return height;
   }

   /** {@inheritDoc} */
   @Override
   public int getWidth () {
      return width;
   }

   /** {@inheritDoc} */
   @Override
   public int getBackBufferWidth () {
      return width;
   }

   /** {@inheritDoc} */
   @Override
   public int getBackBufferHeight () {
      return height;
   }

   /** This instantiates the GL10, GL11 and GL20 instances. Includes the check for certain devices that pretend to support GL11 but
    * fuck up vertex buffer objects. This includes the pixelflinger which segfaults when buffers are deleted as well as the
    * Motorola CLIQ and the Samsung Behold II. */
   private void setupGL () {
      AndroidGL20 candidate = new AndroidGL20();

      String versionString = candidate.glGetString(GL10.GL_VERSION);
      String vendorString = candidate.glGetString(GL10.GL_VENDOR);
      String rendererString = candidate.glGetString(GL10.GL_RENDERER);
      glVersion = new GLVersion(Application.ApplicationType.Android, versionString, vendorString, rendererString);

      if (config.useGL30 && glVersion.getMajorVersion() > 2) {
         if (gl30 != null) return;
         gl20 = gl30 = new AndroidGL30();

         Gdx.gl = gl30;
         Gdx.gl20 = gl30;
         Gdx.gl30 = gl30;
      } else {
         if (gl20 != null) return;
         gl20 = candidate;

         Gdx.gl = candidate;
         Gdx.gl20 = candidate;
      }

      Gdx.app.log(LOG_TAG, "OGL renderer: " + rendererString);
      Gdx.app.log(LOG_TAG, "OGL vendor: " + vendorString);
      Gdx.app.log(LOG_TAG, "OGL version: " + versionString);
      Gdx.app.log(LOG_TAG, "OGL extensions: " + gl20.glGetString(GL10.GL_EXTENSIONS));
   }

   @Override
   public void onSurfaceChanged (int width, int height) {
      this.width = width;
      this.height = height;
      updatePpi();
      updateSafeAreaInsets();
      gl20.glViewport(0, 0, this.width, this.height);
      if (!created) {
         app.getApplicationListener().create();
         created = true;
         synchronized (this) {
            running = true;
         }
      }
      app.getApplicationListener().resize(width, height);
   }

   @Override
   public void onSurfaceCreated (EGLConfig config) {
      setupGL();
      logConfig(config);
      updatePpi();
      updateSafeAreaInsets();

      Mesh.invalidateAllMeshes(app);
      Texture.invalidateAllTextures(app);
      Cubemap.invalidateAllCubemaps(app);
      ShaderProgram.invalidateAllShaderPrograms(app);
      FrameBuffer.invalidateAllFrameBuffers(app);

      logManagedCachesStatus();

      Display display = app.getWindowManager().getDefaultDisplay();
      this.width = display.getWidth();
      this.height = display.getHeight();
      this.mean = new WindowedMean(5);
      this.lastFrameTime = System.nanoTime();

      gl20.glViewport(0, 0, this.width, this.height);
   }

   private void logConfig (EGLConfig config) {
      EGL10 egl = (EGL10)EGLContext.getEGL();
      EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
      int r = getAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0);
      int g = getAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
      int b = getAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
      int a = getAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);
      int d = getAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
      int s = getAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);
      int samples = Math.max(getAttrib(egl, display, config, EGL10.EGL_SAMPLES, 0),
              getAttrib(egl, display, config, GdxEglConfigChooser.EGL_COVERAGE_SAMPLES_NV, 0));
      boolean coverageSample = getAttrib(egl, display, config, GdxEglConfigChooser.EGL_COVERAGE_SAMPLES_NV, 0) != 0;

      Gdx.app.log(LOG_TAG, "framebuffer: (" + r + ", " + g + ", " + b + ", " + a + ")");
      Gdx.app.log(LOG_TAG, "depthbuffer: (" + d + ")");
      Gdx.app.log(LOG_TAG, "stencilbuffer: (" + s + ")");
      Gdx.app.log(LOG_TAG, "samples: (" + samples + ")");
      Gdx.app.log(LOG_TAG, "coverage sampling: (" + coverageSample + ")");

      bufferFormat = new BufferFormat(r, g, b, a, d, s, samples, coverageSample);
   }

   int[] value = new int[1];

   private int getAttrib (EGL10 egl, EGLDisplay display, EGLConfig config, int attrib, int defValue) {
      if (egl.eglGetConfigAttrib(display, config, attrib, value)) {
         return value[0];
      }
      return defValue;
   }

   Object synch = new Object();

   void resume () {
      synchronized (synch) {
         running = true;
         resume = true;
      }
   }

   void pause () {
      synchronized (synch) {
         if (!running) return;
         running = false;
         pause = true;
         while (pause) {
            try {
               // TODO: fix deadlock race condition with quick resume/pause.
               // Temporary workaround:
               // Android ANR time is 5 seconds, so wait up to 4 seconds before assuming
               // deadlock and killing process. This can easily be triggered by opening the
               // Recent Apps list and then double-tapping the Recent Apps button with
               // ~500ms between taps.
               synch.wait(4000);
               if (pause) {
                  // pause will never go false if onDrawEye is never called by the GLThread
                  // when entering this method, we MUST enforce continuous rendering
                  Gdx.app.error(LOG_TAG, "waiting for pause synchronization took too long; assuming deadlock and killing");
                  android.os.Process.killProcess(android.os.Process.myPid());
               }
            } catch (InterruptedException ignored) {
               Gdx.app.log(LOG_TAG, "waiting for pause synchronization failed!");
            }
         }
      }
   }

   void destroy () {
      synchronized (synch) {
         running = false;
         destroy = true;

         while (destroy) {
            try {
               synch.wait();
            } catch (InterruptedException ex) {
               Gdx.app.log(LOG_TAG, "waiting for destroy synchronization failed!");
            }
         }
      }
   }

   @Override
   public long getFrameId () {
      return frameId;
   }

   /** {@inheritDoc} */
   @Override
   public float getDeltaTime () {
      return mean.getMean() == 0 ? deltaTime : mean.getMean();
   }

   @Override
   public float getRawDeltaTime () {
      return deltaTime;
   }

   /** {@inheritDoc} */
   @Override
   public GraphicsType getType () {
      return GraphicsType.AndroidGL;
   }

   /** {@inheritDoc} */
   @Override
   public GLVersion getGLVersion () {
      return glVersion;
   }

   /** {@inheritDoc} */
   @Override
   public int getFramesPerSecond () {
      return fps;
   }

   public void clearManagedCaches () {
      Mesh.clearAllMeshes(app);
      Texture.clearAllTextures(app);
      Cubemap.clearAllCubemaps(app);
      ShaderProgram.clearAllShaderPrograms(app);
      FrameBuffer.clearAllFrameBuffers(app);

      logManagedCachesStatus();
   }

   protected void logManagedCachesStatus () {
      Gdx.app.log(LOG_TAG, Mesh.getManagedStatus());
      Gdx.app.log(LOG_TAG, Texture.getManagedStatus());
      Gdx.app.log(LOG_TAG, Cubemap.getManagedStatus());
      Gdx.app.log(LOG_TAG, ShaderProgram.getManagedStatus());
      Gdx.app.log(LOG_TAG, FrameBuffer.getManagedStatus());
   }

   public View getView () {
      return view;
   }

   @Override
   public float getPpiX () {
      return ppiX;
   }

   @Override
   public float getPpiY () {
      return ppiY;
   }

   @Override
   public float getPpcX () {
      return ppcX;
   }

   @Override
   public float getPpcY () {
      return ppcY;
   }

   @Override
   public float getDensity () {
      return density;
   }

   @Override
   public boolean supportsDisplayModeChange () {
      return false;
   }

   @Override
   public boolean setFullscreenMode (DisplayMode displayMode) {
      return false;
   }

   @Override
   public Monitor getPrimaryMonitor() {
      return new AndroidMonitor(0, 0, "Primary Monitor");
   }

   @Override
   public Monitor getMonitor() {
      return getPrimaryMonitor();
   }

   @Override
   public Monitor[] getMonitors() {
      return new Monitor[]{getPrimaryMonitor()};
   }

   @Override
   public DisplayMode[] getDisplayModes(Monitor monitor) {
      return getDisplayModes();
   }

   @Override
   public DisplayMode getDisplayMode(Monitor monitor) {
      return getDisplayMode();
   }

   @Override
   public DisplayMode[] getDisplayModes () {
      return new DisplayMode[] {getDisplayMode()};
   }

   protected void updateSafeAreaInsets() {
      safeInsetLeft = 0;
      safeInsetTop = 0;
      safeInsetRight = 0;
      safeInsetBottom = 0;

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
         try {
            DisplayCutout displayCutout = app.getApplicationWindow().getDecorView().getRootWindowInsets().getDisplayCutout();
            if (displayCutout != null) {
               safeInsetRight = displayCutout.getSafeInsetRight();
               safeInsetBottom = displayCutout.getSafeInsetBottom();
               safeInsetTop = displayCutout.getSafeInsetTop();
               safeInsetLeft = displayCutout.getSafeInsetLeft();
            }
         } // Some Application implementations (such as Live Wallpapers) do not implement Application#getApplicationWindow()
         catch (UnsupportedOperationException e) {
            Gdx.app.log("AndroidGraphics", "Unable to get safe area insets");
         }
      }
   }

   @Override
   public int getSafeInsetLeft() {
      return safeInsetLeft;
   }

   @Override
   public int getSafeInsetTop() {
      return safeInsetTop;
   }

   @Override
   public int getSafeInsetBottom() {
      return safeInsetBottom;
   }

   @Override
   public int getSafeInsetRight() {
      return safeInsetRight;
   }

   @Override
   public boolean setWindowedMode (int width, int height) {
      return false;
   }

   @Override
   public void setTitle (String title) {
   }

   @Override
   public void setUndecorated(boolean undecorated) {
      final int mask = (undecorated) ? 1 : 0;
      app.getApplicationWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, mask);
   }

   @Override
   public void setResizable(boolean resizable) {
   }

   @Override
   public DisplayMode getDisplayMode () {
      DisplayMetrics metrics = new DisplayMetrics();
      app.getWindowManager().getDefaultDisplay().getMetrics(metrics);
      return new AndroidDisplayMode(metrics.widthPixels, metrics.heightPixels, 0, 0);
   }

   @Override
   public BufferFormat getBufferFormat () {
      return bufferFormat;
   }

   @Override
   public void setVSync (boolean vsync) {
   }

   @Override
   public boolean supportsExtension (String extension) {
      if (extensions == null) extensions = Gdx.gl.glGetString(GL10.GL_EXTENSIONS);
      return extensions.contains(extension);
   }

   @Override
   public void setContinuousRendering (boolean isContinuous) {
      if (view != null) {
         // ignore setContinuousRendering(false) while pausing
         this.isContinuous = enforceContinuousRendering || isContinuous;
         int renderMode = this.isContinuous ? GLSurfaceView.RENDERMODE_CONTINUOUSLY : GLSurfaceView.RENDERMODE_WHEN_DIRTY;
         if (view instanceof GLSurfaceView) ((GLSurfaceView)view).setRenderMode(renderMode);
         mean.clear();
      }
   }

   @Override
   public boolean isContinuousRendering () {
      return isContinuous;
   }

   @Override
   public void requestRendering () {
      if (view != null) {
         if (view instanceof GLSurfaceView) ((GLSurfaceView)view).requestRender();
      }
   }

   @Override
   public void onDrawEye (Eye eye) {
      if (!(app.getApplicationListener() instanceof CardBoardApplicationListener)) {
         throw new RuntimeException("should implement CardBoardApplicationListener");
      }
      if (!disposed) {
         ((CardBoardApplicationListener) app.getApplicationListener())
                 .onDrawEye(eye);
      }
   }

   @Override
   public void onFinishFrame (Viewport viewport) {
      if (!(app.getApplicationListener() instanceof CardBoardApplicationListener)) {
         throw new RuntimeException("should implement CardBoardApplicationListener");
      }
      if (!disposed) {
         ((CardBoardApplicationListener) app.getApplicationListener())
                 .onFinishFrame(viewport);
      }
   }

   @Override
   public void onNewFrame (HeadTransform headTransform) {
      long time = System.nanoTime();
      deltaTime = (time - lastFrameTime) / 1000000000.0f;
      lastFrameTime = time;

      // After pause deltaTime can have somewhat huge value that destabilizes the mean, so let's cut it off
      if (!resume) {
         mean.addValue(deltaTime);
      } else {
         deltaTime = 0;
      }

      boolean lrunning = false;
      boolean lpause = false;
      boolean ldestroy = false;
      boolean lresume = false;

      synchronized (synch) {
         lrunning = running;
         lpause = pause;
         ldestroy = destroy;
         lresume = resume;

         if (resume) {
            resume = false;
         }

         if (pause) {
            pause = false;
            synch.notifyAll();
         }

         if (destroy) {
            destroy = false;
            synch.notifyAll();
         }
      }

      if (lresume) {
         Array<LifecycleListener> listeners = app.getLifecycleListeners();
         synchronized (listeners) {
            for (LifecycleListener listener : listeners) {
               listener.resume();
            }
         }
         app.getApplicationListener().resume();
         Gdx.app.log(LOG_TAG, "resumed");
      }

      if (lrunning) {
         synchronized (app.getRunnables()) {
            app.getExecutedRunnables().clear();
            app.getExecutedRunnables().addAll(app.getRunnables());
            app.getRunnables().clear();
         }

         for (int i = 0; i < app.getExecutedRunnables().size; i++) {
            try {
               app.getExecutedRunnables().get(i).run();
            } catch (Throwable t) {
               t.printStackTrace();
            }
         }
         app.getInput().processEvents();
         frameId++;
         app.getApplicationListener().render();
      }

      if (lpause) {
         Array<LifecycleListener> listeners = app.getLifecycleListeners();
         synchronized (listeners) {
            for (LifecycleListener listener : listeners) {
               listener.pause();
            }
         }
         app.getApplicationListener().pause();
         Gdx.app.log(LOG_TAG, "paused");
      }

      if (ldestroy) {
         Array<LifecycleListener> listeners = app.getLifecycleListeners();
         synchronized (listeners) {
            for (LifecycleListener listener : listeners) {
               listener.dispose();
            }
         }
         app.getApplicationListener().dispose();
         disposed  = true;
         Gdx.app.log(LOG_TAG, "destroyed");
      }

      if (time - frameStart > 1000000000) {
         fps = frames;
         frames = 0;
         frameStart = time;
      }
      frames++;
      if (!(app.getApplicationListener() instanceof CardBoardApplicationListener)) {
         throw new RuntimeException("should implement CardBoardApplicationListener");
      }
      if (!disposed) {
         ((CardBoardApplicationListener) app.getApplicationListener())
                 .onNewFrame(headTransform);
      }
   }

   @Override
   public void onRendererShutdown () {
      if (!(app.getApplicationListener() instanceof CardBoardApplicationListener)) {
         throw new RuntimeException("should implement CardBoardApplicationListener");
      }
      ((CardBoardApplicationListener)app.getApplicationListener()).onRendererShutdown();

   }

   @Override
   public boolean isFullscreen() {
      return true;
   }

   @Override
   public boolean isGL30Available() {
      return gl30 != null;
   }

   @Override
   public GL30 getGL30() {
      return gl30;
   }

   @Override
   public void setGL20(GL20 gl20) {
      this.gl20 = gl20;
      if (gl30 == null) {
         Gdx.gl = gl20;
         Gdx.gl20 = gl20;
      }
   }

   @Override
   public void setGL30(GL30 gl30) {
      this.gl30 = gl30;
      if (gl30 != null) {
         this.gl20 = gl30;

         Gdx.gl = gl20;
         Gdx.gl20 = gl20;
         Gdx.gl30 = gl30;
      }
   }

   @Override
   public Cursor newCursor(Pixmap pixmap, int xHotspot, int yHotspot) {
      return null;
   }

   @Override
   public void setCursor(Cursor cursor) {
   }

   @Override
   public void setSystemCursor(Cursor.SystemCursor systemCursor) {
   }

   private class AndroidDisplayMode extends DisplayMode {
      protected AndroidDisplayMode(int width, int height, int refreshRate, int bitsPerPixel) {
         super(width, height, refreshRate, bitsPerPixel);
      }
   }

   private class AndroidMonitor extends Monitor {
      public AndroidMonitor(int virtualX, int virtualY, String name) {
         super(virtualX, virtualY, name);
      }
   }
}
