package com.pvphelper;

public final class PvpHelperConfig {
    public boolean highlightEnabled = true;
    public boolean persistentHighlightEnabled = true;
    public boolean arrowPredictionEnabled = true;
    public boolean arrowWarningEnabled = true;
    public boolean aimPredictionEnabled = true;
    public boolean hudEnabled = true;
    public boolean hudShowArrowCount = true;
    public boolean hudShowAimInfo = true;
    public int hudX = 8;
    public int hudY = 8;

    public PvpHelperConfig copy() {
        PvpHelperConfig copy = new PvpHelperConfig();
        copy.highlightEnabled = this.highlightEnabled;
        copy.persistentHighlightEnabled = this.persistentHighlightEnabled;
        copy.arrowPredictionEnabled = this.arrowPredictionEnabled;
        copy.arrowWarningEnabled = this.arrowWarningEnabled;
        copy.aimPredictionEnabled = this.aimPredictionEnabled;
        copy.hudEnabled = this.hudEnabled;
        copy.hudShowArrowCount = this.hudShowArrowCount;
        copy.hudShowAimInfo = this.hudShowAimInfo;
        copy.hudX = this.hudX;
        copy.hudY = this.hudY;
        return copy;
    }

    public void clamp() {
        hudX = Math.max(0, hudX);
        hudY = Math.max(0, hudY);
    }
}
