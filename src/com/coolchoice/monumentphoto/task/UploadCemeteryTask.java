package com.coolchoice.monumentphoto.task;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import android.content.Context;

import com.coolchoice.monumentphoto.Settings;
import com.coolchoice.monumentphoto.dal.DB;
import com.coolchoice.monumentphoto.data.BaseDTO;
import com.coolchoice.monumentphoto.data.Cemetery;
import com.coolchoice.monumentphoto.data.GPSCemetery;


public class UploadCemeteryTask extends BaseTask {
		   
    public UploadCemeteryTask(AsyncTaskProgressListener pl, AsyncTaskCompleteListener<TaskResult> cb, Context context) {
		super(pl, cb, context);
		this.mTaskName = Settings.TASK_POSTCEMETERY;
	}

    @Override
    public void init() {

    }

    @Override
    protected TaskResult doInBackground(String... params) {
    	TaskResult result = new TaskResult();
    	result.setTaskName(Settings.TASK_POSTCEMETERY);
    	result.setStatus(TaskResult.Status.OK);    	   	
    	List<Cemetery> cemeteryList = DB.dao(Cemetery.class).queryForEq(BaseDTO.COLUMN_IS_CHANGED, 1);
    	result.setUploadCount(cemeteryList.size());
    	int successCount = 0;
    	int processedCount = 0;
    	boolean isSuccessUpload = false;
    	for(Cemetery cem : cemeteryList){        		
    		isSuccessUpload = false;
    		processedCount++;
            try {
            	checkIsCancelTask();
            	String gpsJSON = "";
            	if(cem.IsGPSChanged != 0) {
            	    for(GPSCemetery gps : cem.GPSCemeteryList){
                        DB.dao(GPSCemetery.class).refresh(gps);
                    }
            	    gpsJSON = createGPSCemeteryJSON(cem);
            	}            	
            	Dictionary<String, String> dictPostData = new Hashtable<String, String>();
            	dictPostData.put("cemeteryId", Integer.toString(cem.ServerId));
            	dictPostData.put("cemeteryName", cem.Name);
            	dictPostData.put("square", cem.Square != null ? Double.toString(cem.Square) : "");
            	dictPostData.put("gps", gpsJSON);
            	String responseString = postData(params[0], dictPostData);
            	if(responseString != null){
            		handleResponseUploadCemeteryJSON(responseString);            		
            	} else{
            		result.setError(true);
            		result.setStatus(TaskResult.Status.HANDLE_ERROR);
            	}
            	successCount++;
            	isSuccessUpload = true;
            } catch (AuthorizationException e) {                
                result.setError(true);
                result.setStatus(TaskResult.Status.LOGIN_FAILED);
            } catch (CancelTaskException cte){
            	result.setError(true);
                result.setStatus(TaskResult.Status.CANCEL_TASK);
            } catch (Exception e) {                
                result.setError(true);
                result.setStatus(TaskResult.Status.HANDLE_ERROR);
            }
            if(isSuccessUpload){
	            DB.dao(Cemetery.class).refresh(cem);
	            cem.IsChanged = 0;
	            cem.IsGPSChanged = 0;
	            DB.dao(Cemetery.class).update(cem);
            }
            result.setUploadCountSuccess(successCount);
            result.setUploadCountError(processedCount - successCount);
            publishUploadProgress("Отправлено кладбищ: %d  из %d...", result);
    	}
        return result;
    }
    
}