package com.coolchoice.monumentphoto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import com.coolchoice.monumentphoto.dal.DB;
import com.coolchoice.monumentphoto.data.BaseDTO;
import com.coolchoice.monumentphoto.data.Burial;
import com.coolchoice.monumentphoto.data.Cemetery;
import com.coolchoice.monumentphoto.data.Region;
import com.coolchoice.monumentphoto.data.SettingsData;
import com.coolchoice.monumentphoto.task.AsyncTaskCompleteListener;
import com.coolchoice.monumentphoto.task.AsyncTaskProgressListener;
import com.coolchoice.monumentphoto.task.BaseTask;
import com.coolchoice.monumentphoto.task.GetBurialTask;
import com.coolchoice.monumentphoto.task.GetCemeteryTask;
import com.coolchoice.monumentphoto.task.GetGraveTask;
import com.coolchoice.monumentphoto.task.GetPlacePhotoTask;
import com.coolchoice.monumentphoto.task.GetPlaceTask;
import com.coolchoice.monumentphoto.task.GetRegionTask;
import com.coolchoice.monumentphoto.task.LoginTask;
import com.coolchoice.monumentphoto.task.RemovePhotoTask;
import com.coolchoice.monumentphoto.task.TaskResult;
import com.coolchoice.monumentphoto.task.TaskResult.Status;
import com.coolchoice.monumentphoto.task.UploadBurialTask;
import com.coolchoice.monumentphoto.task.UploadCemeteryTask;
import com.coolchoice.monumentphoto.task.UploadGraveTask;
import com.coolchoice.monumentphoto.task.UploadPhotoTask;
import com.coolchoice.monumentphoto.task.UploadPlaceTask;
import com.coolchoice.monumentphoto.task.UploadRegionTask;

class SyncTaskHandler implements AsyncTaskCompleteListener<TaskResult>, AsyncTaskProgressListener{

	public interface SyncCompleteListener {
		public void onComplete(OperationType operationType, TaskResult taskResult);
	}
	
	public enum OperationType { NOTHING, CHECK_LOGIN, GET_DATA, UPLOAD_DATA, GET_CHANGED_DATA, GET_CHANGED_DATA_ONE_CEMETERY};
	
	private Context mContext;
	private ProgressDialog mProgressDialogSyncData;
	private ProgressDialog mProgressDialogCancelQuestion;
	private ProgressDialog mProgressDialogCancelInfo;
	private OperationType mOperationType = OperationType.NOTHING;
	
	private String mProgressDialogTitle;
	private String mProgressDialogMessage;
	
	private ArrayList<TaskResult> mUploadTaskResults = new ArrayList<TaskResult>();
	private ArrayList<BaseTask> mTasks = new ArrayList<BaseTask>();
	private BaseTask mCurrentExecutedTask = null;
	private int mCurrentTaskIndex = -1;
	private int mCemeteryServerId, mRegionServerId, mPlaceServerId, mGraveServerId;
	private Burial.StatusEnum mBurialStatus = null;
	private Date mSyncDate;
	private boolean mIsStartExecuteNextTask = true;
	private boolean mIsInteruptExecutedTask = false;
	
	private List<Integer> mCemeteryServerIds;
	private List<Integer> mRegionServerIds;
	private int mCurrentCemeteryIdsIndex;
	private int mCurrentRegionIdsIndex;
	
	private final static String PD_GETCEMETERY_MESSAGE = "Получение кладбищ";
	private final static String PD_GETREGION_MESSAGE = "Получение участков";
	private final static String PD_GETPLACE_MESSAGE = "Получение мест";
	private final static String PD_GETPLACEPHOTO_MESSAGE = "Получение фотографий мест";
	private final static String PD_GETGRAVE_MESSAGE = "Получение могил";
	private final static String PD_GETBURIAL_MESSAGE = "Получение захоронений";
	
	private final static String HANDLE_ERROR_MESSAGE = "Ошибка обработки данных.";
	private final static String LOGIN_FAILED_ERROR_MESSAGE = "Ошибка авторизации на сервере.";
	private final static String SERVER_UNAVALAIBLE_ERROR_MESSAGE = "Сервер не доступен.";
	
	protected final Logger mFileLog = Logger.getLogger(SyncTaskHandler.class);
	
	
	private SyncCompleteListener onSyncCompleteListener;
	
	public void setOnSyncCompleteListener(SyncCompleteListener syncCompleteListener){
		this.onSyncCompleteListener = syncCompleteListener;
	}
	
	public SyncTaskHandler(){		
	}	
	
	private void setEmptyServerId(){
		this.mCemeteryServerId = Integer.MIN_VALUE;
		this.mRegionServerId = Integer.MIN_VALUE;
		this.mPlaceServerId = Integer.MIN_VALUE;
		this.mGraveServerId = Integer.MIN_VALUE;
		this.mBurialStatus = null;
		this.mSyncDate = null;
		
		this.mCurrentCemeteryIdsIndex = -1;
		this.mCurrentRegionIdsIndex = -1;
		this.mCemeteryServerIds = new ArrayList<Integer>();
		this.mRegionServerIds = new ArrayList<Integer>();
		this.mCurrentExecutedTask = null;
		this.mIsStartExecuteNextTask = true;
		this.mIsInteruptExecutedTask = false;
		
		this.mProgressDialogSyncData = null;
		this.mProgressDialogCancelQuestion = null;
		this.mProgressDialogCancelInfo = null;
	}	
	
	public void setContext(Context context){
		this.mContext = context;
	}
	
