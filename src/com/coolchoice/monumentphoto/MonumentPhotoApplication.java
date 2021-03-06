package com.coolchoice.monumentphoto;

import java.io.File;
import java.io.IOException;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

import com.coolchoice.monumentphoto.dal.DB;


@ReportsCrashes(formKey = "", mailTo = "pdmobile.developer.by@gmail.com", 
mode = ReportingInteractionMode.DIALOG,
resToastText = R.string.crash_toast_text,
resDialogText = R.string.crash_dialog_text,
resDialogIcon = android.R.drawable.ic_dialog_info,
resDialogTitle = R.string.crash_dialog_title,
resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
resDialogOkToast = R.string.crash_dialog_ok_toast,
customReportContent = { ReportField.USER_COMMENT, ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.DEVICE_ID, ReportField.DEVICE_FEATURES,
    ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT, 
    ReportField.APPLICATION_LOG, ReportField.SHARED_PREFERENCES},
    applicationLogFileLines = 1000,
    additionalSharedPreferences={Settings.PREFS_NAME})
public class MonumentPhotoApplication extends Application {

    @Override
    public void onCreate() {
        ConfigureLog4J.configure();
        initAcra();        
    	Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());    	
        super.onCreate();
        DB.setContext(getApplicationContext());        
        createNoMediaFile();      
    }

    @Override
    public void onTerminate() {
    	DB.release();
        super.onTerminate();
    }
    
    private void createNoMediaFile(){
        File rootDirPhoto = Settings.getRootDirPhoto();
        if(rootDirPhoto != null && rootDirPhoto.exists()){
            File noMediaFile = new File(rootDirPhoto, Settings.NO_MEDIA_FILENAME);
            if(noMediaFile != null && !noMediaFile.exists()){
                try {
                    noMediaFile.createNewFile();
                } catch (IOException e) {
                    //do nothing
                }
            }
        } 
    }
    
    private void initAcra(){
        ACRA.init(this);
        String applicationLogFilePath = Settings.getLogFilePath();
        ACRA.getErrorReporter().checkReportsOnApplicationStart();
        ACRA.getConfig().setApplicationLogFile(applicationLogFilePath);
    }
}

