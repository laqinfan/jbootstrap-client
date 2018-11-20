package edu.memphis.netlab.homeclient;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.memphis.homesec.bootstrap.BootstrapException;
import edu.memphis.homesec.bootstrap.Configuration;
import edu.memphis.homesec.bootstrap.DefaultDeviceNameGenerator;
import edu.memphis.homesec.bootstrap.Owner;
import edu.memphis.homesec.bootstrap.OwnerImpl;
import edu.memphis.netlab.homeclient.node.NodeService;

import static edu.memphis.netlab.homeclient.Global.SCHEDULED_EXECUTOR_SERVICE;

public class BootstrapOwnerService extends NodeService {
    private static final Logger logger = LoggerFactory.getLogger(BootstrapOwnerService.class);

    public void startBootStrap(String pairingId,
                               String pairingKey,
                               Owner.OnSuccess onSuccess,
                               Owner.OnFail onFail) {
        Configuration cfg = new Configuration();
        cfg.setDevicePairingId(pairingId);
        cfg.setDevicePairingCode(pairingKey);
        cfg.setNameGenerator(new DefaultDeviceNameGenerator());
        cfg.setNode(this.m_node);
        cfg.setNdnKeyChain(this.m_node.getKeyChain());
        BtTask btTask = new BtTask(cfg, onSuccess, onFail);
        SCHEDULED_EXECUTOR_SERVICE.submit(btTask);
        logger.debug("submitted bttask for device {}", cfg.getDevicePairingId());
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
            startService(TemperatureReaderService.newIntent(this));
        }
        m_is_running = true;
        return m_localBinder;
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