	public void startGetCemetery(){		
		if(mOperationType != OperationType.NOTHING){
			return;
		}
		setEmptyServerId();
		mOperationType = OperationType.GET_DATA;		
		mTasks.clear();
		mTasks.add(new GetCemeteryTask(this, this, this.mContext));
		mCurrentTaskIndex = -1;
		mProgressDialogTitle = "Загрузка данных...";
		mProgressDialogMessage = "Подождите";
		showOperationInfo();
		SettingsData settingsData = Settings.getSettingData(this.mContext);
		LoginTask loginTask = new LoginTask(this, this, this.mContext);
		loginTask.execute(Settings.getLoginUrl(mContext), settingsData.Login, settingsData.Password);
		this.mCurrentExecutedTask = loginTask;
	}
	
	public void startGetRegion(int cemeteryServerId){
		if(mOperationType != OperationType.NOTHING){
			return;
		}
		setEmptyServerId();
		this.mCemeteryServerId = cemeteryServerId;
		mOperationType = OperationType.GET_DATA;
		mTasks.clear();
		mTasks.add(new GetRegionTask(this, this, this.mContext));
		mCurrentTaskIndex = -1;
		mProgressDialogTitle = "Загрузка данных...";
		mProgressDialogMessage = "Подождите";
		showOperationInfo();
		SettingsData settingsData = Settings.getSettingData(this.mContext);
		LoginTask loginTask = new LoginTask(this, this, this.mContext);
		loginTask.execute(Settings.getLoginUrl(mContext), settingsData.Login, settingsData.Password);
		this.mCurrentExecutedTask = loginTask;
	}
	
	public void startGetPlace(int regionServerId){
		if(mOperationType != OperationType.NOTHING){
			return;
		}
		setEmptyServerId();
		this.mRegionServerId = regionServerId;
		mOperationType = OperationType.GET_DATA;
		mTasks.clear();
		mTasks.add(new GetPlaceTask(this, this, this.mContext));		
		mCurrentTaskIndex = -1;
		mProgressDialogTitle = "Загрузка данных...";
		mProgressDialogMessage = "Подождите";
		showOperationInfo();
		SettingsData settingsData = Settings.getSettingData(this.mContext);
		LoginTask loginTask = new LoginTask(this, this, this.mContext);
		loginTask.execute(Settings.getLoginUrl(mContext), settingsData.Login, settingsData.Password);
		this.mCurrentExecutedTask = loginTask;
	}
	
	public void startGetPlaceAndGraveAndBurial(int regionServerId){
		if(mOperationType != OperationType.NOTHING){
			return;
		}
		setEmptyServerId();
		this.mRegionServerId = regionServerId;
		mOperationType = OperationType.GET_DATA;
		mTasks.clear();
		mTasks.add(new GetPlaceTask(this, this, this.mContext));
		mTasks.add(new GetGraveTask(this, this, this.mContext));
		mTasks.add(new GetBurialTask(this, this, this.mContext));
		mCurrentTaskIndex = -1;
		mProgressDialogTitle = "Загрузка данных...";
		mProgressDialogMessage = "Подождите";
		showOperationInfo();
		SettingsData settingsData = Settings.getSettingData(this.mContext);
		LoginTask loginTask = new LoginTask(this, this, this.mContext);
		loginTask.execute(Settings.getLoginUrl(mContext), settingsData.Login, settingsData.Password);
		this.mCurrentExecutedTask = loginTask;
	}
	
	public void startGetGrave(int placeServerId){
		if(mOperationType != OperationType.NOTHING){
			return;
		}
		setEmptyServerId();
		this.mPlaceServerId = placeServerId;		
		mOperationType = OperationType.GET_DATA;
		mTasks.clear();
		mTasks.add(new GetGraveTask(this, this, this.mContext));
		mTasks.add(new GetPlacePhotoTask(this, this, this.mContext));
		mCurrentTaskIndex = -1;
		mProgressDialogTitle = "Загрузка данных...";
		mProgressDialogMessage = "Подождите";
		showOperationInfo();
		SettingsData settingsData = Settings.getSettingData(this.mContext);
		LoginTask loginTask = new LoginTask(this, this, this.mContext);
		loginTask.execute(Settings.getLoginUrl(mContext), settingsData.Login, settingsData.Password);
		this.mCurrentExecutedTask = loginTask;
	}
	
	public void startGetBurial(int graveServerId){
		if(mOperationType != OperationType.NOTHING){
			return;
		}
		setEmptyServerId();
		this.mGraveServerId = graveServerId;		
		mOperationType = OperationType.GET_DATA;
		mTasks.clear();
		mTasks.add(new GetBurialTask(this, this, this.mContext));
		mCurrentTaskIndex = -1;
		mProgressDialogTitle = "Загрузка данных...";
		mProgressDialogMessage = "Подождите";
		showOperationInfo();
		SettingsData settingsData = Settings.getSettingData(this.mContext);
		LoginTask loginTask = new LoginTask(this, this, this.mContext);
		loginTask.execute(Settings.getLoginUrl(mContext), settingsData.Login, settingsData.Password);
		this.mCurrentExecutedTask = loginTask;
	}
	
	public void startGetApprovedBurial(){
        if(mOperationType != OperationType.NOTHING){
            return;
        }
        setEmptyServerId();
        this.mBurialStatus = Burial.StatusEnum.APPROVED;       
        mOperationType = OperationType.GET_DATA;
        mTasks.clear();
        mTasks.add(new GetBurialTask(this, this, this.mContext));
        mCurrentTaskIndex = -1;
        mProgressDialogTitle = "Загрузка данных...";
        mProgressDialogMessage = "Подождите";
        showOperationInfo();
        SettingsData settingsData = Settings.getSettingData(this.mContext);
        LoginTask loginTask = new LoginTask(this, this, this.mContext);
        loginTask.execute(Settings.getLoginUrl(mContext), settingsData.Login, settingsData.Password);
        this.mCurrentExecutedTask = loginTask;
    }
	
