package com.enginecal.kioskapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

public class BatteryReceiver extends BroadcastReceiver {

    MainActivity mainActivity;
    BatteryStateReceiver receiverInterfaceone;

    public BatteryReceiver( MainActivity mainActivity,BatteryStateReceiver receiverInterface){
        this.mainActivity = mainActivity;
        this.receiverInterfaceone = receiverInterface;

    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            if (intent.getAction() == Intent.ACTION_POWER_CONNECTED ) {
                receiverInterfaceone.batteryStatus(true);
            } else if (intent.getAction() == Intent.ACTION_POWER_DISCONNECTED) {
                receiverInterfaceone.batteryStatus(false);
            }
        }
    }
}
