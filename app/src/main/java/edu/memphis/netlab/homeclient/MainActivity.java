package edu.memphis.netlab.homeclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.util.Locale;

import edu.memphis.cs.netlab.jnacconsumer.TemperatureReader;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = MainActivity.class.getName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    EditText editText = (EditText) findViewById(R.id.text_main_log);
    editText.setEnabled(false);

    UIHelper.registerOnClick(MainActivity.this, R.id.button_refresh_temperature,
            new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        refreshTemp();
      }
    });

    UIHelper.registerOnClick(MainActivity.this, R.id.button_reg, new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (m_bound) {
          final Runnable onSuccess = new OnRegisterIdentitySuccedss();
          m_temp_service.registerIdentity(onSuccess);
        }
      }
    });

    /* */
     // TODO:
    UIHelper.registerOnClick(MainActivity.this, R.id.button_bootstrap, new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (m_bt_bound) {

          String deviceId = "SampleDevice12345";
          String pairingCode = "unsafe";

          addLog("Start Bootstrapping for " + deviceId);
          m_bt_service.startBootStrap(
                  deviceId,
                  pairingCode,
                  devicePairingId -> {
                    addLog("Bootstrap Success : " + devicePairingId);
                }, (devicePairingId, reason) -> {
                    addLog("Bootstrap Failed for " + devicePairingId + "\r\nreason: " + reason);
                });
        }
      }
    });
    // */

    UIHelper.registerOnClick(MainActivity.this, R.id.button_grant, new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (m_bound) {
          EditText et = (EditText) findViewById(R.id.text_location);
          final String location = et.getText().toString();
          m_temp_service.requestGrantPermission(
              location,
              new OnRequestPermissionSuccess(),
              new OnRequestPermissionFail());
        }
      }
    });

  }

  @Override
  protected void onStart() {
    super.onStart();

    Intent intent = TemperatureReaderService.newIntent(getApplicationContext());
    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    Intent intentBt = BootstrapOwnerService.newIntent(getApplicationContext());
    bindService(intentBt, m_bt_service_conn, Context.BIND_AUTO_CREATE);
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (m_bound) {
      unbindService(mConnection);
      m_bound = false;
    }
  }

  private class OnRegisterIdentitySuccedss implements Runnable {

    @Override
    public void run() {
      addLog("[Success] Identity Registered");
    }
  }

  private class OnRequestPermissionSuccess implements Runnable {

    @Override
    public void run() {
      addLog("[Success] Permission Granted");
    }
  }

  private class OnRequestPermissionFail implements Runnable {

    @Override
    public void run() {
      addLog("[Fail] Permission Not Granted");
    }
  }


  private void addLog(String log) {
    try {
      EditText textView = (EditText) findViewById(R.id.text_main_log);
      assert null != textView;
      textView.append(String.format(Locale.ENGLISH, "%s\r\n", log));
    } catch (Exception e) {
      Log.w(TAG, e.getMessage());
    }
  }

  private void refreshTemp() {
    addLog("start fetching temp");
    if (m_bound) {
      setTempReading("loading...");
      EditText et = (EditText) findViewById(R.id.text_location);
      final String location = et.getText().toString();
      try {
        m_temp_service.fetchTemperature(location, new TemperatureReader.OnDataCallback() {
          @Override
          public void onData(String desc, int temperature) {
            addLog(String.format(Locale.ENGLISH, "%s : %d F", desc, temperature));
            setTempReading(String.format(Locale.ENGLISH, "%d F", temperature));
          }

          @Override
          public void onFail(String reason) {
            setTempReading("ERROR");
          }
        });
      } catch (IOException e) {
        e.printStackTrace();
        addLog(e.getMessage());
      }
    } else {
      addLog("Error: temp service is not bound to current activity");
      setTempReading("ERROR");
    }
  }

  private void setTempReading(String reading) {
    TextView tv = (TextView) findViewById(R.id.text_temp_reading);
    if (null != tv) {
      tv.setText(reading);
    }
  }

  /**
   * Defines callbacks for service binding, passed to bindService()
   */
  private ServiceConnection mConnection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName className,
                                   IBinder service) {
      TemperatureReaderService.LocalBinder binder = (TemperatureReaderService.LocalBinder) service;
      m_temp_service = binder.getService();
      m_bound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
      m_bound = false;
    }
  };

  private ServiceConnection m_bt_service_conn = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
      BootstrapOwnerService.LocalBinder binder = (BootstrapOwnerService.LocalBinder) iBinder;
      m_bt_service = binder.getService();
      m_bt_bound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      m_bt_bound = false;
    }
  };

  private boolean m_bound = false;
  private boolean m_bt_bound = false;
  private TemperatureReaderService m_temp_service = null;
  private BootstrapOwnerService m_bt_service = null;
}
