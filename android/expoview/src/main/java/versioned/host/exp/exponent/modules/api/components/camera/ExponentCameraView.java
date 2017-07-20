package versioned.host.exp.exponent.modules.api.components.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.uimanager.ThemedReactContext;
import com.google.android.cameraview.CameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import host.exp.exponent.utils.ExpFileUtils;
import io.fabric.sdk.android.services.concurrency.AsyncTask;

public class ExponentCameraView extends CameraView implements LifecycleEventListener {

  private ThemedReactContext mThemedReactContext;
  private Queue<Promise> pictureTakenPromises = new ConcurrentLinkedQueue<>();

  public ExponentCameraView(ThemedReactContext themedReactContext) {
    super(themedReactContext);
    mThemedReactContext = themedReactContext;

    mThemedReactContext.addLifecycleEventListener(this);

    addCallback(new Callback() {
      @Override
      public void onPictureTaken(CameraView cameraView, final byte[] data) {
        final Promise promise = pictureTakenPromises.poll();
        AsyncTask.execute(new Runnable() {
          @Override
          public void run() {
            promise.resolve(
              ExpFileUtils.uriFromFile(
                new File(writeImage(BitmapFactory.decodeByteArray(data, 0, data.length),
                CameraModule.getScopedContextSingleton().getCacheDir()))
              ).toString()
            );
          }
        });
      }
    });
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
  }

  @Override
  public void onViewAdded(View child) {
    if (this.getView() == child || this.getView() == null) return;
    // remove and readd view to make sure it is in the back.
    // @TODO figure out why there was a z order issue in the first place and fix accordingly.
    this.removeView(this.getView());
    this.addView(this.getView(), 0);
  }

  private String writeImage(Bitmap image, File cacheDir) {
    FileOutputStream out = null;
    String path = null;
    try {
      File directory = new File(cacheDir + File.separator + "Camera");
      ExpFileUtils.ensureDirExists(directory);
      String filename = UUID.randomUUID().toString();
      path = directory + File.separator + filename + ".jpg";
      out = new FileOutputStream(path);
      image.compress(Bitmap.CompressFormat.JPEG, 100, out);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (out != null) {
          out.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return path;
  }

  public void takePicture(final Promise promise) {
    pictureTakenPromises.add(promise);
    super.takePicture();
  }

  @Override
  public void onHostResume() {
    start();
  }

  @Override
  public void onHostPause() {
    stop();
  }

  @Override
  public void onHostDestroy() {
    stop();
  }
}
