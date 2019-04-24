/*
 * Copyright (C) 2018-2019 Lei Pi, Laqin Fan
 */

package edu.memphis.netlab.homeclient;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import net.named_data.jndn.Name;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.pib.Pib;
import net.named_data.jndn.security.pib.PibIdentity;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.tpm.Tpm;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.security.v2.CertificateV2;
import net.named_data.jndn.security.v2.TrustAnchorContainer;
import net.named_data.jndn.util.Common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import edu.memphis.homesec.bootstrap.BootstrapException;
import edu.memphis.homesec.bootstrap.Configuration;
import edu.memphis.homesec.bootstrap.DefaultDeviceNameGenerator;
import edu.memphis.homesec.bootstrap.Owner;
import edu.memphis.homesec.bootstrap.OwnerImpl;
import edu.memphis.netlab.homeclient.node.NodeService;

import static edu.memphis.netlab.homeclient.Global.SCHEDULED_EXECUTOR_SERVICE;

public class BootstrapOwnerService extends NodeService {
    private static final Logger logger = LoggerFactory.getLogger(BootstrapOwnerService.class);

    Name ownerNameSpace = new Name(Owner.DEFAULT_PREFIX + "/owner");

    public static File certFile = null;
    public static CertificateV2 ownerCert = null;


    public void startBootStrap(String pairingId,
                               String pairingKey,
                               Owner.OnSuccess onSuccess,
                               Owner.OnFail onFail) throws IOException {
        Configuration cfg = new Configuration();
        cfg.setDevicePairingId(pairingId);
        cfg.setDevicePairingCode(pairingKey);
        cfg.setNameGenerator(new DefaultDeviceNameGenerator());
        cfg.setNode(this.m_node);
        cfg.setNdnKeyChain(this.m_node.getKeyChain());

        BtTask btTask = new BtTask(cfg, onSuccess, onFail);
        SCHEDULED_EXECUTOR_SERVICE.submit(btTask);
        logger.debug("submitted BootstrapTask for device {}", cfg.getDevicePairingId());
    }

    private class BtTask implements Runnable {
        Configuration config;
        Owner.OnSuccess onSuccess;
        Owner.OnFail onFail;

        BtTask(Configuration config, Owner.OnSuccess onSuccess, Owner.OnFail onFail) {
            this.config = config;
            this.onSuccess = onSuccess;
            this.onFail = onFail;
        }

        @Override
        public void run() {
            Owner owner = new OwnerImpl();
            try {
                Log.d("Start bootstarp", "bt...");
                owner.start(config, onSuccess, onFail);
            } catch (BootstrapException e) {
                logger.error("bootstrap failed for device [{}]", config.getDevicePairingId(), e);
                this.onFail.onFail(config.getDevicePairingId(), e.getMessage());
            }
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
            startService(BootstrapOwnerService.newIntent(this));
        }
        m_is_running = true;
        return m_localBinder;
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, BootstrapOwnerService.class);
    }

    public class LocalBinder extends Binder {
        public BootstrapOwnerService getService() {
            return BootstrapOwnerService.this;
        }
    }

    ////////////////////////////////////////////////////////

    private static boolean m_is_running = false;

    private IBinder m_localBinder = new BootstrapOwnerService.LocalBinder();

}