	public void startCheckLogin(){
		if(mOperationType != OperationType.NOTHING){
			Toast.makeText(this.mContext, "Идет завершение предыдущей операции", Toast.LENGTH_LONG).show();
			return;
		}
		setEmptyServerId();		
		mOperationType = OperationType.CHECK_LOGIN;
		mTasks.clear();
		mCurrentTaskIndex = -1;
		mProgressDialogTitle = "Проверка доступа к серверу";
		mProgressDialogMessage = "Авторизация";
		mProgressDialogSyncData = ProgressDialog.show(this.mContext, mProgressDialogTitle, mProgressDialogMessage, true);
		mProgressDialogSyncData.setCancelable(false);
		SettingsData settingsData = Settings.getSettingData(this.mContext);
		LoginTask loginTask = new LoginTask(this, this, this.mContext);
		loginTask.execute(Settings.getLoginUrl(mContext), settingsData.Login, settingsData.Password);
		this.mCurrentExecutedTask = loginTask;
	}
	
	public void startGetData(){
		if(mOperationType != OperationType.NOTHING){
			Toast.makeText(this.mContext, "Идет завершение предыдущей операции", Toast.LENGTH_LONG).show();
			return;
		}
		setEmptyServerId();		
		mOperationType = OperationType.GET_DATA;
		mTasks.clear();
		mTasks.add(new GetCemeteryTask(this, this, this.mContext));
		mTasks.add(new GetRegionTask(this, this, this.mContext));
		mTasks.add(new GetPlaceTask(this, this, this.mContext));
		mTasks.add(new GetGraveTask(this, this, this.mContext));
		mTasks.add(new GetBurialTask(this, this, this.mContext));
		mCurrentTaskIndex = -1;
		mProgressDialogTitle = "Загрузка данных...";
		mProgressDialogMessage = "Авторизация";
		showOperationInfo();		
		SettingsData settingsData = Settings.getSettingData(this.mContext);
		LoginTask loginTask = new LoginTask(this, this, this.mContext);
		loginTask.execute(Settings.getLoginUrl(mContext), settingsData.Login, settingsData.Password);
		this.mCurrentExecutedTask = loginTask;
	}
	
	public void startGetOnlyChangedData(){
		if(mOperationType != OperationType.NOTHING){
			Toast.makeText(this.mContext, "Идет завершение предыдущей операции", Toast.LENGTH_LONG).show();
			return;
		}
		setEmptyServerId();		
		mOperationType = OperationType.GET_CHANGED_DATA;
		mProgressDialogTitle = "Загрузка данных...";
		mProgressDialogMessage = "Авторизация";
		showOperationInfo();	
		SettingsData settingsData = Settings.getSettingData(this.mContext);
		LoginTask loginTask = new LoginTask(this, this, this.mContext);
		loginTask.execute(Settings.getLoginUrl(mContext), settingsData.Login, settingsData.Password);	
		this.mCurrentExecutedTask = loginTask;
	}
	
	public void startGetOnlyChangedData(int cemeteryServerId){
		if(mOperationType != OperationType.NOTHING){
			Toast.makeText(this.mContext, "Идет завершение предыдущей операции", Toast.LENGTH_LONG).show();
			return;
		}
		setEmptyServerId();		
		mOperationType = OperationType.GET_CHANGED_DATA_ONE_CEMETERY;
		mCemeteryServerIds.add(cemeteryServerId);
		mProgressDialogTitle = "Загрузка данных...";
		mProgressDialogMessage = "Авторизация";
		showOperationInfo();	
		SettingsData settingsData = Settings.getSettingData(this.mContext);
		LoginTask loginTask = new LoginTask(this, this, this.mContext);
		loginTask.execute(Settings.getLoginUrl(mContext), settingsData.Login, settingsData.Password);
		this.mCurrentExecutedTask = loginTask;
	}
	
	public void startUploadData(){
		if(mOperationType != OperationType.NOTHING){
			Toast.makeText(this.mContext, "Идет завершение предыдущей операции", Toast.LENGTH_LONG).show();
			return;
		}
		setEmptyServerId();		
		executeStartUploadData(false);	
	}
	
	private void executeStartUploadData(boolean isRepeat){
		mOperationType = OperationType.UPLOAD_DATA;
		mTasks.clear();
		mTasks.add(new RemovePhotoTask(this, this, this.mContext));
		mTasks.add(new UploadCemeteryTask(this, this, this.mContext));
		mTasks.add(new UploadRegionTask(this, this, this.mContext));
		mTasks.add(new UploadPlaceTask(this, this, this.mContext));
		mTasks.add(new UploadGraveTask(this, this, this.mContext));
		mTasks.add(new UploadBurialTask(this, this, this.mContext));
		mTasks.add(new UploadPhotoTask(this, this, this.mContext));
		mCurrentTaskIndex = -1;
		mProgressDialogTitle = "Отправка данных...";
		mProgressDialogMessage = "Авторизация";
		if(!isRepeat){
			this.mUploadTaskResults = new ArrayList<TaskResult>();
			for(int i = 0; i < mTasks.size(); i++){
				TaskResult newTaskResult = new TaskResult();
				this.mUploadTaskResults.add(newTaskResult);
			}
			showOperationInfo();
		}
		SettingsData settingsData = Settings.getSettingData(this.mContext);
		LoginTask loginTask = new LoginTask(this, this, this.mContext);
		loginTask.execute(Settings.getLoginUrl(mContext), settingsData.Login, settingsData.Password);
		this.mCurrentExecutedTask = loginTask;
	}
	
