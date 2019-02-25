package ng.com.vela.velasmsapp;

import android.app.Application;

import ng.com.vela.velasms.VelaSMS;

/**
 * @author jerry on 15/12/2018
 * @project VelaSMSApp
 **/
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        VelaSMS.init(
                this,
                BuildConfig.SMS_SHORT_CODE,
                BuildConfig.SHARED_SERVICE_CODE
        );
    }
}
