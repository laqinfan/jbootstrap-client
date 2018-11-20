package edu.memphis.netlab.homeclient;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import net.named_data.jndn.Name;

import java.io.IOException;

import edu.memphis.cs.netlab.jnacconsumer.TemperatureReader;
import edu.memphis.cs.netlab.nacapp.AndroidConsumerSQLiteDBSource;
import edu.memphis.netlab.homeclient.node.NodeService;

import static edu.memphis.netlab.homeclient.Global.SCHEDULED_EXECUTOR_SERVICE;


public class TemperatureReaderService extends NodeService {

  private static final String TAG = TemperatureReaderService.class.getName();

  public TemperatureReaderService() {
    super();
    Name group = new Name(Global.LOCAL_HOME + "/READ");
    m_temperatureReader = new TemperatureReader(new Name(Global.DEVICE_PREFIX + "/temperature-client"),
            group,
            new AndroidConsumerSQLiteDBSource(":memory:"));
  }

  public static Intent newIntent(Context context) {
    return new Intent(context, TemperatureReaderService.class);
  }

  public void registerIdentity(Runnable onSuccess) {
    m_temperatureReader.registerIdentity(onSuccess);
  }

  public void requestGrantPermission(String location, Runnable onSuccess,
                                     Runnable onFail) {
    m_temperatureReader.requestGrantPermission(location, onSuccess, onFail);
  }


  public void fetchTemperature(String location,
                               final TemperatureReader.OnDataCallback callback)
      throws IOException {
    SCHEDULED_EXECUTOR_SERVICE.submit(new FetchTempTask(new Name(location), callback));
  }

  private class FetchTempTask implements Runnable {
    private final Name m_location;
    private final TemperatureReader.OnDataCallback m_callback;

    FetchTempTask(Name location, TemperatureReader.OnDataCallback callback) {
      m_location = location;
      m_callback = callback;
    }

    @Override
    public void run() {
      m_temperatureReader.read(m_location, m_callback);
    }
  }

  ////////////////////////////////////////////////////////

  @Override
  public void onCreate() {
    super.onCreate();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public IBinder onBind(Intent intent) {
    if (!m_is_running) {
      startService(TemperatureReaderService.newIntent(this));
      m_temperatureReader.startFaceProcessing();
    }
    m_is_running = true;
    return m_localBinder;
  }

  public class LocalBinder extends Binder {
    public TemperatureReaderService getService() {
      return TemperatureReaderService.this;
    }
  }

  ////////////////////////////////////////////////////////

  private static boolean m_is_running = false;

  private IBinder m_localBinder = new LocalBinder();

  private TemperatureReader m_temperatureReader;
}
