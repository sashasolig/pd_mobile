package com.coolchoice.monumentphoto.task;

import java.sql.SQLException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import android.content.Context;

import com.coolchoice.monumentphoto.Settings;
import com.coolchoice.monumentphoto.dal.DB;
import com.coolchoice.monumentphoto.data.BaseDTO;
import com.coolchoice.monumentphoto.data.Place;
import com.coolchoice.monumentphoto.data.Region;
import com.coolchoice.monumentphoto.data.Row;
import com.j256.ormlite.stmt.QueryBuilder;


public class UploadPlaceTask extends BaseTask {
		   
    public UploadPlaceTask(AsyncTaskProgressListener pl, AsyncTaskCompleteListener<TaskResult> cb, Context context) {
		super(pl, cb, context);
		this.mTaskName = Settings.TASK_POSTPLACE;
	}

    @Override
    public void init() {

    }

    @Override
    protected TaskResult doInBackground(String... params) {
    	TaskResult result = new TaskResult();
    	result.setTaskName(Settings.TASK_POSTPLACE);
    	result.setStatus(TaskResult.Status.OK);
    	List<Place> placeList = DB.dao(Place.class).queryForEq(BaseDTO.COLUMN_IS_CHANGED, 1);    	
    	List<Row> rowList = DB.dao(Row.class).queryForEq(BaseDTO.COLUMN_IS_CHANGED, 1);
    	for(Row row : rowList){
    		QueryBuilder<Place, Integer> builder = DB.dao(Place.class).queryBuilder();
			try {
				builder.where().eq(Place.ROW_ID_COLUMN, row.Id).and().eq("IsChanged", 0);
				List<Place> findedPlaces = DB.dao(Place.class).query(builder.prepare());
    			if(findedPlaces.size() > 0){
    				placeList.addAll(findedPlaces);
    			}
			} catch (SQLException e) {					
				e.printStackTrace();
			}
			
    	}
    	int successCount = 0;
    	int processedCount = 0;
    	result.setUploadCount(placeList.size());
    	boolean isSuccessUpload = false;
		for(Place place : placeList){
			if(place.Row == null && place.Region == null) continue;
			isSuccessUpload = false;
			processedCount++;
			String rowName = "";
			int regionServerId;
			if(place.Row != null){
				DB.dao(Row.class).refresh(place.Row);
				if(place.Row.Region == null) continue;
				DB.dao(Region.class).refresh(place.Row.Region);    				
				rowName = place.Row.Name;
				regionServerId = place.Row.Region.ServerId;
			} else {
				DB.dao(Region.class).refresh(place.Region);    				
				regionServerId = place.Region.ServerId;   				
			}
			try {
				checkIsCancelTask();
				Dictionary<String, String> dictPostData = new Hashtable<String, String>();
            	dictPostData.put("areaId", Integer.toString(regionServerId));
            	dictPostData.put("placeId", Integer.toString(place.ServerId));
            	dictPostData.put("rowName", rowName);
            	dictPostData.put("placeName", place.Name);
            	if(place.OldName != null){
            		dictPostData.put("oldPlaceName", place.OldName);
            	} else {
            		dictPostData.put("oldPlaceName", "");
            	}
            	if(place.Length != null){
            		dictPostData.put("placeLength", Double.toString(place.Length));
            	} else {
            		dictPostData.put("placeLength", "");
            	}
            	if(place.Width != null){
            		dictPostData.put("placeWidth", Double.toString(place.Width));
            	} else {
            		dictPostData.put("placeWidth", "");
            	}            	
            	String dtWrongFio = place.isWrongFIO() ? this.serializeDate(place.WrongFIODate) : "";
            	String dtMilitary = place.isMilitary() ? this.serializeDate(place.MilitaryDate) : "";
            	String dtSizeViolated = place.isSizeViolated() ? this.serializeDate(place.SizeViolatedDate) : "";
            	String dtUnowned = place.isUnowned() ? this.serializeDate(place.UnownedDate) : "";
            	String dtUnindentified = place.isUnindentified() ? this.serializeDate(place.UnindentifiedDate) : "";
            	dictPostData.put("dtWrongFio", dtWrongFio);
            	dictPostData.put("dtMilitary", dtMilitary);
            	dictPostData.put("dtSizeViolated", dtSizeViolated);
            	dictPostData.put("dtUnowned", dtUnowned);
            	dictPostData.put("dtUnindentified", dtUnindentified);            	
            	String responseString = postData(params[0], dictPostData);
            	if(responseString != null){
            		handleResponseUploadPlaceJSON(responseString);                		
            	} else {
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
    			DB.dao(Place.class).refresh(place);
    			if(place.Row != null){
    				DB.dao(Row.class).refresh(place.Row);
    				place.Row.IsChanged = 0;
    				DB.dao(Row.class).update(place.Row);
    			}
    			place.IsChanged = 0;
    			DB.dao(Place.class).update(place);
			}
			result.setUploadCountSuccess(successCount);
            result.setUploadCountError(processedCount - successCount);
            publishUploadProgress("Отправлено мест: %d из %d...", result);
		}        
        return result;
    }
    
}
