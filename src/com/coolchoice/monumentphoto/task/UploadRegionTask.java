package com.coolchoice.monumentphoto.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.coolchoice.monumentphoto.Settings;
import com.coolchoice.monumentphoto.dal.DB;
import com.coolchoice.monumentphoto.data.Cemetery;
import com.coolchoice.monumentphoto.data.Region;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.QueryBuilder;

import android.content.Context;
import android.os.SystemClock;


public class UploadRegionTask extends BaseTask {
		   
    public UploadRegionTask(AsyncTaskProgressListener pl, AsyncTaskCompleteListener<TaskResult> cb, Context context) {
		super(pl, cb, context);
		this.mTaskName = Settings.TASK_POSTREGION;
	}

    @Override
    public void init() {

    }

    @Override
    protected TaskResult doInBackground(String... params) {
    	TaskResult result = new TaskResult();
    	result.setTaskName(Settings.TASK_POSTREGION);
    	if (params.length == 1) {        	
        	List<Region> regionList = DB.dao(Region.class).queryForEq("IsChanged", 1);
        	for(Region region : regionList){
    			DB.dao(Cemetery.class).refresh(region.Cemetery);
    		}
        	int successCount = 0;
        	int processedCount = 0;
        	result.setUploadCount(regionList.size());
        	boolean isSuccessUpload = false;
        	for(Region region : regionList){
        		if(region.Cemetery == null) continue;
        		processedCount++;
        		isSuccessUpload = false;
	            try {
	            	Dictionary<String, String> dictPostData = new Hashtable<String, String>();
	            	dictPostData.put("cemeteryId", Integer.toString(region.Cemetery.ServerId));
	            	dictPostData.put("areaId", Integer.toString(region.ServerId));
	            	dictPostData.put("areaName", region.Name);
	            	String responseString = postData(params[0], dictPostData);
	            	if(responseString != null){
	            		handleResponseUploadRegionJSON(responseString);       		
	            	} else{
	            		result.setError(true);
	            		result.setStatus(TaskResult.Status.HANDLE_ERROR);
	            	}
	            	successCount++;
	            	isSuccessUpload = true;
	            } catch (AuthorizationException e) {                
	                result.setError(true);
	                result.setStatus(TaskResult.Status.LOGIN_FAILED);
	            }
	            catch (Exception e) {                
	                result.setError(true);
	                result.setStatus(TaskResult.Status.HANDLE_ERROR);
	            }
	            if(isSuccessUpload){
		            DB.dao(Region.class).refresh(region);
		            region.IsChanged = 0;
		            DB.dao(Region.class).update(region);
	            }
	            result.setUploadCountSuccess(successCount);
	            result.setUploadCountError(processedCount - successCount);
	            publishUploadProgress("Отправлено участков: %d  из %d...", result);
        	}
        }else{
        	throw new IllegalArgumentException("Needs 1 param: url");
        }
        return result;
    }
    
}