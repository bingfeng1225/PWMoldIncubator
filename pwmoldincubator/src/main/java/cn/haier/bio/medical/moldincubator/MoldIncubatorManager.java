package cn.haier.bio.medical.moldincubator;

/***
 * 超低温变频、T系列、双系统主控板通讯
 *
 */
public class MoldIncubatorManager {
    private MoldIncubatorSerialPort serialPort;
    private static MoldIncubatorManager manager;

    public static MoldIncubatorManager getInstance() {
        if (manager == null) {
            synchronized (MoldIncubatorManager.class) {
                if (manager == null)
                    manager = new MoldIncubatorManager();
            }
        }
        return manager;
    }

    private MoldIncubatorManager() {

    }

    public void init(String path) {
        if (this.serialPort == null) {
            this.serialPort = new MoldIncubatorSerialPort();
            this.serialPort.init(path);
        }
    }

    public void enable() {
        if (null != this.serialPort) {
            this.serialPort.enable();
        }
    }

    public void disable() {
        if (null != this.serialPort) {
            this.serialPort.disable();
        }
    }

    public void release() {
        if (null != this.serialPort) {
            this.serialPort.release();
            this.serialPort = null;
        }
    }

    public void sendData(byte[] data) {
        if (null != this.serialPort) {
            this.serialPort.sendData(data);
        }
    }

    public void changeListener(IMoldIncubatorListener listener) {
        if (null != this.serialPort) {
            this.serialPort.changeListener(listener);
        }
    }
}

