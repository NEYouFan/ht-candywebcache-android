package com.netease.hearttouch.htfiledownloader;

/**
 * Created by DING on 16/6/4.
 */
public class ProgressInfo {
    private float percent;
    private long transferredSize;
    private long totalSize;
    private float transferSpeed;

    public ProgressInfo() {
    }

    public ProgressInfo(ProgressInfo progressInfo) {
        copyFrom(progressInfo);
    }

    void reset() {
        percent = 0f;
        transferredSize = 0;
        totalSize = 0;
        transferSpeed = 0f;
    }


    void copyFrom(ProgressInfo progressInfo) {
        percent = progressInfo.percent;
        transferredSize = progressInfo.transferredSize;
        totalSize = progressInfo.totalSize;
        transferSpeed = progressInfo.transferSpeed;
    }

    public float getPercent() {
        return percent;
    }

    void setPercent(float percent) {
        this.percent = percent;
    }

    public long getTransferredSize() {
        return transferredSize;
    }

    void setTransferredSize(long transferredSize) {
        this.transferredSize = transferredSize;
    }

    public long getTotalSize() {
        return totalSize;
    }

    void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public float getTransferSpeed() {
        return transferSpeed;
    }

    void setTransferSpeed(float transferSpeed) {
        this.transferSpeed = transferSpeed;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("percent:").append(percent)
                .append(",transferred_size:").append(transferredSize)
                .append(",total_size:").append(totalSize)
                .append(",speed:").append(transferSpeed);
        return sb.toString();
    }
}
