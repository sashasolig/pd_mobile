package com.coolchoice.monumentphoto;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.coolchoice.monumentphoto.BrowserCemeteryActivity.RowGridAdapter.RowOrPlace;
import com.coolchoice.monumentphoto.Settings.ISettings;
import com.coolchoice.monumentphoto.SyncTaskHandler.OperationType;
import com.coolchoice.monumentphoto.dal.DB;
import com.coolchoice.monumentphoto.dal.MonumentDB;
import com.coolchoice.monumentphoto.data.BaseDTO;
import com.coolchoice.monumentphoto.data.Burial;
import com.coolchoice.monumentphoto.data.Cemetery;
import com.coolchoice.monumentphoto.data.ComplexGrave;
import com.coolchoice.monumentphoto.data.Grave;
import com.coolchoice.monumentphoto.data.Photo;
import com.coolchoice.monumentphoto.data.Place;
import com.coolchoice.monumentphoto.data.PlacePhoto;
import com.coolchoice.monumentphoto.data.Region;
import com.coolchoice.monumentphoto.data.ResponsibleUser;
import com.coolchoice.monumentphoto.data.Row;
import com.coolchoice.monumentphoto.data.SettingsData;
import com.coolchoice.monumentphoto.data.ILogable.LogOperation;
import com.coolchoice.monumentphoto.photomanager.ThreadManager;
import com.coolchoice.monumentphoto.task.TaskResult;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.QueryBuilder;


public class BrowserCemeteryActivity extends Activity implements LocationListener, SyncTaskHandler.SyncCompleteListener, ISettings, ThreadManager.OnChangeStatus {

	public static final String EXTRA_CEMETERY_ID = "CemeteryId";
	public static final String EXTRA_REGION_ID = "RegionId";
	public static final String EXTRA_ROW_ID = "RowId";
	public static final String EXTRA_PLACE_ID = "PlaceId";
	public static final String EXTRA_GRAVE_ID = "GraveId";
	public static final String EXTRA_TYPE = "extra_type";
	
	public static final String EXTRA_NEXT_PLACE_ID = "NextPlaceId";
	public static final String EXTRA_PREV_PLACE_ID = "PrevPlaceId";
	public static final String EXTRA_NEXT_PLACE_NAME = "NextPlaceName";
	public static final String EXTRA_PREV_PLACE_NAME = "PrevPlaceName";
	
	public static final int ADD_OBJECT_REQUEST_CODE = 1;
	public static final int EDIT_OBJECT_REQUEST_CODE = 2;
	
	private static int mPrevType = -1; 
		
	private Button btnLinkCemetery, btnLinkRegion, btnLinkRow, btnLinkPlace, btnLinkGrave, btnLinkHome, btnLinkNextPlace, btnLinkPrevPlace;
	
	private LinearLayout mainView;
	
	private int mCemeteryId, mRegionId, mRowId, mPlaceId, mGraveId;
	private int mType;
	
		
	private static Region mChoosedRegion;
	private static RowOrPlace mChoosedRowOrPlace;
	private static Place mChoosedPlace;
	private static Grave mChoosedGrave;
	private static int mChoosedGridViewId = -1;
	
	private static boolean mIsCheckGPS = true;
	private static PHOTOTYPE mMakePhotoType = PHOTOTYPE.PLACEPHOTO_CURRENT; 
	
	public enum PHOTOTYPE {	     
	     PLACEPHOTO_CURRENT,
	     PLACEPHOTO_NEXTPLACE
	}
	
	private GridView mGVRegion, mGVRow, mGVPlace, mGVGrave;
	
	private GraveGridAdapter mGraveGridAdapter;
	
	private HorizontalScrollView mAddressBarSV;
	
	private static SyncTaskHandler mSyncTaskHandler;
	
	private Menu mOptionsMenu;
	
	private CheckBox cbPlaceUnowned, cbPlaceSizeVioleted, cbPlaceUnindentified, cbPlaceWrongFIO, cbPlaceMilitary;
			
	private TextView tvPersons = null;
	
	private EditText etPlaceWidth = null, etPlaceLength = null;
	
	private TextView tvResponsiblePersonOfPlace = null;
	
	private EditText etOldPlaceInAlert;
	
	protected final Logger mFileLog = Logger.getLogger(BrowserCemeteryActivity.class);
	
