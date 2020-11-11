package cn.haier.bio.medical.moldincubator;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.lang.ref.WeakReference;

import cn.qd.peiwen.serialport.PWSerialPortHelper;
import cn.qd.peiwen.serialport.PWSerialPortListener;
import cn.qd.peiwen.serialport.PWSerialPortState;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class MoldIncubatorSerialPort implements PWSerialPortListener {
    private ByteBuf buffer;
    private HandlerThread thread;
    private IncubatorHandler handler;
    private PWSerialPortHelper helper;

    private boolean enabled = false;
    private WeakReference<IMoldIncubatorListener> listener;

    public MoldIncubatorSerialPort() {
    }

    public void init(String path) {
        this.createHandler();
        this.createHelper(path);
        this.createBuffer();
    }

    public void enable() {
        if (this.isInitialized() && !this.enabled) {
            this.enabled = true;
            this.helper.open();
        }
    }

    public void disable() {
        if (this.isInitialized() && this.enabled) {
            this.enabled = false;
            this.helper.close();
        }
    }

    public void release() {
        this.listener = null;
        this.destoryHandler();
        this.destoryHelper();
        this.destoryBuffer();
    }

    public void sendData(byte[] data) {
        if (this.isInitialized() && this.enabled) {
            Message msg = Message.obtain();
            msg.what = 0;
            msg.obj = data;
            this.handler.sendMessage(msg);
        }
    }

    public void changeListener(IMoldIncubatorListener listener) {
        this.listener = new WeakReference<>(listener);
    }

    private boolean isInitialized() {
        if (this.handler == null) {
            return false;
        }
        if (this.helper == null) {
            return false;
        }
        if (this.buffer == null) {
            return false;
        }
        return true;
    }

    private void createHelper(String path) {
        if (this.helper == null) {
            this.helper = new PWSerialPortHelper("MoldIncubatorSerialPort");
            this.helper.setTimeout(9);
            this.helper.setPath(path);
            this.helper.setBaudrate(9600);
            this.helper.init(this);
        }
    }

    private void destoryHelper() {
        if (null != this.helper) {
            this.helper.release();
            this.helper = null;
        }
    }

    private void createHandler() {
        if (this.thread == null && this.handler == null) {
            this.thread = new HandlerThread("MoldIncubatorSerialPort");
            this.thread.start();
            this.handler = new IncubatorHandler(this.thread.getLooper());
        }
    }

    private void destoryHandler() {
        if (null != this.thread) {
            this.thread.quitSafely();
            this.thread = null;
            this.handler = null;
        }
    }

    private void createBuffer() {
        if (this.buffer == null) {
            this.buffer = Unpooled.buffer(4);
        }
    }

    private void destoryBuffer() {
        if (null != this.buffer) {
            this.buffer.release();
            this.buffer = null;
        }
    }

    private void write(byte[] data) {
        if (!this.isInitialized() || !this.enabled) {
            return;
        }
        this.helper.writeAndFlush(data);
        MoldIncubatorSerialPort.this.switchReadModel();
        this.loggerPrint("MoldIncubatorSerialPort Send:" + MoldIncubatorTools.bytes2HexString(data, true, ", "));
    }

    public void switchReadModel() {
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onMoldIncubatorSwitchReadModel();
        }
    }

    public void switchWriteModel() {
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onMoldIncubatorSwitchWriteModel();
        }
    }

    private void loggerPrint(String message) {
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onMoldIncubatorPrint(message);
        }
    }

    private boolean ignorePackage() {
        for (byte item : MoldIncubatorTools.COMMANDS) {
            byte[] bytes = new byte[]{MoldIncubatorTools.HEADER, item};
            int index = MoldIncubatorTools.indexOf(this.buffer, bytes);
            if (index != -1) {
                byte[] data = new byte[index];
                this.buffer.readBytes(data, 0, data.length);
                this.buffer.discardReadBytes();
                this.loggerPrint("MoldIncubatorSerialPort 指令丢弃:" + MoldIncubatorTools.bytes2HexString(data, true, ", "));
                return true;
            }
        }
        return false;
    }


    @Override
    public void onConnected(PWSerialPortHelper helper) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        this.buffer.clear();
        this.switchWriteModel();
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onMoldIncubatorConnected();
        }
    }

    @Override
    public void onReadThreadReleased(PWSerialPortHelper helper) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onMoldIncubatorPrint("MoldIncubatorSerialPort read thread released");
        }
    }

    @Override
    public void onException(PWSerialPortHelper helper, Throwable throwable) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onMoldIncubatorException(throwable);
        }
    }

    @Override
    public void onStateChanged(PWSerialPortHelper helper, PWSerialPortState state) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onMoldIncubatorPrint("MoldIncubatorSerialPort state changed: " + state.name());
        }
    }

    @Override
    public void onByteReceived(PWSerialPortHelper helper, byte[] buffer, int length) throws IOException {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        this.buffer.writeBytes(buffer, 0, length);

        while (this.buffer.readableBytes() >= 3) {
            byte header = this.buffer.getByte(0);
            byte command = this.buffer.getByte(1);

            if (!MoldIncubatorTools.checkHeader(header) || !MoldIncubatorTools.checkCommand(command)) {
                if (this.ignorePackage()) {
                    continue;
                } else {
                    break;
                }
            }

            int lenth = 0;
            if(command == 0x10) {
                lenth = 8;
            } else {
                lenth = 5 + (0xFF & this.buffer.getByte(2));
            }
            if (this.buffer.readableBytes() < lenth) {
                break;
            }
            byte[] data = new byte[lenth];
            this.buffer.readBytes(data, 0, data.length);
            this.buffer.discardReadBytes();
            this.loggerPrint("MoldIncubatorSerialPort Recv:" + MoldIncubatorTools.bytes2HexString(data, true, ", "));
            this.switchWriteModel();
            if (null != this.listener && null != this.listener.get()) {
                this.listener.get().onMoldIncubatorPackageReceived(data);
            }
        }
    }

    private class IncubatorHandler extends Handler {
        public IncubatorHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0: {
                    byte[] message = (byte[]) msg.obj;
                    if (null != message && message.length > 0) {
                        MoldIncubatorSerialPort.this.write(message);
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }
}
