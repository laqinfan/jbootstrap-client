package edu.memphis.netlab.homeclient;

import android.app.Activity;
import android.view.View;
import android.widget.Button;

/**
 * Description:
 * <p>
 * Author: lei
 */

public class UIHelper {

  public static Button
  registerOnClick(Activity v, int buttonId, View.OnClickListener listener) {
    Button btn = (Button) v.findViewById(buttonId);
    if (btn != null) {
      btn.setOnClickListener(listener);
    }
    return btn;
  }

  public static abstract class FindTask implements Runnable {

    public void onNotFound() {
    }

    public View getView() {
      return mView;
    }

    public void setView(View mView) {
      this.mView = mView;
    }

    private View mView = null;
  }

  public static void find(Activity act, int id, FindTask t) {
    View vw = act.findViewById(id);
    if (vw != null) {
      t.setView(vw);
      t.run();
    } else {
      t.onNotFound();
    }
  }
}
