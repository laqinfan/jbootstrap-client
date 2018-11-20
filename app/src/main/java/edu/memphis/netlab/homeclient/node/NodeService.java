package edu.memphis.netlab.homeclient.node;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import net.named_data.jndn.Name;

import edu.memphis.cs.netlab.nacapp.NACNode;
import edu.memphis.netlab.homeclient.Global;

public class NodeService extends Service {


  ////////////////////////////////////////////////////////
  // Android Service LifeCycle API
  ////////////////////////////////////////////////////////
  @Override
  public void onCreate() {
    m_node = new NACNode();
    m_node.init(new Name(Global.APP_PREFIX));
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    m_node.startFaceProcessing();
    return START_STICKY;
  }

  ////////////////////////////////////////////////////////

  @Override
  public IBinder onBind(Intent intent) {
    return m_localBinder;
  }

  public class LocalBinder extends Binder {
    public NodeService getService() {
      return NodeService.this;
    }
  }


  private final static String TAG = NodeService.class.getName();

  protected NACNode m_node;

  private LocalBinder m_localBinder = new LocalBinder();
}
