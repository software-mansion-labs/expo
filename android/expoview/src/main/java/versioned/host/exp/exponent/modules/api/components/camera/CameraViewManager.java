package versioned.host.exp.exponent.modules.api.components.camera;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.google.android.cameraview.AspectRatio;

import java.io.File;
import java.util.Set;

import host.exp.exponent.utils.ScopedContext;


public class CameraViewManager extends ViewGroupManager<ExponentCameraView> {
  private static final String REACT_CLASS = "ExponentCamera";

  private static CameraViewManager instance;
  private ExponentCameraView mCameraView;

  public CameraViewManager() {
    super();
    instance = this;
  }

  public static CameraViewManager getInstance() { return instance; }

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  protected ExponentCameraView createViewInstance(ThemedReactContext reactContext) {
    mCameraView = new ExponentCameraView(reactContext);
    return mCameraView;
  }

  @ReactProp(name = "type")
  public void setType(ExponentCameraView view, int type) {
    view.setFacing(type);
  }

  @ReactProp(name = "ratio")
  public void setRatio(ExponentCameraView view, String ratio) {
    view.setAspectRatio(AspectRatio.parse(ratio));
  }

  @ReactProp(name = "flashMode")
  public void setFlashMode(ExponentCameraView view, int torchMode) {
    view.setFlash(torchMode);
  }

  @ReactProp(name = "autoFocus")
  public void setAutoFocus(ExponentCameraView view, boolean autoFocus) {
    view.setAutoFocus(autoFocus);
  }

  @ReactProp(name = "focusDepth")
  public void setFocusDepth(ExponentCameraView view, float depth) {
    view.setFocusDepth(depth);
  }

  @ReactProp(name = "zoom")
  public void setZoom(ExponentCameraView view, float zoom) {
    view.setZoom(zoom);
  }

  public void takePicture(Promise promise) {
    if (mCameraView.isCameraOpened()) {
      mCameraView.takePicture(promise);
    } else {
      promise.reject("E_CAMERA_UNAVAILABLE", "Camera is not running");
    }
  }

  public Set<AspectRatio> getSupportedRatios() {
    if (mCameraView.isCameraOpened()) {
      return mCameraView.getSupportedAspectRatios();
    }
    return null;
  }
}
