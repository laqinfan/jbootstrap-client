/*
 * Copyright (C) 2018-2019 Lei Pi, Laqin Fan
 */

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

import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.pib.AndroidSqlite3Pib;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.tpm.TpmBackEndFile;

import java.io.IOException;
import java.util.Locale;

import static edu.memphis.cs.netlab.nacapp.KeyChainHelper.pibPath;
import static edu.memphis.cs.netlab.nacapp.KeyChainHelper.pib;
import static edu.memphis.cs.netlab.nacapp.KeyChainHelper.keyChain;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    EditText editText = (EditText) findViewById(R.id.text_main_log);
    editText.setEnabled(false);

    String rootPath = getApplicationContext().getFilesDir().toString();
    Context context = getApplicationContext();

    pibPath = "pib-sqlite3:" + rootPath;

    try {
      pib = new AndroidSqlite3Pib(rootPath, "/pib.db");
    } catch (PibImpl.Error error) {
      error.printStackTrace();
    }

    try {
      pib.setTpmLocator("tpm-file:" + TpmBackEndFile.getDefaultDirecoryPath(context.getFilesDir()));
    } catch (PibImpl.Error error) {
      error.printStackTrace();
    }

    try {
      keyChain = new KeyChain(pibPath, pib.getTpmLocator());
    } catch (KeyChain.Error error) {
      error.printStackTrace();
    } catch (PibImpl.Error error) {
      error.printStackTrace();
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    /*bootstrap*/
    UIHelper.registerOnClick(MainActivity.this, R.id.button_bootstrap, new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (m_bt_bound) {

          EditText et_deviceid = (EditText) findViewById(R.id.text_deviceid);
          final String deviceId = et_deviceid.getText().toString();

          EditText et_paircode = (EditText) findViewById(R.id.text_paircode);
          final String pairingCode = et_paircode.getText().toString();

          addLog("Start Bootstrapping for " + deviceId);
          try {
            m_bt_service.startBootStrap(
                    deviceId,
                    pairingCode,
                    devicePairingId -> {
                      addLog("Bootstrap Success : " + devicePairingId);
                  }, (devicePairingId, reason) -> {
                      addLog("Bootstrap Failed for " + devicePairingId + "\r\nreason: " + reason);
                  });
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    });

  }

  @Override
  protected void onStart() {
    super.onStart();

//    Intent intent = TemperatureReaderService.newIntent(getApplicationContext());
//    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    Intent intentBt = BootstrapOwnerService.newIntent(getApplicationContext());
    bindService(intentBt, m_bt_service_conn, Context.BIND_AUTO_CREATE);
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
  private BootstrapOwnerService m_bt_service = null;
}
