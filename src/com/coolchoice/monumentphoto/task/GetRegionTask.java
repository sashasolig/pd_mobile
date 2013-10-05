package com.coolchoice.monumentphoto.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
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
import com.coolchoice.monumentphoto.data.BaseDTO;
import com.coolchoice.monumentphoto.data.Cemetery;
import com.coolchoice.monumentphoto.data.Region;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.QueryBuilder;

import android.content.Context;
import android.os.SystemClock;


public class GetRegionTask extends BaseTask {
		   
    public GetRegionTask(AsyncTaskProgressListener pl, AsyncTaskCompleteListener<TaskResult> cb, Context context) {
		super(pl, cb, context);
		this.mTaskName = Settings.TASK_GETREGION;
	}

    @Override
    public void init() {

    }

    @Override
    protected TaskResult doInBackground(String... params) {
    	TaskResult result = new TaskResult();
    	result.setTaskName(Settings.TASK_GETREGION);
    	String resultJSON = null;
        if (params.length == 1) {
            try {
            	initGETQueryParameters(params[0]);
            	resultJSON = getJSON(params[0]);            	
            } catch (AuthorizationException e) {                
                result.setError(true);
                result.setStatus(TaskResult.Status.LOGIN_FAILED);
            }
            catch (Exception e) {                
                result.setError(true);
                result.setStatus(TaskResult.Status.SERVER_UNAVALAIBLE);
            }
            
            if(resultJSON != null){
	            try{	            	
	            	handleResponseGetRegionJSON(resultJSON, this.mCemeteryServerId, this.mLastQueryServerDate);
	            } catch (CancelTaskException cte){
	            	result.setError(true);
	                result.setStatus(TaskResult.Status.CANCEL_TASK);
	            } catch (Exception e) {                
	                result.setError(true);
	                result.setStatus(TaskResult.Status.HANDLE_ERROR);
	            }
            }
        
        }else{
        	throw new IllegalArgumentException("Needs 1 param: url");
        }
        return result;
    }    
}