package cn.haier.bio.medical.moldincubator;

public interface IMoldIncubatorListener {
    void onMoldIncubatorConnected();
    void onMoldIncubatorSwitchReadModel();
    void onMoldIncubatorSwitchWriteModel();
    void onMoldIncubatorPrint(String message);
    void onMoldIncubatorException(Throwable throwable);
    void onMoldIncubatorPackageReceived(byte[] message);
}