	public void enterOldPlaceName(){
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View mainView = inflater.inflate(R.layout.enter_old_place_alert, null, false);
		this.etOldPlaceInAlert = (EditText) mainView.findViewById(R.id.etPlace);
		final Button btnSearch = (Button) mainView.findViewById(R.id.btnSearchPlace);	    
	    alert.setTitle("Введите старое место");
	    alert.setView(mainView);
	    btnSearch.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(BrowserCemeteryActivity.this, PlaceSearchActivity.class);
				intent.putExtra(PlaceSearchActivity.EXTRA_CEMETERY_ID, mCemeteryId);
				startActivityForResult(intent, PlaceSearchActivity.PLACE_SEARCH_REQUESTCODE);
			}
		});
	    alert.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	            String value = etOldPlaceInAlert.getText().toString().trim();
	            switch (mMakePhotoType) {
                case PLACEPHOTO_NEXTPLACE:
                    makePlacePhotoNextPlace(value);
                default:
                    break;
                }	            
	            dialog.cancel();
	        }
	    });

	    alert.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	            switch (mMakePhotoType) {
                case PLACEPHOTO_NEXTPLACE:
                    makePlacePhotoNextPlace(null);
                default:
                    break;
	            }
	            dialog.cancel();
	        }
	    });
	    alert.show(); 
	}
	
	private void updateOptionsMenu() {
		if(this.mOptionsMenu == null) return;
		MenuItem actionGetMenuItem = this.mOptionsMenu.findItem(R.id.action_get);
        if (Settings.IsAutoDownloadData(this)) {
            actionGetMenuItem.setIcon(R.drawable.load_data_enable);           
        } else {
        	actionGetMenuItem.setIcon(R.drawable.load_data_disable); 
        }
        MenuItem actionRemoveMenuItem = this.mOptionsMenu.findItem(R.id.action_remove);
        int type = getIntent().getIntExtra(EXTRA_TYPE, -1);
        if(type == AddObjectActivity.ADD_GRAVE_WITHROW || type == AddObjectActivity.ADD_GRAVE_WITHOUTROW || 
                type == AddObjectActivity.ADD_PLACE_WITHOUTROW || type == AddObjectActivity.ADD_PLACE_WITHROW ){
        	actionRemoveMenuItem.setVisible(true);
        } else {
        	actionRemoveMenuItem.setVisible(false);
        }
        
    }
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.mPrevType = -1;
	}
			
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.browser_cemetery_activity);
		mCemeteryId = getIntent().getIntExtra(EXTRA_CEMETERY_ID, -1);
		mRegionId = getIntent().getIntExtra(EXTRA_REGION_ID, -1);
		mRowId = getIntent().getIntExtra(EXTRA_ROW_ID, -1);
		mPlaceId = getIntent().getIntExtra(EXTRA_PLACE_ID, -1);
		mGraveId = getIntent().getIntExtra(EXTRA_GRAVE_ID, -1);
		mType = getIntent().getIntExtra(EXTRA_TYPE, -1);
		this.mainView = (LinearLayout)findViewById(R.id.main_view);
		this.mAddressBarSV = (HorizontalScrollView) findViewById(R.id.addressBarSV);
		this.btnLinkCemetery = (Button) findViewById(R.id.btnLinkCemetery);
		this.btnLinkRegion = (Button) findViewById(R.id.btnLinkRegion);
		this.btnLinkRow = (Button) findViewById(R.id.btnLinkRow);
		this.btnLinkPlace = (Button) findViewById(R.id.btnLinkPlace);
		this.btnLinkNextPlace = (Button) findViewById(R.id.btnLinkNextPlace);
		this.btnLinkPrevPlace = (Button) findViewById(R.id.btnLinkPrevPlace);
		this.btnLinkGrave = (Button) findViewById(R.id.btnLinkGrave);	
		this.btnLinkHome = (Button) findViewById(R.id.btnLinkRoot);
		this.btnLinkHome.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();				
			}
		});
								
		btnLinkCemetery.setLinksClickable(true);
		btnLinkCemetery.setMovementMethod(new LinkMovementMethod());
		btnLinkRegion.setLinksClickable(true);
		btnLinkRegion.setMovementMethod(new LinkMovementMethod());
		btnLinkRow.setLinksClickable(true);
		btnLinkRow.setMovementMethod(new LinkMovementMethod());
		btnLinkPlace.setLinksClickable(true);
		btnLinkPlace.setMovementMethod(new LinkMovementMethod());
		btnLinkGrave.setLinksClickable(true);
		btnLinkGrave.setMovementMethod(new LinkMovementMethod());				
		
		
		this.btnLinkCemetery.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mType = AddObjectActivity.ADD_CEMETERY;
				setNewIdInExtras(EXTRA_TYPE, mType);
				updateContent(mType, mCemeteryId);
				
			}
		});
		
		this.btnLinkRegion.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mType = AddObjectActivity.ADD_REGION;
				setNewIdInExtras(EXTRA_TYPE, mType);
				updateContent(mType, mRegionId);				
			}
		});
		
		this.btnLinkRow.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mType = AddObjectActivity.ADD_ROW;
				setNewIdInExtras(EXTRA_TYPE, mType);
				updateContent(mType, mRowId);				
			}
		});
		
		this.btnLinkPlace.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if((mType & AddObjectActivity.MASK_ROW) == AddObjectActivity.MASK_ROW ){
					mType = AddObjectActivity.ADD_PLACE_WITHROW;
				} else {
					mType = AddObjectActivity.ADD_PLACE_WITHOUTROW;
				}
				setNewIdInExtras(EXTRA_TYPE, mType);
				updateContent(mType, mPlaceId);				
			}
		});
		
		this.btnLinkGrave.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if((mType & AddObjectActivity.MASK_ROW) == AddObjectActivity.MASK_ROW ){
					mType = AddObjectActivity.ADD_GRAVE_WITHROW;
				} else {
					mType = AddObjectActivity.ADD_GRAVE_WITHOUTROW;
				}
				setNewIdInExtras(EXTRA_TYPE, mType);
				updateContent(mType, mGraveId);
				
			}
		});			
		if(mSyncTaskHandler == null){
			mSyncTaskHandler = new SyncTaskHandler();
		}
		mSyncTaskHandler.setContext(this);
		mSyncTaskHandler.checkResumeDataOperation(this);
		mSyncTaskHandler.setOnSyncCompleteListener(this);
		updateContent(mType);
		ThreadManager.getInstance().setOnChangeDownloadStatus(this);
	}
	
	@Override
	public void onComplete(OperationType operationType, TaskResult taskResult) {
		updateContent(mType);
		switch (mType) {        
        case AddObjectActivity.ADD_PLACE_WITHOUTROW:
            mPlaceId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_PLACE_ID, -1);
            startDownloadThumbnails(mPlaceId);
            break;
        case AddObjectActivity.ADD_PLACE_WITHROW :
            mPlaceId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_PLACE_ID, -1);
            startDownloadThumbnails(mPlaceId);
            break;
        default:
            break;
        }
	}
		
	private void updateContent(int type){
		switch (type) {
		case AddObjectActivity.ADD_CEMETERY:
			updateContent(AddObjectActivity.ADD_CEMETERY, mCemeteryId);
			break;
		case AddObjectActivity.ADD_REGION:
			updateContent(AddObjectActivity.ADD_REGION, mRegionId);
			break;
		case AddObjectActivity.ADD_ROW:
			updateContent(AddObjectActivity.ADD_ROW, mRowId);
			break;
		case AddObjectActivity.ADD_PLACE_WITHOUTROW:
			updateContent(AddObjectActivity.ADD_PLACE_WITHOUTROW, mPlaceId);
			break;
		case AddObjectActivity.ADD_PLACE_WITHROW :
			updateContent(AddObjectActivity.ADD_PLACE_WITHROW, mPlaceId);
			break;
		case AddObjectActivity.ADD_GRAVE_WITHOUTROW:
			updateContent(AddObjectActivity.ADD_GRAVE_WITHOUTROW, mGraveId);
			break;
		case AddObjectActivity.ADD_GRAVE_WITHROW:
			updateContent(AddObjectActivity.ADD_GRAVE_WITHROW, mGraveId);
			break;
		default:
			break;
		}		
	}
	
	private int getPrevType(int currentType){
		int prevType = -1;
		switch (currentType) {
		case AddObjectActivity.ADD_CEMETERY:
			prevType = -1;
			break;
		case AddObjectActivity.ADD_REGION:
			prevType = AddObjectActivity.ADD_CEMETERY;
			break;
		case AddObjectActivity.ADD_ROW:
			prevType = AddObjectActivity.ADD_REGION;
			break;
		case AddObjectActivity.ADD_PLACE_WITHOUTROW:
			prevType = AddObjectActivity.ADD_REGION;
			break;
		case AddObjectActivity.ADD_PLACE_WITHROW :
			prevType = AddObjectActivity.ADD_ROW;
			break;
		case AddObjectActivity.ADD_GRAVE_WITHOUTROW:
			prevType = AddObjectActivity.ADD_PLACE_WITHOUTROW;
			break;
		case AddObjectActivity.ADD_GRAVE_WITHROW:
			prevType = AddObjectActivity.ADD_PLACE_WITHROW;
			break;
		default:
			break;
		}
		return prevType;
	}
	
	private SpannableString getSpanStringForLink(String linkText){
		SpannableString spanText = new SpannableString(linkText);
		spanText.setSpan(new StyleSpan(Typeface.BOLD), 0, linkText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		spanText.setSpan(new StyleSpan(Typeface.ITALIC), 0, linkText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		spanText.setSpan(new UnderlineSpan(), 0, linkText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return spanText;
	}
	
	@Override
	public void onResume(){
		super.onResume();
		this.mSyncTaskHandler.setContext(this);
				
		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		int gpsInterval = Settings.getGPSInterval(this);
        Log.i("GPSTimeUpdate", Integer.toString(gpsInterval) + " second");
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, gpsInterval * 1000, 0, this);
		if(Settings.getCurrentLocation() == null){			
			Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		    Settings.setCurrentLocation(lastKnownLocation);			
		}
		updateContent(mType);
		updateOptionsMenu();
	}
	
	@Override
	public void onBackPressed() {
		int nextType = getPrevType(this.mType);		
		if(nextType < 0){
			super.onBackPressed();
		} else {
			this.mType = nextType;
			setNewIdInExtras(EXTRA_TYPE, mType);
			updateContent(nextType);
		}
	}
	
	@Override
	public void onLocationChanged(Location location) {
		if(Settings.isBetterLocation(location, Settings.getCurrentLocation())){
    		Settings.setCurrentLocation(location);
    	}
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_browser, menu);
		this.mOptionsMenu = menu;
		this.updateOptionsMenu();
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    super.onOptionsItemSelected(item);
	    switch (item.getItemId()) {
	    case R.id.action_remove:
	    	deleteSelectedPhotos();
	    	break;
		case R.id.action_settings:
			actionSettings();
			break;
		case R.id.action_edit:			
			actionEdit();
			break;
		case R.id.action_get:
			SettingsData settingsData = Settings.getSettingData(this);
			if (!settingsData.IsAutoDownloadData){
				settingsData.IsAutoDownloadData = true;
				actionGet();
			} else {
				settingsData.IsAutoDownloadData = false;
			}
			Settings.saveSettingsData(this, settingsData);
			updateOptionsMenu();
			break;
		case R.id.action_burial_plan:
		    Intent intent = new Intent(this, BurialPlanActivity.class);
		    startActivity(intent);
		    break;
		}	    
	    return true;
	}	
	
	private void actionGet(){		
		switch (mType) {
		case AddObjectActivity.ADD_CEMETERY:
			mCemeteryId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_CEMETERY_ID, -1);
			startGetRegion(mCemeteryId);
			break;
		case AddObjectActivity.ADD_REGION:
			mRegionId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_REGION_ID, -1);
			startGetPlaceAndGraveAndBurial(mRegionId);
			break;
		case AddObjectActivity.ADD_ROW:
			mRowId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_ROW_ID, -1);
			Row row = DB.dao(Row.class).queryForId(mRowId);
			DB.dao(Region.class).refresh(row.Region);
			startGetPlaceAndGraveAndBurial(row.Region.Id);
			break;
		case AddObjectActivity.ADD_PLACE_WITHOUTROW:
			mPlaceId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_PLACE_ID, -1);
			startGetGrave(mPlaceId);
			break;
		case AddObjectActivity.ADD_PLACE_WITHROW :
			mPlaceId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_PLACE_ID, -1);
			startGetGrave(mPlaceId);
			break;
		case AddObjectActivity.ADD_GRAVE_WITHOUTROW:
			mGraveId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_GRAVE_ID, -1);
			startGetBurial(mGraveId);
			break;
		case AddObjectActivity.ADD_GRAVE_WITHROW:
			mGraveId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_GRAVE_ID, -1);
			startGetBurial(mGraveId);
			break;
		default:
			break;
		}
	}
	
	private void startGetRegion(int cemeteryId){
		if(mSyncTaskHandler != null){
			Cemetery cemetery = DB.dao(Cemetery.class).queryForId(cemeteryId);
			if(cemetery.ServerId > 0 ) {
				mSyncTaskHandler.startGetRegion(cemetery.ServerId);				
			}
		}
		
	}
	
	private void startGetPlaceAndGraveAndBurial(int regionId){
		if(mSyncTaskHandler != null){
			Region region = DB.dao(Region.class).queryForId(regionId);
			if(region.ServerId > 0){
				mSyncTaskHandler.startGetPlaceAndGraveAndBurial(region.ServerId);
			}
		}
	}
	
	private void startGetGrave(int placeId){
		if(mSyncTaskHandler != null){
			Place place = DB.dao(Place.class).queryForId(placeId);
			if(place.ServerId > 0 ){
				mSyncTaskHandler.startGetGrave(place.ServerId);				
			}
		}
	}
	
	private void startGetBurial(int graveId){
		if(mSyncTaskHandler != null){
			Grave grave = DB.dao(Grave.class).queryForId(graveId);
			if(grave.ServerId > 0 ){
				mSyncTaskHandler.startGetBurial(grave.ServerId);				
			}
		}
	}
	
	private void actionSettings(){
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}
	
	private void actionEdit(){
		Intent intent = new Intent(this, AddObjectActivity.class);
		intent.putExtra(AddObjectActivity.EXTRA_TYPE, mType);
		intent.putExtra(AddObjectActivity.EXTRA_EDIT, true);
		switch (mType) {
		case AddObjectActivity.ADD_CEMETERY:
			mCemeteryId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_CEMETERY_ID, -1);
			intent.putExtra(AddObjectActivity.EXTRA_ID, mCemeteryId);
			break;
		case AddObjectActivity.ADD_REGION:
			mRegionId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_REGION_ID, -1);
			intent.putExtra(AddObjectActivity.EXTRA_ID, mRegionId);
			break;
		case AddObjectActivity.ADD_ROW:
			mRowId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_ROW_ID, -1);
			intent.putExtra(AddObjectActivity.EXTRA_ID, mRowId);
			break;
		case AddObjectActivity.ADD_PLACE_WITHOUTROW:
			mPlaceId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_PLACE_ID, -1);
			intent.putExtra(AddObjectActivity.EXTRA_ID, mPlaceId);
			break;
		case AddObjectActivity.ADD_PLACE_WITHROW :
			mPlaceId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_PLACE_ID, -1);
			intent.putExtra(AddObjectActivity.EXTRA_ID, mPlaceId);
			break;
		case AddObjectActivity.ADD_GRAVE_WITHOUTROW:
			mGraveId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_GRAVE_ID, -1);
			intent.putExtra(AddObjectActivity.EXTRA_ID, mGraveId);			
			break;
		case AddObjectActivity.ADD_GRAVE_WITHROW:
			mGraveId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_GRAVE_ID, -1);
			intent.putExtra(AddObjectActivity.EXTRA_ID, mGraveId);			
			break;
		default:
			break;
		}
		startActivityForResult(intent, EDIT_OBJECT_REQUEST_CODE);
	}
	
	private void actionEdit(Region region){
		Intent intent = new Intent(this, AddObjectActivity.class);
		intent.putExtra(AddObjectActivity.EXTRA_TYPE, AddObjectActivity.ADD_REGION);
		intent.putExtra(AddObjectActivity.EXTRA_EDIT, true);
		intent.putExtra(AddObjectActivity.EXTRA_ID, region.Id);
		startActivityForResult(intent, EDIT_OBJECT_REQUEST_CODE);		
	}
	
	private void actionEdit(Row row){
		Intent intent = new Intent(this, AddObjectActivity.class);
		intent.putExtra(AddObjectActivity.EXTRA_TYPE, AddObjectActivity.ADD_ROW);
		intent.putExtra(AddObjectActivity.EXTRA_EDIT, true);
		intent.putExtra(AddObjectActivity.EXTRA_ID, row.Id);
		startActivityForResult(intent, EDIT_OBJECT_REQUEST_CODE);		
	}
	
	private void actionEdit(Place place){
		ComplexGrave complexGrave = new ComplexGrave();
		complexGrave.loadByPlaceId(place.Id);		
		Intent intent = new Intent(this, AddObjectActivity.class);
		if(complexGrave.Row != null){
			intent.putExtra(AddObjectActivity.EXTRA_TYPE, AddObjectActivity.ADD_PLACE_WITHROW);
		} else {
			intent.putExtra(AddObjectActivity.EXTRA_TYPE, AddObjectActivity.ADD_PLACE_WITHOUTROW);
		}
		intent.putExtra(AddObjectActivity.EXTRA_EDIT, true);
		intent.putExtra(AddObjectActivity.EXTRA_ID, place.Id);
		startActivityForResult(intent, EDIT_OBJECT_REQUEST_CODE);		
	}
	
	private void actionEdit(Grave grave){
		ComplexGrave complexGrave = new ComplexGrave();
		complexGrave.loadByGraveId(grave.Id);
		Intent intent = new Intent(this, AddObjectActivity.class);
		if(complexGrave.Row != null){
			intent.putExtra(AddObjectActivity.EXTRA_TYPE, AddObjectActivity.ADD_GRAVE_WITHROW);
		} else {
			intent.putExtra(AddObjectActivity.EXTRA_TYPE, AddObjectActivity.ADD_GRAVE_WITHOUTROW);
		}
		intent.putExtra(AddObjectActivity.EXTRA_EDIT, true);
		intent.putExtra(AddObjectActivity.EXTRA_ID, grave.Id);
		startActivityForResult(intent, EDIT_OBJECT_REQUEST_CODE);		
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
	    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.edit_context_menu, menu);	       
	    mChoosedGridViewId = v.getId();
	    mChoosedRegion = null;
	    mChoosedRowOrPlace = null;
	    mChoosedPlace = null;
	    mChoosedGrave = null;
	    boolean isMustDelete = false;
	    switch (v.getId()) {
		case R.id.gvRegions:
			Region region = (Region) this.mGVRegion.getAdapter().getItem(info.position);
			mChoosedRegion = region;
			if(region.ServerId < 0){
				isMustDelete = true;
			}
			menu.setHeaderTitle(region.Name);
			break;
		case R.id.gvRows:
			RowOrPlace rowOrPlace = (RowOrPlace) this.mGVRow.getAdapter().getItem(info.position);
			mChoosedRowOrPlace = rowOrPlace;
			if(rowOrPlace.getServerId() < 0){
				isMustDelete = true;
			}
			menu.setHeaderTitle(rowOrPlace.getName());
			break;
		case R.id.gvPlaces:
			Place place = (Place) this.mGVPlace.getAdapter().getItem(info.position);
			mChoosedPlace = place;
			if(place.ServerId < 0){
				isMustDelete = true;
			}
			menu.setHeaderTitle(place.Name);			
			break;
		case R.id.gvGraves:
			Grave grave = (Grave) this.mGVGrave.getAdapter().getItem(info.position);
			mChoosedGrave = grave;
			if(grave.ServerId < 0){
				isMustDelete = true;
			}
			menu.setHeaderTitle(grave.Name);
			break;
		default:
			break;
		}
	    if(isMustDelete){
	    	menu.getItem(1).setEnabled(true);
	    } else {
	    	menu.getItem(1).setEnabled(false);
	    }
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	    String titleConfirmDeleteDialog = null;
	    switch (mChoosedGridViewId) {
	    case R.id.gvRegions:
		    switch (item.getItemId()) {
	        case R.id.action_edit:
	            actionEdit(mChoosedRegion);
	            return true;
	        case R.id.action_remove:
	        	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    	    @Override
		    	    public void onClick(DialogInterface dialog, int which) {
		    	        switch (which){
		    	        case DialogInterface.BUTTON_POSITIVE:
		    	        	MonumentDB.deleteRegion(mChoosedRegion.Id);
		    	        	mType = getIntent().getIntExtra(EXTRA_TYPE, -1);				
		    				updateContent(mType);
		    	            break;
		    	        case DialogInterface.BUTTON_NEGATIVE:
		    	            //do nothing
		    	            break;
		    	        }
		    	    }
		    	};	
		    	AlertDialog.Builder builder = new AlertDialog.Builder(BrowserCemeteryActivity.this);
		    	titleConfirmDeleteDialog = String.format(getString(R.string.deleteItemQuestion), mChoosedRegion.Name);
		    	builder.setMessage(titleConfirmDeleteDialog).setPositiveButton(getString(R.string.yes), dialogClickListener)
		    	    .setNegativeButton(getString(R.string.no), dialogClickListener).show();	
	        	
	            return true;
	        default:
	            return super.onContextItemSelected(item);
		    }		
		case R.id.gvRows:
		    switch (item.getItemId()) {
	        case R.id.action_edit:
	        	if(mChoosedRowOrPlace.isRow()){
	        		actionEdit(mChoosedRowOrPlace.Row);
	        	} else {
	        		actionEdit(mChoosedRowOrPlace.Place);
	        	}	            
	            return true;
	        case R.id.action_remove:
	        	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    	    @Override
		    	    public void onClick(DialogInterface dialog, int which) {
		    	        switch (which){
		    	        case DialogInterface.BUTTON_POSITIVE:
		    	        	if(mChoosedRowOrPlace.isRow()){
		    	        		MonumentDB.deleteRow(mChoosedRowOrPlace.Row.Id);
		    	        	} else {
		    	        		MonumentDB.deletePlace(mChoosedRowOrPlace.Place.Id);
		    	        	}
		    	        	mType = getIntent().getIntExtra(EXTRA_TYPE, -1);				
		    				updateContent(mType);
		    	            break;
		    	        case DialogInterface.BUTTON_NEGATIVE:
		    	            //do nothing
		    	            break;
		    	        }
		    	    }
		    	};	
		    	AlertDialog.Builder builder = new AlertDialog.Builder(BrowserCemeteryActivity.this);
		    	titleConfirmDeleteDialog = String.format(getString(R.string.deleteItemQuestion), mChoosedRowOrPlace.getName());
		    	builder.setMessage(titleConfirmDeleteDialog).setPositiveButton(getString(R.string.yes), dialogClickListener)
		    	    .setNegativeButton(getString(R.string.no), dialogClickListener).show();		            
	            return true;
	        default:
	            return super.onContextItemSelected(item);
		    }			
		case R.id.gvPlaces:
		    switch (item.getItemId()) {
	        case R.id.action_edit:
	            actionEdit(mChoosedPlace);
	            return true;
	        case R.id.action_remove:
	        	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    	    @Override
		    	    public void onClick(DialogInterface dialog, int which) {
		    	        switch (which){
		    	        case DialogInterface.BUTTON_POSITIVE:
		    	        	MonumentDB.deletePlace(mChoosedPlace.Id);
		    	        	mType = getIntent().getIntExtra(EXTRA_TYPE, -1);				
		    				updateContent(mType);
		    	            break;
		    	        case DialogInterface.BUTTON_NEGATIVE:
		    	            //do nothing
		    	            break;
		    	        }
		    	    }
		    	};	
		    	AlertDialog.Builder builder = new AlertDialog.Builder(BrowserCemeteryActivity.this);
		    	titleConfirmDeleteDialog = String.format(getString(R.string.deleteItemQuestion), mChoosedPlace.Name);
		    	builder.setMessage(titleConfirmDeleteDialog).setPositiveButton(getString(R.string.yes), dialogClickListener)
		    	    .setNegativeButton(getString(R.string.no), dialogClickListener).show();	
	        	
	            return true;
	        default:
	            return super.onContextItemSelected(item);
		    }
		case R.id.gvGraves:
		    switch (item.getItemId()) {
	        case R.id.action_edit:
	            actionEdit(mChoosedGrave);
	            return true;
	        case R.id.action_remove:
	        	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    	    @Override
		    	    public void onClick(DialogInterface dialog, int which) {
		    	        switch (which){
		    	        case DialogInterface.BUTTON_POSITIVE:
		    	        	MonumentDB.deleteGrave(mChoosedGrave.Id);
		    	        	mType = getIntent().getIntExtra(EXTRA_TYPE, -1);				
		    				updateContent(mType);
		    	            break;
		    	        case DialogInterface.BUTTON_NEGATIVE:
		    	            //do nothing
		    	            break;
		    	        }
		    	    }
		    	};	
		    	AlertDialog.Builder builder = new AlertDialog.Builder(BrowserCemeteryActivity.this);
		    	titleConfirmDeleteDialog = String.format(getString(R.string.deleteItemQuestion), mChoosedGrave.Name);
		    	builder.setMessage(titleConfirmDeleteDialog).setPositiveButton(getString(R.string.yes), dialogClickListener)
		    	    .setNegativeButton(getString(R.string.no), dialogClickListener).show();	
	            return true;
	        default:
	            return super.onContextItemSelected(item);
		    }			
		default:
			break;
		}
	    return super.onContextItemSelected(item);
	}
	
	private void updateContent(int type, int id){		
		ComplexGrave complexGrave = new ComplexGrave();
		View contentView = null;		
		this.btnLinkCemetery.setBackgroundDrawable(getResources().getDrawable(R.drawable.button));
		this.btnLinkRegion.setBackgroundDrawable(getResources().getDrawable(R.drawable.button));
		this.btnLinkRow.setBackgroundDrawable(getResources().getDrawable(R.drawable.button));
		this.btnLinkPlace.setBackgroundDrawable(getResources().getDrawable(R.drawable.button));
		this.btnLinkGrave.setBackgroundDrawable(getResources().getDrawable(R.drawable.button));
		this.btnLinkPrevPlace.setVisibility(View.GONE);
		this.btnLinkNextPlace.setVisibility(View.GONE);
		switch(type){
			case AddObjectActivity.ADD_CEMETERY:
				complexGrave.loadByCemeteryId(id);
				contentView = LayoutInflater.from(this).inflate(R.layout.region_list, null);
				btnLinkCemetery.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_pressed));
				handleRegionList(contentView, id);
			break;
			case AddObjectActivity.ADD_REGION:
				complexGrave.loadByRegionId(id);
				contentView = LayoutInflater.from(this).inflate(R.layout.row_list, null);
				btnLinkRegion.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_pressed));
				handleRowList(contentView, id);
			break;
			case AddObjectActivity.ADD_ROW:
				complexGrave.loadByRowId(id);
				contentView = LayoutInflater.from(this).inflate(R.layout.place_list, null);
				btnLinkRow.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_pressed));
				if((type & AddObjectActivity.MASK_ROW) == AddObjectActivity.MASK_ROW){
					handlePlaceList(contentView, -1, id);
				} else {
					handlePlaceList(contentView, id, -1);
				}
				
			break;
			case AddObjectActivity.ADD_PLACE_WITHROW:
				complexGrave.loadByPlaceId(id);
				contentView = LayoutInflater.from(this).inflate(R.layout.grave_list, null);
				btnLinkPlace.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_pressed));
				handleGraveList(contentView, id);
			break;
			case AddObjectActivity.ADD_PLACE_WITHOUTROW:
				complexGrave.loadByPlaceId(id);
				contentView = LayoutInflater.from(this).inflate(R.layout.grave_list, null);
				btnLinkPlace.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_pressed));
				handleGraveList(contentView, id);
			break;			
		}
		updateButtonLink(complexGrave);		
		mainView.removeAllViews();
		LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		mainView.addView(contentView, params);
		if(Settings.IsAutoDownloadData(this)){
			if(mPrevType < type){
				if(type != AddObjectActivity.ADD_ROW){
					actionGet();
				}
			}
		}
		mPrevType = type;
		updateOptionsMenu();
	}
	
	private void updateButtonLink(ComplexGrave complexGrave){
		if(complexGrave.Cemetery != null){
			this.btnLinkCemetery.setText(getSpanStringForLink("Кладб.:" + complexGrave.Cemetery.Name + " "));
		} else {
			this.btnLinkCemetery.setText(null);
		}
		if(complexGrave.Region != null){
			this.btnLinkRegion.setText(getSpanStringForLink("Уч.:" + complexGrave.Region.Name + " "));
		} else {
			this.btnLinkRegion.setText(null);
		}
		if(complexGrave.Row != null){
			this.btnLinkRow.setText(getSpanStringForLink("Ряд:" + complexGrave.Row.Name + " "));
		} else {
			this.btnLinkRow.setText(null);
		}
		if(complexGrave.Place != null){
			this.btnLinkPlace.setText(getSpanStringForLink("Место:" + complexGrave.Place.Name + " "));
		} else {
			this.btnLinkPlace.setText(null);
		}
		if(complexGrave.Grave != null){
			this.btnLinkGrave.setText(getSpanStringForLink("Могила:" + complexGrave.Grave.Name + " "));
		} else {
			this.btnLinkGrave.setText(null);
		}
		mAddressBarSV.postDelayed(new Runnable() {
		    public void run() {
		        mAddressBarSV.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
		    }
		}, 100L);
	}
	
	private void handleRegionList(View contentView, int cemeteryId){
		this.btnLinkCemetery.setVisibility(View.VISIBLE);		
		this.btnLinkRegion.setVisibility(View.GONE);		
		this.btnLinkRow.setVisibility(View.GONE);
		this.btnLinkPlace.setVisibility(View.GONE);
		this.btnLinkGrave.setVisibility(View.GONE);		
		Button btnAddRegion = (Button) contentView.findViewById(R.id.btnAddRegion);
		this.mGVRegion = (GridView) contentView.findViewById(R.id.gvRegions);
		registerForContextMenu(this.mGVRegion);
		List<Region> regions = getRegions(cemeteryId);
		RegionGridAdapter regionGridAdapter = new RegionGridAdapter(regions);
		mGVRegion.setAdapter(regionGridAdapter);
		btnAddRegion.setOnClickListener( new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(BrowserCemeteryActivity.this, AddObjectActivity.class);
				intent.putExtra(AddObjectActivity.EXTRA_TYPE, AddObjectActivity.ADD_REGION);
				intent.putExtra(AddObjectActivity.EXTRA_PARENT_ID, mCemeteryId);
				startActivityForResult(intent, ADD_OBJECT_REQUEST_CODE);							
			}
		});
		mGVRegion.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				Region region = (Region)mGVRegion.getAdapter().getItem(pos);
				mRegionId = region.Id;
				mType = AddObjectActivity.ADD_REGION;
				setNewIdInExtras(EXTRA_TYPE, mType);
				setNewIdInExtras(EXTRA_REGION_ID, mRegionId);
				updateContent(mType, mRegionId);
				
			}
		});		
		
	}
	
	private void handleRowList(View contentView, int regionId){
		this.btnLinkCemetery.setVisibility(View.VISIBLE);
		this.btnLinkRegion.setVisibility(View.VISIBLE);
		this.btnLinkRow.setVisibility(View.GONE);
		this.btnLinkPlace.setVisibility(View.GONE);
		this.btnLinkGrave.setVisibility(View.GONE);
		Button btnAddRow = (Button) contentView.findViewById(R.id.btnAddRow);
		Button btnAddPlace = (Button) contentView.findViewById(R.id.btnAddPlace);
		this.mGVRow = (GridView) contentView.findViewById(R.id.gvRows);
		registerForContextMenu(this.mGVRow);
		List<RowOrPlace> rowOrPlaceList = getRowsOrPlaces(regionId);
		RowGridAdapter rowGridAdapter = new RowGridAdapter(rowOrPlaceList);
		mGVRow.setAdapter(rowGridAdapter);
		btnAddRow.setOnClickListener( new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(BrowserCemeteryActivity.this, AddObjectActivity.class);
				intent.putExtra(AddObjectActivity.EXTRA_TYPE, AddObjectActivity.ADD_ROW);
				intent.putExtra(AddObjectActivity.EXTRA_PARENT_ID, mRegionId);
				startActivityForResult(intent, ADD_OBJECT_REQUEST_CODE);							
			}
		});
		
		btnAddPlace.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(BrowserCemeteryActivity.this, AddObjectActivity.class);
				intent.putExtra(AddObjectActivity.EXTRA_TYPE, AddObjectActivity.ADD_PLACE_WITHOUTROW);
				intent.putExtra(AddObjectActivity.EXTRA_PARENT_ID, mRegionId);
				startActivityForResult(intent, ADD_OBJECT_REQUEST_CODE);				
			}
		});
		
		mGVRow.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				RowOrPlace rowOrPlace = (RowOrPlace) mGVRow.getAdapter().getItem(pos);
				if(rowOrPlace.isRow()){
					mRowId = rowOrPlace.Row.Id;
					mType = AddObjectActivity.ADD_ROW;
					setNewIdInExtras(EXTRA_TYPE, mType);
					setNewIdInExtras(EXTRA_ROW_ID, mRowId);
					updateContent(mType, mRowId);
				} else {
					mPlaceId = rowOrPlace.Place.Id;
					mRowId = -1;
					mType = AddObjectActivity.ADD_PLACE_WITHOUTROW;
					setNewIdInExtras(EXTRA_TYPE, mType);
					setNewIdInExtras(EXTRA_ROW_ID, mRowId);
					setNewIdInExtras(EXTRA_PLACE_ID, mPlaceId);
					updateContent(mType, mPlaceId);
				}				
			}
		});				
	}
	
	private void handlePlaceList(View contentView, int regionId, int rowId){
		this.btnLinkCemetery.setVisibility(View.VISIBLE);
		this.btnLinkRegion.setVisibility(View.VISIBLE);
		if((mType & AddObjectActivity.MASK_ROW) == AddObjectActivity.MASK_ROW){
			this.btnLinkRow.setVisibility(View.VISIBLE);
		} else {
			this.btnLinkRow.setVisibility(View.GONE);
		}
		this.btnLinkPlace.setVisibility(View.GONE);
		this.btnLinkGrave.setVisibility(View.GONE);
		Button btnAddPlace = (Button) contentView.findViewById(R.id.btnAddPlace);
		Button btnInvertPlaceOrder = (Button) contentView.findViewById(R.id.btnInvertPlaceOrder);
		this.mGVPlace = (GridView) contentView.findViewById(R.id.gvPlaces);
		registerForContextMenu(this.mGVPlace);
		List<Place> places = getPlaces(rowId);
		PlaceGridAdapter placeGridAdapter = new PlaceGridAdapter(places);
		mGVPlace.setAdapter(placeGridAdapter);
		btnAddPlace.setOnClickListener( new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(BrowserCemeteryActivity.this, AddObjectActivity.class);
				if((mType & AddObjectActivity.MASK_ROW) == AddObjectActivity.MASK_ROW){
					intent.putExtra(AddObjectActivity.EXTRA_TYPE, AddObjectActivity.ADD_PLACE_WITHROW);
				} else {
					intent.putExtra(AddObjectActivity.EXTRA_TYPE, AddObjectActivity.ADD_PLACE_WITHOUTROW);
				}
				intent.putExtra(AddObjectActivity.EXTRA_PARENT_ID, mRowId);
				startActivityForResult(intent, ADD_OBJECT_REQUEST_CODE);						
			}
		});
		
		boolean isPossibleInvertOrder = isPossibleInvertPlaceOrder(rowId);
		btnInvertPlaceOrder.setEnabled(isPossibleInvertOrder);
		btnInvertPlaceOrder.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				invertPlaceOrder(mRowId);
				updateContent(AddObjectActivity.ADD_ROW, mRowId);
			}
		});
		
		mGVPlace.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				Place place = (Place) mGVPlace.getAdapter().getItem(pos);				
				if(place.Row != null){
					mType = AddObjectActivity.ADD_PLACE_WITHROW;
				} else {
					mType = AddObjectActivity.ADD_PLACE_WITHOUTROW;
				}
				mPlaceId = place.Id;
				setNewIdInExtras(EXTRA_TYPE, mType);
				setNewIdInExtras(EXTRA_PLACE_ID, mPlaceId);				
				updateContent(mType, mPlaceId);				
			}
		});		
		
	}
	
	private boolean isPossibleInvertPlaceOrder(int rowId){
		boolean result = false;
		try {
	        long clientPlaceCount = DB.dao(Place.class).countOf(DB.dao(Place.class).queryBuilder().setCountOf(true).where().eq(Place.ROW_ID_COLUMN, rowId).and().eq(BaseDTO.COLUMN_SERVER_ID, BaseDTO.INT_NULL_VALUE).and().eq(BaseDTO.COLUMN_IS_CHANGED, 1).prepare());
	        long totalPlaceCount = DB.dao(Place.class).countOf(DB.dao(Place.class).queryBuilder().setCountOf(true).where().eq(Place.ROW_ID_COLUMN, rowId).prepare());
	        if(clientPlaceCount == totalPlaceCount && clientPlaceCount > 1){
	        	result = true;
	        }
        } catch (SQLException e) {	        
	        e.printStackTrace();
        }
		return result;
	}
	
	private void invertPlaceOrder(int rowId){
		try {
			boolean isPosibleInvertOrder = isPossibleInvertPlaceOrder(rowId);
			if(isPosibleInvertOrder){
				List<Place> listPlace = DB.dao(Place.class).queryBuilder().orderByRaw(BaseDTO.ORDER_BY_COLUMN_NAME).where().eq(Place.ROW_ID_COLUMN, rowId).query();
				String[] arrayNames = new String[listPlace.size()];
	        	for(int i = 0; i < listPlace.size(); i++){
	        		arrayNames[i] = listPlace.get(i).Name;
	        	}
	        	for(int i = 0; i < listPlace.size(); i++){
	        		int j = listPlace.size() - 1 - i;
	        		listPlace.get(i).Name = arrayNames[j];
	        		DB.dao(Place.class).update(listPlace.get(i));
	        	}
	        	Toast.makeText(this, "Порядок мест изменен", Toast.LENGTH_LONG).show();
			}
        } catch (SQLException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
	}
	
	private void handleGraveList(View contentView, int placeId){	    
		this.btnLinkCemetery.setVisibility(View.VISIBLE);
		this.btnLinkRegion.setVisibility(View.VISIBLE);
		if((mType & AddObjectActivity.MASK_ROW) == AddObjectActivity.MASK_ROW){
			this.btnLinkRow.setVisibility(View.VISIBLE);
		} else {
			this.btnLinkRow.setVisibility(View.GONE);
		}
		this.btnLinkPlace.setVisibility(View.VISIBLE);
		this.btnLinkGrave.setVisibility(View.GONE);
		
		this.btnLinkPrevPlace.setVisibility(View.VISIBLE);
		
		this.btnLinkNextPlace.setVisibility(View.VISIBLE);
		this.btnLinkNextPlace.setText("Следующее>>");
		ArrayList<Place> prevAndNextPlaces =  this.getPrevAndNextPlaceInRow(placeId);
		Place prevPlace = prevAndNextPlaces.get(0);
		Place nextPlace = prevAndNextPlaces.get(1);
		if(prevPlace != null){
		    this.btnLinkPrevPlace.setText(Html.fromHtml(String.format("<< Предыдущее<br/><u> № %s</u>", prevPlace.Name)));
		    this.btnLinkPrevPlace.setTag(prevPlace.Id);
		    this.btnLinkPrevPlace.setEnabled(true);
		} else {
		    this.btnLinkPrevPlace.setText("<< Предыдущее");
		    this.btnLinkPrevPlace.setTag(null);
            this.btnLinkPrevPlace.setEnabled(false);
		}
		if(nextPlace != null){
		    this.btnLinkNextPlace.setText(Html.fromHtml(String.format("Следующее >><br/><u> № %s</u>", nextPlace.Name)));
		    this.btnLinkNextPlace.setTag(nextPlace.Id);
		    this.btnLinkNextPlace.setEnabled(true);
		} else {
		    this.btnLinkNextPlace.setText("Следующее >>");
		    this.btnLinkNextPlace.setTag(null);
            this.btnLinkNextPlace.setEnabled(false);		    
		}
		
		View.OnClickListener btnLinkPrevAndNextPlaceListener = new View.OnClickListener() {            
            @Override
            public void onClick(View v) {                
                if(v.getTag() != null){
                    int placeId = (Integer) v.getTag();
                    mType = getIntent().getExtras().getInt(EXTRA_TYPE);
                    mPlaceId = placeId;
                    setNewIdInExtras(EXTRA_PLACE_ID, placeId);             
                    updateContent(mType, mPlaceId);     
                    
                }                
            }
        };
		this.btnLinkNextPlace.setOnClickListener(btnLinkPrevAndNextPlaceListener);
		this.btnLinkPrevPlace.setOnClickListener(btnLinkPrevAndNextPlaceListener);
		
		Button btnAddGrave = (Button) contentView.findViewById(R.id.btnAddGrave);
		Button btnAddNewGrave = (Button) contentView.findViewById(R.id.btnAddNewGrave);
		this.mGVGrave = (GridView) contentView.findViewById(R.id.gvGraves);
		this.etPlaceLength = (EditText) contentView.findViewById(R.id.etPlaceLength);
		this.etPlaceWidth = (EditText) contentView.findViewById(R.id.etPlaceWidth);
		
		this.cbPlaceMilitary = (CheckBox) contentView.findViewById(R.id.cb_place_is_military);
		this.cbPlaceUnowned = (CheckBox) contentView.findViewById(R.id.cb_place_is_unowner);
		this.cbPlaceWrongFIO = (CheckBox) contentView.findViewById(R.id.cb_place_is_wrong_fio);
		this.cbPlaceSizeVioleted = (CheckBox) contentView.findViewById(R.id.cb_place_is_size_violated);
		this.cbPlaceUnindentified = (CheckBox) contentView.findViewById(R.id.cb_place_is_unindentified);
		
		this.tvResponsiblePersonOfPlace = (TextView) contentView.findViewById(R.id.tvPlaceResponsiblePerson);
		this.btnMakePlacePhoto = (Button) contentView.findViewById(R.id.btnMakePhoto);
		this.btnMakePlacePhotoNextPlace = (Button) contentView.findViewById(R.id.btnMakePhotoNextPlace);
		
		registerForContextMenu(this.mGVGrave);
		setColumnWidthForPhoto();
        updatePhotoGridItems();
		List<Grave> graves = getGraves(placeId);
		this.mGraveGridAdapter = new GraveGridAdapter(graves);
		mGVGrave.setAdapter(this.mGraveGridAdapter);
		
		this.mGVGrave.setOnItemClickListener(new OnItemClickListener() {            
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id ) {
            	int photoPosition = mGraveGridAdapter.getPositionPhoto(position);
            	if(photoPosition >= 0){
            		PhotoGridItem item = gridPhotoItems.get(photoPosition);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri imageUri = item.getImageUri();
                    if(imageUri == null){
                        imageUri = item.getThumbnailUri();
                    }
                    intent.setDataAndType(imageUri, "image/*");
                    startActivity(intent); 
            	}
                             
            }
        });
        this.mGVGrave.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
            	int photoPosition = mGraveGridAdapter.getPositionPhoto(position);
            	if(photoPosition >= 0){
            		gridPhotoItems.get(photoPosition).setChecked(!gridPhotoItems.get(photoPosition).isChecked());
                    ((BaseAdapter)mGVGrave.getAdapter()).notifyDataSetChanged();              
                    MenuItem actionRemoveMenuItem = BrowserCemeteryActivity.this.mOptionsMenu.findItem(R.id.action_remove);
                    actionRemoveMenuItem.setEnabled(mGraveGridAdapter.isChoosePhoto());
                    return true;
            	}
            	return false;
                
            }
        });        
        if(BrowserCemeteryActivity.this.mOptionsMenu != null){
            MenuItem actionRemoveMenuItem = BrowserCemeteryActivity.this.mOptionsMenu.findItem(R.id.action_remove);
            actionRemoveMenuItem.setEnabled(mGraveGridAdapter.isChoosePhoto());
        }
		
		btnAddGrave.setOnClickListener( new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(BrowserCemeteryActivity.this, AddObjectActivity.class);
				if((mType & AddObjectActivity.MASK_ROW) == AddObjectActivity.MASK_ROW){
					intent.putExtra(AddObjectActivity.EXTRA_TYPE, AddObjectActivity.ADD_GRAVE_WITHROW);
				} else {
					intent.putExtra(AddObjectActivity.EXTRA_TYPE, AddObjectActivity.ADD_GRAVE_WITHOUTROW);
				}
				intent.putExtra(AddObjectActivity.EXTRA_PARENT_ID, mPlaceId);
				startActivityForResult(intent, ADD_OBJECT_REQUEST_CODE);
			}
		});
		
		btnAddNewGrave.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Place dbPlace = DB.dao(Place.class).queryForId(mPlaceId);
                Grave newGrave = new Grave();
                newGrave.Place = dbPlace;
                newGrave.IsChanged = 1;
                newGrave.Name = null;
                DB.dao(Grave.class).create(newGrave);
                ((GraveGridAdapter) mGVGrave.getAdapter()).addGrave(newGrave);
            }
        });
		
		this.btnMakePlacePhoto.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View view) {
                mMakePhotoType = PHOTOTYPE.PLACEPHOTO_CURRENT;
                if(isMayMakePhoto()){
                    makePlacePhotoCurrent();
                }
                
            }
        });
		
		this.btnMakePlacePhotoNextPlace.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                mMakePhotoType = PHOTOTYPE.PLACEPHOTO_NEXTPLACE;
                if(isMayMakePhoto()){
                    if(Settings.IsOldPlaceNameOption(BrowserCemeteryActivity.this)){
                        enterOldPlaceName();
                    } else {
                        makePlacePhotoNextPlace(null);
                    }
                }
                
            }
        });
		
		Place place = DB.dao(Place.class).queryForId(placeId);
		this.cbPlaceUnowned.setChecked(place.isUnowned());
		this.cbPlaceMilitary.setChecked(place.isMilitary());
		this.cbPlaceWrongFIO.setChecked(place.isWrongFIO());
		this.cbPlaceUnindentified.setChecked(place.isUnindentified());
		this.cbPlaceSizeVioleted.setChecked(place.isSizeViolated());
		if(place.Width != null){
		    this.etPlaceWidth.setText(place.Width.toString());
		} else {
		    this.etPlaceWidth.setText("");
		}
		if(place.Length != null){
            this.etPlaceLength.setText(place.Length.toString());
        } else {
            this.etPlaceLength.setText("");
        }
		if(place.ResponsibleUser != null){
			DB.dao(ResponsibleUser.class).refresh(place.ResponsibleUser);
			String fio = String.format("%s %s %s", 
				place.ResponsibleUser.LastName != null ? place.ResponsibleUser.LastName : "",
				place.ResponsibleUser.FirstName != null ? place.ResponsibleUser.FirstName : "",
				place.ResponsibleUser.MiddleName != null ? place.ResponsibleUser.MiddleName : "");
			String phone = place.ResponsibleUser.Phones != null ? place.ResponsibleUser.Phones : "";
			StringBuilder sbAddress = new StringBuilder();
			if(place.ResponsibleUser.City != null){
				sbAddress.append(place.ResponsibleUser.City);
				sbAddress.append(", ");
			}
			if(place.ResponsibleUser.Street != null){
				sbAddress.append(place.ResponsibleUser.Street);
				sbAddress.append(", ");
			}
			
			if(place.ResponsibleUser.House != null){
				sbAddress.append(String.format("д. %s", place.ResponsibleUser.House));
				sbAddress.append(", ");
			}
			if(place.ResponsibleUser.Block != null){
				sbAddress.append(String.format("к. %s", place.ResponsibleUser.Block));
				sbAddress.append(", ");
			}
			if(place.ResponsibleUser.Building != null){
				sbAddress.append(String.format("стр. %s", place.ResponsibleUser.Building));
				sbAddress.append(", ");
			}
			if(place.ResponsibleUser.Flat != null){
				sbAddress.append(String.format("кв. %s", place.ResponsibleUser.Flat));				
			}
			String address = String.format("%s %s %s", fio, phone, sbAddress.toString());
			tvResponsiblePersonOfPlace.setText(address);					
		} else {
			tvResponsiblePersonOfPlace.setText("");
		}
		
		this.cbPlaceUnowned.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Place place = DB.dao(Place.class).queryForId(getIntent().getIntExtra(EXTRA_PLACE_ID, -1));
                Date unownedDate = null;
                if(isChecked){
                    unownedDate = new Date();
                }
                place.UnownedDate = unownedDate;
                place.IsOwnerLess  = isChecked;
                place.IsChanged = 1;
                DB.dao(Place.class).update(place);
                place.toLog(mFileLog, LogOperation.UPDATE);
                
            }
        });
        
        this.cbPlaceMilitary.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Place place = DB.dao(Place.class).queryForId(getIntent().getIntExtra(EXTRA_PLACE_ID, -1));
                Date militaryDate = null;
                if(isChecked){
                    militaryDate = new Date();
                }
                place.MilitaryDate  = militaryDate;
                place.IsChanged = 1;
                DB.dao(Place.class).update(place);
                place.toLog(mFileLog, LogOperation.UPDATE);
            }
        });
        
        this.cbPlaceWrongFIO.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Place place = DB.dao(Place.class).queryForId(getIntent().getIntExtra(EXTRA_PLACE_ID, -1));
                Date wrongFIODate = null;
                if(isChecked){
                    wrongFIODate = new Date();
                }
                place.WrongFIODate  = wrongFIODate;
                place.IsChanged = 1;
                DB.dao(Place.class).update(place);
                place.toLog(mFileLog, LogOperation.UPDATE);
                
            }
        });
        
        this.cbPlaceSizeVioleted.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Place place = DB.dao(Place.class).queryForId(getIntent().getIntExtra(EXTRA_PLACE_ID, -1));
                Date sizeVioletedate = null;
                if(isChecked){
                    sizeVioletedate = new Date();
                }
                place.SizeViolatedDate  = sizeVioletedate;
                place.IsChanged = 1;
                DB.dao(Place.class).update(place);
                place.toLog(mFileLog, LogOperation.UPDATE);
                
            }
        });
        
        this.cbPlaceUnindentified.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Place place = DB.dao(Place.class).queryForId(getIntent().getIntExtra(EXTRA_PLACE_ID, -1));
                Date unindentifiedDate = null;
                if(isChecked){
                    unindentifiedDate = new Date();
                }
                place.UnindentifiedDate  = unindentifiedDate;
                place.IsChanged = 1;
                DB.dao(Place.class).update(place);
                place.toLog(mFileLog, LogOperation.UPDATE);
            }
        });
        
        this.etPlaceWidth.addTextChangedListener(new TextWatcher() {
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub                
            }
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub                
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                Place place = DB.dao(Place.class).queryForId(getIntent().getIntExtra(EXTRA_PLACE_ID, -1));
                Double width = null;
                if(!TextUtils.isEmpty(s)){
                    try{
                        width = Double.parseDouble(s.toString());
                    }catch(NumberFormatException exc){
                        width = null;
                    }                    
                }
                place.Width = width;
                place.IsChanged = 1;
                DB.dao(Place.class).update(place);
                place.toLog(mFileLog, LogOperation.UPDATE);
            }
        });
        
        this.etPlaceLength.addTextChangedListener(new TextWatcher() {
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub                
            }
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub                
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                Place place = DB.dao(Place.class).queryForId(getIntent().getIntExtra(EXTRA_PLACE_ID, -1));
                Double length = null;
                if(!TextUtils.isEmpty(s)){
                    try{
                        length = Double.parseDouble(s.toString());
                    }catch(NumberFormatException exc){
                        length = null;
                    }                    
                }
                place.Length = length;
                place.IsChanged = 1;
                DB.dao(Place.class).update(place);
                place.toLog(mFileLog, LogOperation.UPDATE);
            }
        });
		
	}
	
	public boolean isMayMakePhoto(){
	    if(mIsCheckGPS){
            boolean enableGPS = Settings.checkWorkOfGPS(BrowserCemeteryActivity.this, BrowserCemeteryActivity.this);
            if(enableGPS == false){
                mIsCheckGPS = false;
                return false;
            }
        }
	    return true;
	}
		
	public void setNewIdInExtras(String extraName, int id){
		getIntent().removeExtra(extraName);
		getIntent().putExtra(extraName, id);
	}
	
	private List<Region> getRegions(int cemeteryId){
		RuntimeExceptionDao<Region, Integer> regionDAO = DB.dao(Region.class);
    	QueryBuilder<Region, Integer> regionBuilder = regionDAO.queryBuilder();
    	List<Region> regionList = new ArrayList<Region>();
    	try {
			regionBuilder.orderByRaw(BaseDTO.ORDER_BY_COLUMN_NAME).where().eq("Cemetery_id", cemeteryId);
			regionList = regionDAO.query(regionBuilder.prepare());		
		} catch (SQLException e) {
			this.mFileLog.error(Settings.UNEXPECTED_ERROR_MESSAGE, e);
		}
		return regionList;
	}
	
	private List<RowOrPlace> getRowsOrPlaces(int regionId){
		RuntimeExceptionDao<Row, Integer> rowDAO = DB.dao(Row.class);
		RuntimeExceptionDao<Place, Integer> placeDAO = DB.dao(Place.class);
    	QueryBuilder<Row, Integer> rowBuilder = rowDAO.queryBuilder();
    	QueryBuilder<Place, Integer> placeBuilder = placeDAO.queryBuilder();
    	List<Row> rows = new ArrayList<Row>();
    	List<Place> places = new ArrayList<Place>();
    	try {    		
			rowBuilder.orderByRaw(BaseDTO.ORDER_BY_COLUMN_NAME).where().eq(Place.REGION_ID_COLUMN, regionId);
			rows = rowDAO.query(rowBuilder.prepare());
			placeBuilder.orderByRaw(BaseDTO.ORDER_BY_COLUMN_NAME).where().eq(Place.REGION_ID_COLUMN, regionId);
			places = placeDAO.query(placeBuilder.prepare());
		} catch (SQLException e) {
			this.mFileLog.error(Settings.UNEXPECTED_ERROR_MESSAGE, e);
		}		
		RowGridAdapter instance = new RowGridAdapter(null);
		List<RowOrPlace> list = new ArrayList<BrowserCemeteryActivity.RowGridAdapter.RowOrPlace>();
		for(Row row : rows){
			RowOrPlace rowOrPlace = instance.createRowOrPlace();
			rowOrPlace.Row = row;
			list.add(rowOrPlace);
		}
		for(Place place : places){
			RowOrPlace rowOrPlace = instance.createRowOrPlace();
			rowOrPlace.Place = place;
			list.add(rowOrPlace);
		}
		return list;
	}
	
	private List<Place> getPlaces(int rowId){
		RuntimeExceptionDao<Place, Integer> placeDAO = DB.dao(Place.class);
    	QueryBuilder<Place, Integer> placeBuilder = placeDAO.queryBuilder();
    	List<Place> placeList = new ArrayList<Place>();
    	try {
			placeBuilder.orderByRaw(BaseDTO.ORDER_BY_COLUMN_NAME).where().eq(Place.ROW_ID_COLUMN, rowId);
			placeList = placeDAO.query(placeBuilder.prepare());		
		} catch (SQLException e) {
			this.mFileLog.error(Settings.UNEXPECTED_ERROR_MESSAGE, e);
		}
		return placeList;
	}
	
	private ArrayList<Place> getPrevAndNextPlaceInRow(int currentPlaceId){
	    ArrayList<Place> resultList = new ArrayList<Place>();
	    Place nextPlace = null;
	    Place prevPlace = null;	    
        RuntimeExceptionDao<Place, Integer> placeDAO = DB.dao(Place.class);
        Place currentPlace = placeDAO.queryForId(currentPlaceId);
        QueryBuilder<Place, Integer> placePrevBuilder = placeDAO.queryBuilder();
        QueryBuilder<Place, Integer> placeNextBuilder = placeDAO.queryBuilder();
        List<Place> placeList = null;
        
        try {            
            if(currentPlace.Row != null){
                placePrevBuilder.limit(1).orderByRaw(BaseDTO.ORDER_BY_DESC_COLUMN_NAME).where().eq(Place.ROW_ID_COLUMN, currentPlace.Row.Id).and().raw(String.format("CAST (Name As INTEGER) < CAST ('%s' As INTEGER)", currentPlace.Name));
            } else {
                placePrevBuilder.limit(1).orderByRaw(BaseDTO.ORDER_BY_DESC_COLUMN_NAME).where().eq(Place.REGION_ID_COLUMN, currentPlace.Region.Id).and().raw(String.format("CAST (Name As INTEGER) < CAST ('%s' As INTEGER)", currentPlace.Name));
            }
            placeList = placeDAO.query(placePrevBuilder.prepare()); 
            if(placeList.size() > 0){
                prevPlace = placeList.get(0);
            }
            if(currentPlace.Row != null) {
                placeNextBuilder.limit(1).orderByRaw(BaseDTO.ORDER_BY_COLUMN_NAME).where().eq(Place.ROW_ID_COLUMN, currentPlace.Row.Id).and().raw(String.format("CAST (Name As INTEGER) > CAST ('%s' As INTEGER)", currentPlace.Name));
            } else {
                placeNextBuilder.limit(1).orderByRaw(BaseDTO.ORDER_BY_COLUMN_NAME).where().eq(Place.REGION_ID_COLUMN, currentPlace.Region.Id).and().raw(String.format("CAST (Name As INTEGER) > CAST ('%s' As INTEGER)", currentPlace.Name));
            }           
            placeList = placeDAO.query(placeNextBuilder.prepare());
            if(placeList.size() > 0){
                nextPlace = placeList.get(0);
            }            
        } catch (SQLException e) {
            this.mFileLog.error(Settings.UNEXPECTED_ERROR_MESSAGE, e);
        }
        resultList.add(prevPlace);
        resultList.add(nextPlace);
        return resultList;
    }
	
	private List<Grave> getGraves(int placeId){
		RuntimeExceptionDao<Grave, Integer> graveDAO = DB.dao(Grave.class);
    	QueryBuilder<Grave, Integer> graveBuilder = graveDAO.queryBuilder();
    	List<Grave> graveList = new ArrayList<Grave>();
    	try {
			graveBuilder.orderByRaw(BaseDTO.ORDER_BY_COLUMN_NAME).where().eq("Place_id", placeId);
			graveList = graveDAO.query(graveBuilder.prepare());		
		} catch (SQLException e) {
			this.mFileLog.error(Settings.UNEXPECTED_ERROR_MESSAGE, e);
		}
		return graveList;
	}

	public class RegionGridAdapter extends BaseAdapter {
		
		private List<Region> mItems;
		
        public RegionGridAdapter(List<Region> items) {
        	this.mItems = items;
        }        

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
            	LayoutInflater inflater = (LayoutInflater) BrowserCemeteryActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.region_item, parent, false);
            }
            ImageView ivIcon = (ImageView) convertView.findViewById(R.id.ivRegion);
            TextView tvRegion = (TextView) convertView.findViewById(R.id.tvRegion);
            TextView tvRegionSquare = (TextView) convertView.findViewById(R.id.tvRegionSquare);
            Region region = mItems.get(position);
            tvRegion.setText(region.Name);
            if(region.Square != null){
            	tvRegionSquare.setText(String.format("%s м2", Double.toString(region.Square)));
            } else {
            	tvRegionSquare.setText(null);
            }
            return convertView;
        }
        
        public final int getCount() {
            return mItems.size();
        }

        public final Object getItem(int position) {
            return mItems.get(position);
        }

        public final long getItemId(int position) {
            return position;
        }
    }
	
	public class RowGridAdapter extends BaseAdapter {
		
		public RowOrPlace createRowOrPlace(){
			return new RowOrPlace();
		}
		
		public class RowOrPlace{
			
			public Row Row;
			
			public Place Place;
			
			public RowOrPlace(){
				
			}
			
			public int getServerId(){
				if(isRow()){
					return this.Row.ServerId;
				}
				if(isPlace()){
					return this.Place.ServerId;
				}
				return -1;
			}
			
			public String getName(){
				if(this.Row != null){
					return this.Row.Name;
				}
				if(this.Place != null){
					return this.Place.Name;
				}
				return null;
			}
			
			public boolean isRow(){
				return this.Row != null;
			}
			
			public boolean isPlace(){
				return this.Place != null;
			}
			
		}
		
		private List<RowOrPlace> mItems;
		
        public RowGridAdapter(List<RowOrPlace> items) {
        	this.mItems = items;
        }        

        public View getView(int position, View convertView, ViewGroup parent) {
        	RowOrPlace rowOrPlace = mItems.get(position);            
        	LayoutInflater inflater = (LayoutInflater) BrowserCemeteryActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        	if(rowOrPlace.isRow()){
        		convertView = inflater.inflate(R.layout.row_item, parent, false);
        	} else {
        		convertView = inflater.inflate(R.layout.place_item, parent, false);
        	}        
            if(rowOrPlace.isRow()){
            	ImageView ivIcon = (ImageView) convertView.findViewById(R.id.ivRow);
            	TextView tvRow = (TextView) convertView.findViewById(R.id.tvRow);
            	tvRow.setText(rowOrPlace.getName());
            } else {
            	ImageView ivIcon = (ImageView) convertView.findViewById(R.id.ivPlace);
                TextView tvPlace = (TextView) convertView.findViewById(R.id.tvPlace);
                TextView tbOwnerLess = (TextView) convertView.findViewById(R.id.tvOwnerLess);
                tvPlace.setText(rowOrPlace.getName());
                if(rowOrPlace.Place.IsOwnerLess){
                	tbOwnerLess.setVisibility(View.VISIBLE);
                	tvPlace.setTextColor(getResources().getColor(R.color.ownerless_color));            	
                	tbOwnerLess.setTextColor(getResources().getColor(R.color.ownerless_color));
                } else {
                	tbOwnerLess.setVisibility(View.GONE);
                	tvPlace.setTextColor(getResources().getColor(R.color.text_view_color));            	
                	tbOwnerLess.setTextColor(getResources().getColor(R.color.text_view_color));
                }
            }
            return convertView;
        }
        
        public final int getCount() {
            return mItems.size();
        }

        public final Object getItem(int position) {
            return mItems.get(position);
        }

        public final long getItemId(int position) {
            return position;
        }
    }
	
	public class PlaceGridAdapter extends BaseAdapter {
		
		private List<Place> mItems;
		
        public PlaceGridAdapter(List<Place> items) {
        	this.mItems = items;
        }        

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
            	LayoutInflater inflater = (LayoutInflater) BrowserCemeteryActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.place_item, parent, false);
            }
            ImageView ivIcon = (ImageView) convertView.findViewById(R.id.ivPlace);
            TextView tvPlace = (TextView) convertView.findViewById(R.id.tvPlace);
            TextView tbOwnerLess = (TextView) convertView.findViewById(R.id.tvOwnerLess); 
            Place place = mItems.get(position);
            tvPlace.setText(place.Name);
            if(place.IsOwnerLess){
            	tbOwnerLess.setVisibility(View.VISIBLE);            	
            } else {
            	tbOwnerLess.setVisibility(View.GONE);
            }
            return convertView;
        }
        
        public final int getCount() {
            return mItems.size();
        }

        public final Object getItem(int position) {
            return mItems.get(position);
        }

        public final long getItemId(int position) {
            return position;
        }
    }
	
	public class GraveGridAdapter extends BaseAdapter {
		
		private List<Grave> mItems;
		
        public GraveGridAdapter(List<Grave> items) {
        	this.mItems = items;
        }
        
        public int getPositionPhoto(int position){
        	if(position < mItems.size()){
        		return -1;
        	} else {
        		return position - mItems.size(); 
        	}
        }
        
        public void addGrave(Grave grave){
            this.mItems.add(grave);
            this.notifyDataSetChanged();
        }
        
        public View getView(int position, View convertView, ViewGroup parent) {
        	if(position < mItems.size()){
        		return getGraveView(position, convertView, parent);
        	} else {
        		return getPhotoView(position, convertView, parent);
        	}
        }

        public View getGraveView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) BrowserCemeteryActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.grave_item, parent, false);
            Grave grave = mItems.get(position);
            ImageView ivIcon = (ImageView) convertView.findViewById(R.id.ivGrave);
            TextView tvGrave = (TextView) convertView.findViewById(R.id.tvGrave);
            TextView tvFIO = (TextView) convertView.findViewById(R.id.tvFIO);
            Button btnBindBurial = (Button) convertView.findViewById(R.id.btnBind);            
            btnBindBurial.setTag(grave.Id);            
            if(grave.Name != null){
                tvGrave.setText(grave.Name);
            } else {
                tvGrave.setText(R.string.new_grave_name);
            }            
            btnBindBurial.setOnClickListener(new View.OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    int graveId = (Integer) v.getTag();
                    Intent intent = new Intent(BrowserCemeteryActivity.this, BurialPlanActivity.class);
                    intent.putExtra(BurialPlanActivity.EXTRA_GRAVE_ID, graveId);
                    startActivity(intent);                    
                }
            });
            
            tvFIO.setText(Html.fromHtml(getGraveItemText(grave.Id)));
            convertView.setLayoutParams(new GridView.LayoutParams(GridView.AUTO_FIT, Settings.THUMBNAIL_SIZE));
            return convertView;
        }
        
        public View getPhotoView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) BrowserCemeteryActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.photo_grid_item, parent, false);
            ImageView ivPhoto = (ImageView) convertView.findViewById(R.id.ivPhoto);
            ImageView ivPhotoChoose = (ImageView) convertView.findViewById(R.id.ivChoosePhoto);
            ImageView ivIsSend = (ImageView) convertView.findViewById(R.id.ivStatus);
            TextView tvGPS = (TextView) convertView.findViewById(R.id.tvGPS);
            ProgressBar pbDownload = (ProgressBar) convertView.findViewById(R.id.pbDownload);
            TextView tvDownloadStatus = (TextView) convertView.findViewById(R.id.tvStatusDownload);
            PhotoGridItem item = gridPhotoItems.get(position - mItems.size());
            item.setImageView(ivPhoto);            
            if(item.isChecked()) {
            	ivPhotoChoose.setVisibility(View.VISIBLE);
            } else {
            	ivPhotoChoose.setVisibility(View.GONE);
            }
            Photo photo = item.getPhoto();
            
            if(photo != null){                
                if(photo.ThumbnailUriString != null){                    
                    ivPhoto.setImageURI(Uri.parse(photo.ThumbnailUriString));
                }
            	ivIsSend.getDrawable().setLevel(photo.Status);
            	double lat = photo.Latitude;
                double lng = photo.Longitude;
                String gpsString = String.format("GPS:%s, %s", Location.convert(lat, Location.FORMAT_SECONDS), Location.convert(lat, Location.FORMAT_SECONDS) );
                tvGPS.setText(gpsString);
            }
            if(item.getStatus() != BaseDTO.INT_NULL_VALUE){
                pbDownload.setVisibility(View.GONE);
                tvDownloadStatus.setVisibility(View.GONE);
                switch (item.getStatus()) {
                case ThreadManager.STATUS_INITIAL:
                case ThreadManager.STATUS_DOWNLOAD_START:                
                case ThreadManager.STATUS_CREATE_THUMBNAIL_START:
                    pbDownload.setVisibility(View.VISIBLE);
                    break;
                case ThreadManager.STATUS_DOWNLOAD_ERROR:
                    tvDownloadStatus.setVisibility(View.VISIBLE);
                    tvDownloadStatus.setText(R.string.photo_download_error);
                    break;
                case ThreadManager.STATUS_CREATE_THUMBNAIL_ERROR:
                    tvDownloadStatus.setVisibility(View.VISIBLE);
                    tvDownloadStatus.setText(R.string.photo_create_thumbnail_error);
                    break;
                case ThreadManager.STATUS_DOWNLOAD_COMPLETE:
                    break;
                default:                    
                    break;
                }                
                
            } else {
                pbDownload.setVisibility(View.GONE);
                tvDownloadStatus.setVisibility(View.GONE);
                if(photo.ThumbnailUriString == null && photo.UriString == null){
                    tvDownloadStatus.setVisibility(View.VISIBLE);
                    tvDownloadStatus.setText(R.string.photo_not_found);
                }                
                
            }
            convertView.setLayoutParams(new GridView.LayoutParams(GridView.AUTO_FIT, Settings.THUMBNAIL_SIZE));
            return convertView;
        }  
        
        public final int getCount() {
            return mItems.size() + gridPhotoItems.size();
        }

        public final Object getItem(int position) {
        	if(position < mItems.size()){
        		return mItems.get(position);
        	} else {
        		return gridPhotoItems.get(position - mItems.size());
        	}
        }
        
        @Override
        public long getItemId(int position) {	        
	        return position;
        }
        
        public boolean isChoosePhoto(){
        	for(PhotoGridItem item : gridPhotoItems){
        		if(item.isChecked()){
        			return true;
        		}
        	}
        	return false;
        }
        
    }

	//make photo
	private Button btnMakePlacePhoto, btnMakePlacePhotoNextPlace;
	
	private static List<PhotoGridItem> gridPhotoItems = new ArrayList<PhotoGridItem>();
		
	private static Uri mUri;
		
	
	private final int REQUEST_CODE_PHOTO_INTENT = 101;
	
	private static int screenWidth, screenHeight;
	private static int screenWidthDp, screenHeightDp;
	
	private static int widthPhoto, widthPhotoDp, gridPhotoWidthDp;
		
	private static final int WIDTH_PTOTO_DP = 300;

	private String getGraveItemText(int graveId){
	    String result = "";
	    try {
            List<Burial> burials = DB.q(Burial.class).where().eq("Grave_id", graveId).query();            
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < burials.size(); i++){
                Burial burial = burials.get(i);
                burial.toUpperFirstCharacterInFIO();
                String fio = String.format("ФИО: <u>%s %s %s</u>, <u>%s</u>, <u>%s</u>", (burial.LName != null) ? burial.LName : "", 
                        (burial.FName != null) ? burial.FName : "", (burial.MName != null ) ? burial.MName : "",
                                 (burial.FactDate != null) ? android.text.format.DateFormat.format("dd.MM.yyyy", burial.FactDate) : "",
                                 (burial.ContainerType != null ? burial.ContainerType.toString() : ""));
                sb.append(fio);
                if(i < (burials.size() - 1)){
                    sb.append("<br/>");
                }               
            }
            result = sb.toString();
            
        } catch (SQLException e) {
            this.mFileLog.error(Settings.UNEXPECTED_ERROR_MESSAGE, e);
        }
	    return result;	    
	}
		
	private void deleteSelectedPhotos(){
		ArrayList<PhotoGridItem> deletedItems = new ArrayList<PhotoGridItem>();
		for(PhotoGridItem item : gridPhotoItems){
			if(item.isChecked()){
			    String thumbnailPath = item.getThumbnailPath();
			    String imagePath = item.getImagePath();
			    if(imagePath != null){
			        File file = new File(imagePath);
                    file.delete();
			    }
			    if(thumbnailPath != null){
			        File file = new File(thumbnailPath);
                    file.delete();
			    }				
				if(item.getPlacePhoto() != null){
				    MonumentDB.deletePlacePhoto(item.getPlacePhoto());
				}				
				deletedItems.add(item);
			}
		}
		gridPhotoItems.removeAll(deletedItems);
		((BaseAdapter)mGVGrave.getAdapter()).notifyDataSetChanged();
		MenuItem actionRemoveMenuItem = BrowserCemeteryActivity.this.mOptionsMenu.findItem(R.id.action_remove);
		actionRemoveMenuItem.setEnabled(mGraveGridAdapter.isChoosePhoto());	
	}
	
	private void startIntentForMakePhoto(ComplexGrave complexGrave){
	    mUri = complexGrave.generateFileUri(null);
        if (mUri == null) {
            Toast.makeText(BrowserCemeteryActivity.this, "Невозможно сделать фото", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);        
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri);
        startActivityForResult(intent, REQUEST_CODE_PHOTO_INTENT);
	}
		
	private void makePlacePhotoCurrent(){
        Place place = DB.dao(Place.class).queryForId(getIntent().getIntExtra(EXTRA_PLACE_ID, -1));
        ComplexGrave complexGrave = new ComplexGrave();
        complexGrave.loadByPlaceId(place.Id);      
        startIntentForMakePhoto(complexGrave);
    }
	
	private void makePlacePhotoNextPlace(String filterOldPlaceName){
        Place place = DB.dao(Place.class).queryForId(getIntent().getIntExtra(EXTRA_PLACE_ID, -1));
        ComplexGrave complexGrave = new ComplexGrave();
        complexGrave.loadByPlaceId(place.Id);        
        Place nextPlace = null;        
        RuntimeExceptionDao<Place, Integer> placeDAO = DB.dao(Place.class);        
        nextPlace = getPlaceForMakePhotoInNextPlace(filterOldPlaceName, complexGrave);      
        placeDAO.createOrUpdate(nextPlace);
        nextPlace.toLog(mFileLog, LogOperation.INSERT);
               
        complexGrave = new ComplexGrave();
        complexGrave.loadByPlaceId(nextPlace.Id);
        mPlaceId = nextPlace.Id;       
        setNewIdInExtras(EXTRA_PLACE_ID, nextPlace.Id);        
        startIntentForMakePhoto(complexGrave);
    }
	
	private Place getPlaceForMakePhotoInNextPlace(String filterOldPlaceName, ComplexGrave complexGrave){
	    RuntimeExceptionDao<Place, Integer> placeDAO = DB.dao(Place.class);
	    String nextPlaceName = nextString(complexGrave.Place.Name);
        Place nextPlace = new Place();
        nextPlace.Name = nextPlaceName;
        nextPlace.Row = complexGrave.Row;
        nextPlace.Region = complexGrave.Region;
        nextPlace.IsChanged = 1;
        
        List<Place> findedPlaces = null;
        boolean isFindByOldName = false;
        try{
            if(complexGrave.Row != null){
                QueryBuilder<Place, Integer> placeBuilder = placeDAO.queryBuilder();
                if(!TextUtils.isEmpty(filterOldPlaceName)){
                    placeBuilder.where().eq("Name", filterOldPlaceName).and().eq(Place.ROW_ID_COLUMN, complexGrave.Row.Id);
                    findedPlaces = placeDAO.query(placeBuilder.prepare());
                    if(findedPlaces.size() > 0 ){
                        isFindByOldName = true;
                    } else {
                        placeBuilder = placeDAO.queryBuilder();
                        placeBuilder.where().eq("OldName", filterOldPlaceName).and().eq(Place.ROW_ID_COLUMN, complexGrave.Row.Id);
                        findedPlaces = placeDAO.query(placeBuilder.prepare());
                        if(findedPlaces.size() > 0 ){
                            isFindByOldName = true;
                        }
                    }                   
                }
                if(isFindByOldName == false){
                    placeBuilder = placeDAO.queryBuilder();
                    placeBuilder.where().eq("Name", nextPlaceName).and().eq(Place.ROW_ID_COLUMN, complexGrave.Row.Id);
                    findedPlaces = placeDAO.query(placeBuilder.prepare());
                }
            } else {
                QueryBuilder<Place, Integer> placeBuilder = placeDAO.queryBuilder();
                if(filterOldPlaceName != null && filterOldPlaceName != ""){
                    placeBuilder.where().eq("Name", filterOldPlaceName).and().eq(Place.REGION_ID_COLUMN, complexGrave.Region.Id);
                    findedPlaces = placeDAO.query(placeBuilder.prepare());
                    if(findedPlaces.size() > 0 ){
                        isFindByOldName = true;
                    } else {
                        placeBuilder = placeDAO.queryBuilder();
                        placeBuilder.where().eq("OldName", filterOldPlaceName).and().eq(Place.REGION_ID_COLUMN, complexGrave.Region.Id);
                        findedPlaces = placeDAO.query(placeBuilder.prepare());
                        if(findedPlaces.size() > 0 ){
                            isFindByOldName = true;
                        }   
                    }                   
                }
                if(isFindByOldName == false){
                    placeBuilder = placeDAO.queryBuilder();
                    placeBuilder.where().eq("Name", nextPlaceName).and().eq(Place.REGION_ID_COLUMN, complexGrave.Region.Id);
                    findedPlaces = placeDAO.query(placeBuilder.prepare());
                }
            }                   
            if(findedPlaces != null && findedPlaces.size() > 0){
                nextPlace = findedPlaces.get(0);
                if(isFindByOldName){
                    nextPlace.OldName = filterOldPlaceName;
                    nextPlace.Name = nextPlaceName;
                }
                else{
                    nextPlace.OldName = filterOldPlaceName;
                }
                nextPlace.IsChanged = 1;               
                
            } else {
                if(!TextUtils.isEmpty(filterOldPlaceName)){
                    nextPlace.OldName = filterOldPlaceName;                 
                }
            }
            if(nextPlace.Row != null){
            	nextPlace.Region = null;
            }
        } catch(SQLException e){
            this.mFileLog.error(Settings.UNEXPECTED_ERROR_MESSAGE, e);            
        }        
	    return nextPlace;	    
	}
	
	public static String nextString(String s){
		String result = null;
		int value;
		if(s == null || s == "" ){
			result = "1";
		} else {
			try{
				value = Integer.parseInt(s);
				value++;
				result = Integer.toString(value);
			} catch(Exception e){
				char lastChar = s.charAt(s.length() - 1);
				if(Character.isDigit(lastChar)){
					value = Integer.parseInt(Character.toString(lastChar));
					value++;
					result = s.substring(0, s.length() - 1) + Integer.toString(value);
				}else{
					result = s + "1";
				}
			}
		}
		return result;
	}
	
	
	private void setColumnWidthForPhoto(){
		DisplayMetrics metrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenHeight = metrics.heightPixels;
        screenWidth = metrics.widthPixels;
        screenHeightDp = (int)(screenHeight/metrics.density);
        screenWidthDp = (int)(screenWidth/metrics.density);
        int sizeDp = 0;        
        if(screenWidth < screenHeight){
            //min is screenWidth
            sizeDp = screenWidthDp;
            gridPhotoWidthDp = WIDTH_PTOTO_DP;
        } else {
            //min is screenHeight
            sizeDp = screenHeightDp;
            gridPhotoWidthDp = WIDTH_PTOTO_DP;
        }
        if(sizeDp < gridPhotoWidthDp){
            gridPhotoWidthDp = sizeDp;
        }        
        widthPhotoDp = gridPhotoWidthDp;
        widthPhoto =(int) (widthPhotoDp * metrics.density);
        this.mGVGrave.setColumnWidth(gridPhotoWidthDp);   
	}
	
	private void updatePhotoGridItems(){
	    Grave grave = null;
	    Place place = null;
	    int type = getIntent().getIntExtra(EXTRA_TYPE, -1);
	    switch (type) {
        case AddObjectActivity.ADD_PLACE_WITHROW:
        case AddObjectActivity.ADD_PLACE_WITHOUTROW:
            int placeId = getIntent().getIntExtra(EXTRA_PLACE_ID, -1);
            place = DB.dao(Place.class).queryForId(placeId);            
            break;
        case AddObjectActivity.ADD_GRAVE_WITHROW:
        case AddObjectActivity.ADD_GRAVE_WITHOUTROW:
            int graveId = getIntent().getIntExtra(EXTRA_GRAVE_ID, -1);
            grave = DB.dao(Grave.class).queryForId(graveId);
            break;
        default:
            break;
        }
	    
	    HashMap<Integer, PhotoGridItem> histPlacePhotoGridItemHashMap = new HashMap<Integer, PhotoGridItem>();	    
	    for(PhotoGridItem photoGridItem : gridPhotoItems){
	        if(photoGridItem.getPlacePhoto() != null){
	            histPlacePhotoGridItemHashMap.put(photoGridItem.getPlacePhoto().Id, photoGridItem);
	        }	        
	    }
        gridPhotoItems.clear();    	
    	if(place != null){
    	    ArrayList<PlacePhoto> photoList = MonumentDB.getPhotos(place);    	        	    
    	    for(PlacePhoto photo : photoList){
    	        PhotoGridItem item = new PhotoGridItem();                    
                item.setChecked(false);
                PhotoGridItem prevPhotoGridItem = null;                
                prevPhotoGridItem = histPlacePhotoGridItemHashMap.get(photo.Id);
                item.setPlacePhoto(photo);                    
                
                if(prevPhotoGridItem != null){
                    item.setChecked(prevPhotoGridItem.isChecked());
                    item.setStatus(prevPhotoGridItem.getStatus());
                }
                gridPhotoItems.add(item);
    	    }
    	    startCreateThumbnails(place.Id);
    	}
    	
        
	}	
	
	public void startDownloadThumbnails(int placeId){
	    Place place = DB.dao(Place.class).queryForId(placeId);
	    ComplexGrave complexGrave = new ComplexGrave();
	    complexGrave.loadByPlaceId(placeId);
	    for(PhotoGridItem photoGridItem : gridPhotoItems){
	        if(photoGridItem.isNecessaryToDownloadImage() && photoGridItem.getPlacePhoto() != null){
	            ThreadManager.getInstance().downloadThumbnail(this, photoGridItem.getPlacePhoto(), complexGrave);
	        }
	    }
	}
	
	public void startCreateThumbnails(int placeId){
        ComplexGrave complexGrave = new ComplexGrave();
        complexGrave.loadByPlaceId(placeId);
        for(PhotoGridItem photoGridItem : gridPhotoItems){
            if(photoGridItem.getPlacePhoto() != null && photoGridItem.isNecessaryToCreateThumbnailImage()){
                ThreadManager.getInstance().createThumbnail(this, photoGridItem.getPlacePhoto(), complexGrave);
            }
        }        
    }
	
	public void startCreateThumbnail(int placePhotoId){
        PlacePhoto placePhoto = DB.dao(PlacePhoto.class).queryForId(placePhotoId);
        ComplexGrave complexGrave = new ComplexGrave();
        complexGrave.loadByPlaceId(placePhoto.Place.Id);
        ThreadManager.getInstance().createThumbnail(this, placePhoto, complexGrave);        
    }
	
	@Override
    public void onChangeDownloadStatus(PlacePhoto placePhoto, int status) {
	    PlacePhoto dbPlacePhoto = DB.dao(PlacePhoto.class).queryForId(placePhoto.Id);
	    if(status == ThreadManager.STATUS_DOWNLOAD_COMPLETE){	        
	        dbPlacePhoto.UriString = placePhoto.UriString;
	        dbPlacePhoto.ThumbnailUriString = placePhoto.ThumbnailUriString;
	        DB.dao(PlacePhoto.class).update(dbPlacePhoto);
	        dbPlacePhoto.toLog(mFileLog, LogOperation.UPDATE);
	    }
	    for(PhotoGridItem photoGridItem : gridPhotoItems){
	        if(photoGridItem.getPlacePhoto() != null && photoGridItem.getPlacePhoto().Id == placePhoto.Id){
	            photoGridItem.setPlacePhoto(dbPlacePhoto);
	            photoGridItem.setStatus(status);
	            break;
	        }
	    }
	    this.mGraveGridAdapter.notifyDataSetChanged();
    }
	
	@Override
    public void onChangeCreateThumbnailStatus(PlacePhoto placePhoto, int status) {
	    PlacePhoto dbPlacePhoto = DB.dao(PlacePhoto.class).queryForId(placePhoto.Id);
        if(status == ThreadManager.STATUS_CREATE_THUMBNAIL_COMPLETE){
            dbPlacePhoto.ThumbnailUriString = placePhoto.ThumbnailUriString;
            DB.dao(PlacePhoto.class).update(dbPlacePhoto);
            dbPlacePhoto.toLog(mFileLog, LogOperation.UPDATE);
        }
        for(PhotoGridItem photoGridItem : gridPhotoItems){
            if(photoGridItem.getPlacePhoto() != null && photoGridItem.getPlacePhoto().Id == placePhoto.Id){
                photoGridItem.setPlacePhoto(dbPlacePhoto);
                photoGridItem.setStatus(status);
                break;
            }
        }
        this.mGraveGridAdapter.notifyDataSetChanged();      
    }
		
	
   
    class PhotoGridItem {
		    	
    	private boolean checked;
    	private PlacePhoto placePhoto;
    	private ImageView imageView;
    	private int status = BaseDTO.INT_NULL_VALUE;

        public int getStatus() {
            return this.status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public ImageView getImageView() {
            return imageView;
        }

        public void setImageView(ImageView imageView) {
            this.imageView = imageView;
        }

        public PlacePhoto getPlacePhoto() {
            return placePhoto;
        }

        public void setPlacePhoto(PlacePhoto placePhoto) {
            this.placePhoto = placePhoto;
        }

        
		public boolean isChecked() {
			return checked;
		}
		
		public Photo getPhoto(){
		    if(this.placePhoto != null){
		        return this.placePhoto;
		    }		    
		    return null;
		}
		
		public Uri getImageUri() {
		    Photo photo = getPhoto();
            String uriString = photo.UriString;
            if(uriString != null){
                return Uri.parse(uriString);
            }
            return null;            
        }
    	
		public String getImagePath() {
			Uri uri = getImageUri();
			if(uri != null){
			    return uri.getPath();
			}
			return null;
			
		}
		
		public Uri getThumbnailUri() {
		    Photo photo = getPhoto();
            String uriString = photo.ThumbnailUriString;
            if(uriString != null){
                return Uri.parse(uriString);
            }
            return null;
        }
		
		public String getThumbnailPath() {
		    Uri uri = getThumbnailUri();
            if(uri != null){
                return uri.getPath();
            }
            return null;
        }		
		
		public void setChecked(boolean checked) {
			this.checked = checked;
		}
		
		public boolean isNecessaryToDownloadImage(){
		    if(this.getPhoto().UriString == null && this.getPhoto().ThumbnailUriString == null && this.status == BaseDTO.INT_NULL_VALUE){
		        return true;
		    }
		    return false;
		}
		
		public boolean isNecessaryToCreateThumbnailImage(){
            if(this.getPhoto().UriString != null && this.getPhoto().ThumbnailUriString == null && this.status == BaseDTO.INT_NULL_VALUE){
                return true;
            }
            return false;
        }
		
	}

	@Override
	public void onCloseCheckGPS(boolean isTurnGPS) {
		if(isTurnGPS == false){
			switch (mMakePhotoType) {			
			case PLACEPHOTO_CURRENT:
			    makePlacePhotoCurrent();
			    break;
			case PLACEPHOTO_NEXTPLACE:
			    if(Settings.IsOldPlaceNameOption(BrowserCemeteryActivity.this)){
                    enterOldPlaceName();
                } else {
                    makePlacePhotoNextPlace(null);
                }
			    break;
			default:
				break;
			}
		}
		
	}
	
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
            case REQUEST_CODE_PHOTO_INTENT:
                ComplexGrave complexGrave = new ComplexGrave();
                switch (mMakePhotoType) {                
                case PLACEPHOTO_CURRENT:
                case PLACEPHOTO_NEXTPLACE:
                    int extraPlaceId = this.getIntent().getIntExtra(EXTRA_PLACE_ID, -1);
                    Place place = DB.dao(Place.class).queryForId(extraPlaceId);
                    complexGrave.loadByPlaceId(place.Id);
                    PlacePhoto placePhoto = new PlacePhoto();
                    placePhoto.Place = place;
                    placePhoto.CreateDate = new Date();
                    placePhoto.UriString = mUri.toString();
                    if(Settings.getCurrentLocation() != null){
                        Location location = Settings.getCurrentLocation();
                        placePhoto.Latitude = location.getLatitude();
                        placePhoto.Longitude = location.getLongitude();
                    }
                    if(Settings.IsAutoSendPhotoToServer(this)){
                        placePhoto.Status = Photo.STATUS_WAIT_SEND;
                    } else {
                        placePhoto.Status = Photo.STATUS_FORMATE;
                    }
                    DB.dao(PlacePhoto.class).create(placePhoto);
                    placePhoto.toLog(mFileLog, LogOperation.INSERT);
                    gridPhotoItems.clear();
                    mType = getIntent().getIntExtra(EXTRA_TYPE, -1);                
                    updateContent(mType, extraPlaceId);
                    startCreateThumbnail(placePhoto.Id);
                    break;
                default:
                    break;
                }
                
                break;
            case ADD_OBJECT_REQUEST_CODE:
            	if(data != null){
                	if(data.getIntExtra(AddObjectActivity.EXTRA_TYPE, -1) == AddObjectActivity.ADD_PLACE_WITHOUTROW || 
                			data.getIntExtra(AddObjectActivity.EXTRA_TYPE, -1) == AddObjectActivity.ADD_PLACE_WITHROW ){
                		if(data.getBooleanExtra(AddObjectActivity.EXTRA_IS_ADD_NEW, false)){            			
                			mType = data.getIntExtra(AddObjectActivity.EXTRA_TYPE, -1);
                			mPlaceId = data.getIntExtra(AddObjectActivity.EXTRA_ID, -1);
                			setNewIdInExtras(EXTRA_TYPE, mType);
                			setNewIdInExtras(EXTRA_PLACE_ID, mPlaceId);
                		}
                	}
            	}
                mType = getIntent().getIntExtra(EXTRA_TYPE, -1);                
                updateContent(mType);                
                break;
            case EDIT_OBJECT_REQUEST_CODE:
                mType = getIntent().getIntExtra(EXTRA_TYPE, -1);                
                updateContent(mType);
                break;
            case PlaceSearchActivity.PLACE_SEARCH_REQUESTCODE:
                String oldPlaceName = data.getStringExtra(PlaceSearchActivity.EXTRA_PLACE_OLDNAME);
                if(oldPlaceName == null){
                    oldPlaceName = data.getStringExtra(PlaceSearchActivity.EXTRA_PLACE_NAME);
                }
                if(this.etOldPlaceInAlert == null){
                    enterOldPlaceName();                    
                }
                this.etOldPlaceInAlert.setText(oldPlaceName);
                break;
            }           
        } 
        
    }

    

    
	
}
