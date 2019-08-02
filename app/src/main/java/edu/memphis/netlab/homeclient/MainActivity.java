/*
 * Copyright (C) 2018-2019 Lei Pi, Laqin Fan
 */

package edu.memphis.netlab.homeclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.pib.AndroidSqlite3Pib;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.tpm.TpmBackEndFile;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.IOException;
import java.util.Locale;

import static com.google.zxing.integration.android.IntentIntegrator.QR_CODE_TYPES;
import static edu.memphis.cs.netlab.nacapp.KeyChainHelper.pibPath;
import static edu.memphis.cs.netlab.nacapp.KeyChainHelper.pib;
import static edu.memphis.cs.netlab.nacapp.KeyChainHelper.keyChain;
import static edu.memphis.homesec.bootstrap.detail.OwnerImplDetail.start_bt;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getName();
  private static String devId;
  private static String scanCode;

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

    /*scan QR code*/
    UIHelper.registerOnClick(MainActivity.this, R.id.button_scan, new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        scanDeviceQR(v);
      }
    });

    /*bootstrap*/
    UIHelper.registerOnClick(MainActivity.this, R.id.button_bootstrap, new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (m_bt_bound) {

          EditText et_deviceid = (EditText) findViewById(R.id.text_deviceid);
          final String deviceId = et_deviceid.getText().toString();

          EditText et_paircode = (EditText) findViewById(R.id.text_paircode);
          final String pairingCode = et_paircode.getText().toString();

//          final String deviceId  = devId;
//          final String pairingCode = scanCode;

//          EditText et_deviceid = (EditText) findViewById(R.id.text_deviceid);
//          et_deviceid.append(String.format(Locale.ENGLISH, "%s\r\n", deviceId));
//
//          EditText et_paircode = (EditText) findViewById(R.id.text_paircode);
//          et_paircode.append(String.format(Locale.ENGLISH, "%s", pairingCode));

          addLog("Start Bootstrapping for " + deviceId + " with code: " + pairingCode);

          try {
            m_bt_service.startBootStrap(
                    deviceId,
                    pairingCode,
                    devicePairingId -> {
                      addLog("Bootstrap Success : " + devicePairingId);

                      long dif = System.nanoTime()-start_bt;
                      addLog("Excution Time : " + dif );
                      addLog("Excution Time : " + dif/1000000 + "ms");

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

  public void scanDeviceQR(View view) {
    IntentIntegrator scanner = new IntentIntegrator(this);
    // only want QR code scanner
    scanner.setDesiredBarcodeFormats(QR_CODE_TYPES);
    scanner.setOrientationLocked(true);
    // back facing camera id
    scanner.setCameraId(0);
    Intent intent = scanner.createScanIntent();
    startActivityForResult(intent, 0);

  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
      IntentResult result = IntentIntegrator.parseActivityResult(IntentIntegrator.REQUEST_CODE, resultCode, data);
      if (result == null) {
        Toast.makeText(this, "Null", Toast.LENGTH_LONG).show();
      }
      if (result != null) {
        // check the contents and separate into two data
        if (result.getContents() != null) {
          String content = result.getContents();
          String str[] = content.split("\n");
          devId = str[0];
          scanCode = str[1];

          EditText et_deviceid = (EditText) findViewById(R.id.text_deviceid);
          et_deviceid.append(String.format(Locale.ENGLISH, "%s", devId));

          EditText et_paircode = (EditText) findViewById(R.id.text_paircode);
          et_paircode.append(String.format(Locale.ENGLISH, "%s", scanCode));

            //scanCode = content;
          addLog("\nScan DeviceId: " + devId);
          addLog("Scan PairingCode: " + scanCode);
        }
      }
  }

  @Override
  protected void onStart() {
    super.onStart();

//    Intent intent = TemperatureReaderService.newIntent(getApplicationContext());
//    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    Intent intentBt = BootstrapOwnerService.newIntent(getApplicationContext());
    bindService(intentBt, m_bt_service_conn, Context.BIND_AUTO_CREATE);
  }


  public void addLog(String log) {
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