	private void showOperationInfo(){
		mProgressDialogSyncData = new ProgressDialog(this.mContext);
		mProgressDialogSyncData.setMessage(mProgressDialogMessage);
		mProgressDialogSyncData.setTitle(mProgressDialogTitle);
		mProgressDialogSyncData.setCancelable(false);
		if(mOperationType == OperationType.GET_CHANGED_DATA || mOperationType == OperationType.GET_CHANGED_DATA_ONE_CEMETERY){
			mProgressDialogSyncData.setButton("Отменить", new DialogInterface.OnClickListener() {
	
	            public void onClick(DialogInterface dialog, int which) {
	        		mIsStartExecuteNextTask = false;            		
	        		mProgressDialogSyncData.dismiss();
	        		showCancelOperationQuestion();
	            }
	        });
		}
		if(mOperationType == OperationType.GET_DATA || mOperationType == OperationType.UPLOAD_DATA){
			mProgressDialogSyncData.setButton("Отменить", new DialogInterface.OnClickListener() {
	
	            public void onClick(DialogInterface dialog, int which) {
	        		interruptCurrentTask();           		
	        		mProgressDialogSyncData.dismiss();
	        		showCancelOperationInfo();
	            }
	        });
		}
		mProgressDialogSyncData.show();
	}
	
	private void showCancelOperationQuestion(){
		mProgressDialogCancelQuestion = new ProgressDialog(this.mContext);
		mProgressDialogCancelQuestion.setTitle("Принудительно завершить?");
		mProgressDialogCancelQuestion.setMessage("Идет завершение последней операции...");		
		mProgressDialogCancelQuestion.setCancelable(false);		
		mProgressDialogCancelQuestion.setButton("Да", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
        		interruptCurrentTask();
        		mProgressDialogCancelQuestion.dismiss();
        		showCancelOperationInfo();
            }
        });
		mProgressDialogCancelQuestion.show();		
	}
	
	private void showCancelOperationInfo(){
		mProgressDialogCancelInfo = new ProgressDialog(this.mContext);
		mProgressDialogCancelInfo.setTitle("Идет завершение последней операции");
		mProgressDialogCancelInfo.setMessage("Завершение...");
		mProgressDialogCancelInfo.setCancelable(false);
		mProgressDialogCancelInfo.show();		
	}
	
	private void interruptCurrentTask(){
		this.mIsStartExecuteNextTask = false; 
		this.mIsInteruptExecutedTask = true;
		if(this.mCurrentExecutedTask != null){
			try{				
    			mCurrentExecutedTask.cancel(true);
    		}catch(Exception exc){
    			exc.printStackTrace();        			
    		}
		}
	}
	
	public void checkResumeDataOperation(Context context){
		if(mOperationType == OperationType.GET_DATA || mOperationType == OperationType.GET_CHANGED_DATA || mOperationType == OperationType.GET_CHANGED_DATA_ONE_CEMETERY ){
			setContext(context);
			if(this.mIsStartExecuteNextTask == true){
				showOperationInfo();
			} else {
				if(this.mIsInteruptExecutedTask){
					showCancelOperationInfo();
				} else {
					showCancelOperationQuestion();
				}
			}
		}
		if(mOperationType == OperationType.UPLOAD_DATA){
			setContext(context);
			mProgressDialogSyncData = ProgressDialog.show(this.mContext, mProgressDialogTitle, mProgressDialogMessage, true);
			mProgressDialogSyncData.setCancelable(false);
		}
		if(mOperationType == OperationType.CHECK_LOGIN){
			setContext(context);
			mProgressDialogSyncData = ProgressDialog.show(this.mContext, mProgressDialogTitle, mProgressDialogMessage, true);
			mProgressDialogSyncData.setCancelable(false);
		}
	}
	
	private void showSummaryUploadInfo(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
		builder.setTitle("Статистика отправленных данных");
		StringBuilder sbMessage = new StringBuilder();
		boolean isTryUploadSomething = false;
		boolean isCancelTask = false;
		int i = 0; 
		for(BaseTask task : this.mTasks){
			TaskResult result = this.mUploadTaskResults.get(i++);
			if(result.getUploadCount() > 0){
				isTryUploadSomething = true;
				sbMessage.append(System.getProperty("line.separator"));
				if(task instanceof RemovePhotoTask){
					sbMessage.append(String.format("Успешно удалено фотографий: %d  из %d.", result.getUploadCountSuccess(), result.getUploadCount()));				
				}
				if(task instanceof UploadCemeteryTask){
					sbMessage.append(String.format("Успешно отправлено кладбищ: %d из %d.", result.getUploadCountSuccess(), result.getUploadCount()));				
				}
				if(task instanceof UploadRegionTask){
					sbMessage.append(String.format("Успешно отправлено участков: %d  из %d.", result.getUploadCountSuccess(), result.getUploadCount()));				
				}
				if(task instanceof UploadPlaceTask){
					sbMessage.append(String.format("Успешно отправлено мест: %d из %d.", result.getUploadCountSuccess(), result.getUploadCount()));				
				}
				if(task instanceof UploadGraveTask){
					sbMessage.append(String.format("Успешно отправлено могил: %d из %d.", result.getUploadCountSuccess(), result.getUploadCount()));				
				}
				if(task instanceof UploadBurialTask){
                    sbMessage.append(String.format("Успешно отправлено захоронений: %d из %d.", result.getUploadCountSuccess(), result.getUploadCount()));                
                }
				if(task instanceof UploadPhotoTask){
					sbMessage.append(String.format("Успешно отправлено фотографий: %d из %d.", result.getUploadCountSuccess(), result.getUploadCount()));				
				}
				sbMessage.append(System.getProperty("line.separator"));
			}			
			if(result.getStatus() == TaskResult.Status.CANCEL_TASK){
				isCancelTask = true;
				break;
			}
		}
		
		if(isCancelTask){
			sbMessage.append(System.getProperty("line.separator"));
			sbMessage.append("Отправка данных отменена пользователем");
			sbMessage.append(System.getProperty("line.separator"));
		} else {
			if(!isTryUploadSomething){
				sbMessage = new StringBuilder();
				sbMessage.append(System.getProperty("line.separator"));
				sbMessage.append("Изменившихся данных для отправки не найдено");
				sbMessage.append(System.getProperty("line.separator"));
			}
		}
		builder.setMessage(sbMessage.toString());
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
		
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
	private boolean isRepeatUploadAndStoreResults(){
		boolean isRepeat = false;		
		for(int i = 0; i < this.mTasks.size(); i++){
			TaskResult result = this.mTasks.get(i).getTaskResult();
			TaskResult previousResult = this.mUploadTaskResults.get(i);
			int previousUploadCountError = (previousResult.getStatus() != null) ? previousResult.getUploadCountError() : Integer.MAX_VALUE;
			if(result.getUploadCountError() > 0 && result.getUploadCountError() < previousUploadCountError){
				isRepeat = true;
			}
			
			//store result of tasks
			if(previousResult.getStatus() == null){				
				previousResult.setUploadCount(result.getUploadCount());
			}			
			previousResult.setUploadCountError(result.getUploadCountError());
			previousResult.setUploadCountSuccess(previousResult.getUploadCountSuccess() + result.getUploadCountSuccess());
			previousResult.setError(result.isError());
			previousResult.setStatus(result.getStatus());
		}
		return isRepeat;		
	}
	
	private void showErrorInfo(String infoMessage){
		AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
		builder.setTitle("Ошибка");
		builder.setMessage(infoMessage);
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
		
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
	public String getURLArgs(){
		String getArgs = "";
		if(mCemeteryServerId > 0){
			if(getArgs.length() == 0){
				getArgs += String.format("?"+ BaseTask.ARG_CEMETERY_ID + "=%d", mCemeteryServerId);
			} else{
				getArgs += String.format("&"+ BaseTask.ARG_CEMETERY_ID + "=%d", mCemeteryServerId);
			}
		}
		if(mRegionServerId > 0){
			if(getArgs.length() == 0){
				getArgs += String.format("?"+ BaseTask.ARG_AREA_ID + "=%d", mRegionServerId);
			} else{
				getArgs += String.format("&"+ BaseTask.ARG_AREA_ID + "=%d", mRegionServerId);
			}
		}
		if(mPlaceServerId > 0){
			if(getArgs.length() == 0){
				getArgs += String.format("?"+ BaseTask.ARG_PLACE_ID + "=%d", mPlaceServerId);
			} else{
				getArgs += String.format("&"+ BaseTask.ARG_PLACE_ID + "=%d", mPlaceServerId);
			}
		}
		if(mGraveServerId > 0){
			if(getArgs.length() == 0){
				getArgs += String.format("?"+ BaseTask.ARG_GRAVE_ID + "=%d", mGraveServerId);
			} else{
				getArgs += String.format("&"+ BaseTask.ARG_GRAVE_ID + "=%d", mGraveServerId);
			}
		}
		
		if(mBurialStatus != null){
            if(getArgs.length() == 0){
                getArgs += String.format("?"+ BaseTask.ARG_BURIAL_STATUS + "=%s", mBurialStatus.name().toLowerCase());
            } else{
                getArgs += String.format("&"+ BaseTask.ARG_BURIAL_STATUS + "=%s", mBurialStatus.name().toLowerCase());
            }
        }
		
		if(mSyncDate != null){
			if(getArgs.length() == 0){
				getArgs += String.format("?"+ BaseTask.ARG_SYNC_DATE + "=%d", (mSyncDate.getTime()/1000L));
			} else{
				getArgs += String.format("&"+ BaseTask.ARG_SYNC_DATE + "=%d", (mSyncDate.getTime()/1000L));
			}
		}
		return getArgs;
	}
	
	@Override
	public void onProgressUpdate(String... messages) {		
		mProgressDialogSyncData.setMessage(messages[0]);		
	}

	@Override
	public void onTaskComplete(BaseTask finishedTask, TaskResult result) {
		boolean isNextTaskStarted = false;
		boolean isCancelTask = false;		
		boolean isAuthenticated = true;
		boolean isError = false;
		TaskResult loginTaskResult = null;
		if(result.isError() && result.getStatus() != TaskResult.Status.CANCEL_TASK ){
			isError = true;
		}
		if(result.getStatus() == Status.CANCEL_TASK){
			isCancelTask = true;
		}
		if(finishedTask instanceof LoginTask){
			loginTaskResult = result;
			if(!result.isError() && result.getStatus() == TaskResult.Status.LOGIN_SUCCESSED){
				LoginTask loginTask = (LoginTask) finishedTask;				
				isAuthenticated = true;
			} else {
				isAuthenticated = false;
			}
		}
		
		if(isAuthenticated && !isError && !isCancelTask && this.mIsStartExecuteNextTask){
			String getArgs = getURLArgs();
			if(mOperationType == OperationType.GET_CHANGED_DATA || mOperationType == OperationType.GET_CHANGED_DATA_ONE_CEMETERY){			
				if(finishedTask.getTaskName() == Settings.TASK_LOGIN){
					mProgressDialogMessage = PD_GETCEMETERY_MESSAGE;
					mProgressDialogSyncData.setMessage(mProgressDialogMessage);						
					GetCemeteryTask getCemeteryTask = new GetCemeteryTask(this, this, this.mContext);
					getCemeteryTask.execute(Settings.getCemeteryUrl(mContext));
					this.mCurrentExecutedTask = getCemeteryTask;
					isNextTaskStarted = true;	
				}
				//finish GetCemetery
				if(finishedTask.getTaskName() == Settings.TASK_GETCEMETERY){
					if(mCemeteryServerIds.size() > 0){
						//for one cemetery get date
						List<Cemetery> listCemetery = DB.dao(Cemetery.class).queryForEq("ServerId", mCemeteryServerIds.get(0));
						if(listCemetery.size() == 0){
							mCemeteryServerIds.clear();
						}
					} else {
						List<Cemetery> listCemetery = DB.dao(Cemetery.class).queryForAll();
						for(Cemetery cem : listCemetery){
							if(cem.ServerId != BaseDTO.INT_NULL_VALUE){
								mCemeteryServerIds.add(cem.ServerId);
							}
						}						
					}
					mCurrentCemeteryIdsIndex = 0;
					
					if(mCemeteryServerIds.size() > 0){
						mProgressDialogMessage = PD_GETREGION_MESSAGE;
						mProgressDialogSyncData.setMessage(mProgressDialogMessage);
						GetRegionTask getRegionTask = new GetRegionTask(this, this, this.mContext);
						this.mCemeteryServerId = mCemeteryServerIds.get(mCurrentCemeteryIdsIndex);
						this.mRegionServerId = BaseDTO.INT_NULL_VALUE;
						Cemetery cemetery = DB.dao(Cemetery.class).queryForEq("ServerId", mCemeteryServerId).get(0);
						this.mSyncDate = cemetery.RegionSyncDate;
						getArgs = getURLArgs();
						getRegionTask.execute(Settings.getRegionUrl(mContext) + getArgs);
						this.mCurrentExecutedTask = getRegionTask;
						isNextTaskStarted = true;
					}
				}
				//finish GetRegion
				if(finishedTask.getTaskName() == Settings.TASK_GETREGION){
					this.mCemeteryServerId = mCemeteryServerIds.get(mCurrentCemeteryIdsIndex);
					List<Region> listRegion = DB.dao(Region.class).queryForEq("ParentServerId", this.mCemeteryServerId);
					this.mRegionServerIds.clear();
					for(Region reg : listRegion){
						if(reg.ServerId != BaseDTO.INT_NULL_VALUE){
							this.mRegionServerIds.add(reg.ServerId);
						}
					}
					if(mRegionServerIds.size() > 0){
						mCurrentRegionIdsIndex = 0;
						this.mRegionServerId = this.mRegionServerIds.get(mCurrentRegionIdsIndex);						
						Region region = DB.dao(Region.class).queryForEq("ServerId", this.mRegionServerId).get(0);
						this.mSyncDate = region.PlaceSyncDate;
						
						mProgressDialogMessage = PD_GETPLACE_MESSAGE;
						mProgressDialogSyncData.setMessage(mProgressDialogMessage);
						GetPlaceTask getPlaceTask = new GetPlaceTask(this, this, this.mContext);
						getArgs = getURLArgs();					
						getPlaceTask.execute(Settings.getPlaceUrl(mContext) + getArgs);
						this.mCurrentExecutedTask = getPlaceTask;
						isNextTaskStarted = true;
					} else {
						//нет участков у кладбища
						if(mCurrentCemeteryIdsIndex < (mCemeteryServerIds.size() - 1)){
							mProgressDialogMessage = PD_GETREGION_MESSAGE;
							mProgressDialogSyncData.setMessage(mProgressDialogMessage);
							GetRegionTask getRegionTask = new GetRegionTask(this, this, this.mContext);
							mCurrentCemeteryIdsIndex++;
							this.mCemeteryServerId = mCemeteryServerIds.get(mCurrentCemeteryIdsIndex);
							this.mRegionServerId = BaseDTO.INT_NULL_VALUE;
							Cemetery cemetery = DB.dao(Cemetery.class).queryForEq("ServerId", mCemeteryServerId).get(0);
							this.mSyncDate = cemetery.RegionSyncDate;
							getArgs = getURLArgs();
							getRegionTask.execute(Settings.getRegionUrl(mContext) + getArgs);
							this.mCurrentExecutedTask = getRegionTask;
							isNextTaskStarted = true;
						} else {
							//finish
						}
					}
					
					
					
				}
				//finish GetPlace
				if(finishedTask.getTaskName() == Settings.TASK_GETPLACE){					
					this.mRegionServerId = this.mRegionServerIds.get(mCurrentRegionIdsIndex);
					Region region = DB.dao(Region.class).queryForEq("ServerId", this.mRegionServerId).get(0);
					this.mSyncDate = region.GraveSyncDate;					
					this.mCemeteryServerId = mCemeteryServerIds.get(mCurrentCemeteryIdsIndex);
					
					mProgressDialogMessage = PD_GETGRAVE_MESSAGE;
					mProgressDialogSyncData.setMessage(mProgressDialogMessage);
					GetGraveTask getGraveTask = new GetGraveTask(this, this, this.mContext);
					getArgs = getURLArgs();
					this.mFileLog.info("onTaskCompletePrevAndStartNew:" + getGraveTask.getTaskName() + getGraveTask.hashCode());
					getGraveTask.execute(Settings.getGraveUrl(mContext) + getArgs);
					this.mCurrentExecutedTask = getGraveTask;
					isNextTaskStarted = true;
				}
				//finish GetGrave
				if(finishedTask.getTaskName() == Settings.TASK_GETGRAVE){
					this.mRegionServerId = this.mRegionServerIds.get(mCurrentRegionIdsIndex);
					Region region = DB.dao(Region.class).queryForEq("ServerId", this.mRegionServerId).get(0);
					this.mSyncDate = region.BurialSyncDate;					
					this.mCemeteryServerId = mCemeteryServerIds.get(mCurrentCemeteryIdsIndex);
					
					mProgressDialogMessage = PD_GETBURIAL_MESSAGE;
					mProgressDialogSyncData.setMessage(mProgressDialogMessage);
					GetBurialTask getBurialTask = new GetBurialTask(this, this, this.mContext);
					getArgs = getURLArgs();
					getBurialTask.execute(Settings.getBurialUrl(mContext) + getArgs);
					this.mCurrentExecutedTask = getBurialTask;
					isNextTaskStarted = true;
				}
				//finish GetBurial
				if(finishedTask.getTaskName() == Settings.TASK_GETBURIAL){
					if(mCurrentRegionIdsIndex < (mRegionServerIds.size() - 1)){
						mCurrentRegionIdsIndex++;
						this.mCemeteryServerId = this.mCemeteryServerIds.get(mCurrentCemeteryIdsIndex);
						this.mRegionServerId = this.mRegionServerIds.get(mCurrentRegionIdsIndex);
						Region region = DB.dao(Region.class).queryForEq("ServerId", this.mRegionServerId).get(0);
						this.mSyncDate = region.PlaceSyncDate;
						
						mProgressDialogMessage = PD_GETPLACE_MESSAGE;
						mProgressDialogSyncData.setMessage(mProgressDialogMessage);
						GetPlaceTask getPlaceTask = new GetPlaceTask(this, this, this.mContext);
						getArgs = getURLArgs();					
						getPlaceTask.execute(Settings.getPlaceUrl(mContext) + getArgs);
						this.mCurrentExecutedTask = getPlaceTask;
						isNextTaskStarted = true;
					} else {
						if(mCurrentCemeteryIdsIndex < (mCemeteryServerIds.size() - 1)){
							mCurrentCemeteryIdsIndex++;
							mCurrentRegionIdsIndex = 0;
							mProgressDialogMessage = PD_GETREGION_MESSAGE;
							mProgressDialogSyncData.setMessage(mProgressDialogMessage);
							GetRegionTask getRegionTask = new GetRegionTask(this, this, this.mContext);
							this.mCemeteryServerId = mCemeteryServerIds.get(mCurrentCemeteryIdsIndex);
							this.mRegionServerId = BaseDTO.INT_NULL_VALUE;
							Cemetery cemetery = DB.dao(Cemetery.class).queryForEq("ServerId", mCemeteryServerId).get(0);
							this.mSyncDate = cemetery.RegionSyncDate;
							getArgs = getURLArgs();
							getRegionTask.execute(Settings.getRegionUrl(mContext) + getArgs);
							this.mCurrentExecutedTask = getRegionTask;
							isNextTaskStarted = true;
						} else {
							//finish
						}
					}
				}
				 
			}
			
			if(mOperationType == OperationType.GET_DATA){			
				mCurrentTaskIndex++;
				if(mCurrentTaskIndex < mTasks.size()){
					if(mCurrentTaskIndex == 0){
						LoginTask loginTask = (LoginTask) finishedTask;						
					}
					BaseTask nextTask = mTasks.get(mCurrentTaskIndex);
					if(nextTask.getTaskName() == Settings.TASK_GETCEMETERY){
						mProgressDialogMessage = PD_GETCEMETERY_MESSAGE;
						mProgressDialogSyncData.setMessage(mProgressDialogMessage);						
						GetCemeteryTask getCemeteryTask = new GetCemeteryTask(this, this, this.mContext);
						getCemeteryTask.execute(Settings.getCemeteryUrl(mContext) + getArgs);
						this.mCurrentExecutedTask = getCemeteryTask;
						this.mTasks.set(mCurrentTaskIndex, this.mCurrentExecutedTask);
						isNextTaskStarted = true;						
					}
					if(nextTask.getTaskName() == Settings.TASK_GETREGION){
						mProgressDialogMessage = PD_GETREGION_MESSAGE;
						mProgressDialogSyncData.setMessage(mProgressDialogMessage);
						GetRegionTask getRegionTask = new GetRegionTask(this, this, this.mContext);
						getRegionTask.execute(Settings.getRegionUrl(mContext) + getArgs);
						this.mCurrentExecutedTask = getRegionTask;
						isNextTaskStarted = true;
					}
					if(nextTask.getTaskName() == Settings.TASK_GETPLACE){
						mProgressDialogMessage = PD_GETPLACE_MESSAGE;
						mProgressDialogSyncData.setMessage(mProgressDialogMessage);
						GetPlaceTask getPlaceTask = new GetPlaceTask(this, this, this.mContext);
						getPlaceTask.execute(Settings.getPlaceUrl(mContext) + getArgs);
						this.mCurrentExecutedTask = getPlaceTask;						
						isNextTaskStarted = true;
					}					
					if(nextTask.getTaskName() == Settings.TASK_GETGRAVE){
						mProgressDialogMessage = PD_GETGRAVE_MESSAGE;
						mProgressDialogSyncData.setMessage(mProgressDialogMessage);
						GetGraveTask getGraveTask = new GetGraveTask(this, this, this.mContext);						
						getGraveTask.execute(Settings.getGraveUrl(mContext) + getArgs);
						this.mCurrentExecutedTask = getGraveTask;
						isNextTaskStarted = true;
					}
					if(nextTask.getTaskName() == Settings.TASK_GETPLACEPHOTO){
                        mProgressDialogMessage = PD_GETPLACEPHOTO_MESSAGE;
                        mProgressDialogSyncData.setMessage(mProgressDialogMessage);
                        GetPlacePhotoTask getPlacePhotoTask = new GetPlacePhotoTask(this, this, this.mContext);
                        getPlacePhotoTask.execute(Settings.getPlacePhotoItemsUrl(mContext) + getArgs);
                        this.mCurrentExecutedTask = getPlacePhotoTask;                       
                        isNextTaskStarted = true;
                    }
					if(nextTask.getTaskName() == Settings.TASK_GETBURIAL){
						mProgressDialogMessage = PD_GETBURIAL_MESSAGE;
						mProgressDialogSyncData.setMessage(mProgressDialogMessage);
						GetBurialTask getBurialTask = new GetBurialTask(this, this, this.mContext);
						getBurialTask.execute(Settings.getBurialUrl(mContext) + getArgs);
						this.mCurrentExecutedTask = getBurialTask;
						isNextTaskStarted = true;
					}
					this.mTasks.set(mCurrentTaskIndex, this.mCurrentExecutedTask);
				}			
				if(mCurrentTaskIndex == mTasks.size()){
					DB.db().updateDBLink();
					mProgressDialogMessage = "Данные получены";
					mProgressDialogSyncData.setMessage(mProgressDialogMessage);					
				}
			}			
			
		}
		
		if(isAuthenticated && !isCancelTask && this.mIsStartExecuteNextTask) {
			if(mOperationType == OperationType.UPLOAD_DATA){
				mCurrentTaskIndex++;
				if(mCurrentTaskIndex < mTasks.size()){					
					BaseTask nextTask = mTasks.get(mCurrentTaskIndex);
					if(nextTask.getTaskName() == Settings.TASK_REMOVEPHOTO){
						mProgressDialogMessage = "Удаление фотографий на сервере";
						mProgressDialogSyncData.setMessage(mProgressDialogMessage);
						RemovePhotoTask removePhotoTask = new RemovePhotoTask(this, this, this.mContext);
						removePhotoTask.execute();
						this.mCurrentExecutedTask = removePhotoTask;
						isNextTaskStarted = true;
					}
					if(nextTask.getTaskName() == Settings.TASK_POSTCEMETERY){
						mProgressDialogMessage = "Отправка кладбищ на сервер";
						mProgressDialogSyncData.setMessage(mProgressDialogMessage);
						UploadCemeteryTask uploadCemeteryTask = new UploadCemeteryTask(this, this, this.mContext);
						uploadCemeteryTask.execute(Settings.getUploadCemeteryUrl(this.mContext));
						this.mCurrentExecutedTask = uploadCemeteryTask;
						isNextTaskStarted = true;
					}
					if(nextTask.getTaskName() == Settings.TASK_POSTREGION){
						mProgressDialogMessage = "Отправка участков на сервер";
						mProgressDialogSyncData.setMessage(mProgressDialogMessage);						
						UploadRegionTask uploadRegionTask = new UploadRegionTask(this, this, this.mContext);
						uploadRegionTask.execute(Settings.getUploadRegionUrl(this.mContext));
						this.mCurrentExecutedTask = uploadRegionTask;
						isNextTaskStarted = true;
					}
					if(nextTask.getTaskName() == Settings.TASK_POSTPLACE){
						mProgressDialogMessage = "Отправка мест на сервер";
						mProgressDialogSyncData.setMessage(mProgressDialogMessage);						
						UploadPlaceTask uploadPlaceTask = new UploadPlaceTask(this, this, this.mContext);
						uploadPlaceTask.execute(Settings.getUploadPlaceUrl(this.mContext));
						this.mCurrentExecutedTask = uploadPlaceTask;
						isNextTaskStarted = true;
					}
					if(nextTask.getTaskName() == Settings.TASK_POSTGRAVE){
						mProgressDialogMessage = "Отправка могил на сервер";
						mProgressDialogSyncData.setMessage(mProgressDialogMessage);						
						UploadGraveTask uploadGraveTask = new UploadGraveTask(this, this, this.mContext);
						uploadGraveTask.execute(Settings.getUploadGraveUrl(this.mContext));
						this.mCurrentExecutedTask = uploadGraveTask;
						isNextTaskStarted = true;
					}
					if(nextTask.getTaskName() == Settings.TASK_POSTBURIAL){
                        mProgressDialogMessage = "Отправка захоронений на сервер";
                        mProgressDialogSyncData.setMessage(mProgressDialogMessage);                     
                        UploadBurialTask uploadBurialTask = new UploadBurialTask(this, this, this.mContext);
                        uploadBurialTask.execute(Settings.getUploadBurialUrl(this.mContext));
                        this.mCurrentExecutedTask = uploadBurialTask;
                        isNextTaskStarted = true;
                    }
					if(nextTask.getTaskName() == Settings.TASK_POSTPHOTO){
						mProgressDialogMessage = "Отправка фотографий на сервер";
						mProgressDialogSyncData.setMessage(mProgressDialogMessage);						
						UploadPhotoTask uploadPhotoTask = new UploadPhotoTask(this, this, this.mContext);
						uploadPhotoTask.execute();
						this.mCurrentExecutedTask = uploadPhotoTask;
						isNextTaskStarted = true;
					}
					this.mTasks.set(mCurrentTaskIndex, this.mCurrentExecutedTask);
				}			
			}
		}
		
		//Если были ошибки при отправке, то пытаемся заново отправить данные
		if(mOperationType == OperationType.UPLOAD_DATA && !isNextTaskStarted && isAuthenticated){		
			if(mOperationType == OperationType.UPLOAD_DATA){
				if(!isCancelTask && this.mIsStartExecuteNextTask && isRepeatUploadAndStoreResults()){
					isNextTaskStarted = true;
					executeStartUploadData(true);					
				}
			}
			
		}
		
		
		if(!isNextTaskStarted){	
			if(mOperationType == OperationType.UPLOAD_DATA && isAuthenticated){
				showSummaryUploadInfo();
			} else {
				String infoMessage = null;			
				switch (result.getStatus()) {
				    case HANDLE_ERROR :
				    	infoMessage = "Ошибка обработки данных.";
				    	break;
				    case LOGIN_FAILED :
				    	infoMessage = "Ошибка авторизации на сервере.";
				    	break;
				    case SERVER_UNAVALAIBLE :
				    	infoMessage = "Сервер не доступен.";
				    	break;
				    default:
				    	break;
				}			
				if(infoMessage != null){
					showErrorInfo(infoMessage);
				}				
			}
			
			if(mOperationType == OperationType.GET_DATA || mOperationType == OperationType.GET_CHANGED_DATA || mOperationType == OperationType.GET_CHANGED_DATA_ONE_CEMETERY){
				DB.db().updateDBLink();				
			}
			mProgressDialogSyncData.dismiss();
			if(mProgressDialogCancelQuestion != null) {
				mProgressDialogCancelQuestion.dismiss();
			}
			if(mProgressDialogCancelInfo != null){
				mProgressDialogCancelInfo.dismiss();
			}
			if(this.onSyncCompleteListener != null){
				this.onSyncCompleteListener.onComplete(this.mOperationType, result);
			}
			mOperationType = OperationType.NOTHING;
			
			
			
			
			
		}
		
		
	}
	
}
