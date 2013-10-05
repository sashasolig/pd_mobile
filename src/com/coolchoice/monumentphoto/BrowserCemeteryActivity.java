package com.coolchoice.monumentphoto;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.ExifTagConstants;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory;
import org.apache.sanselan.formats.tiff.write.TiffOutputField;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;


import android.R.bool;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.coolchoice.monumentphoto.BrowserCemeteryActivity.RowGridAdapter.RowOrPlace;
import com.coolchoice.monumentphoto.Settings.ISettings;
import com.coolchoice.monumentphoto.SyncTaskHandler.OperationType;
import com.coolchoice.monumentphoto.dal.DB;
import com.coolchoice.monumentphoto.dal.MonumentDB;
import com.coolchoice.monumentphoto.data.*;
import com.coolchoice.monumentphoto.task.TaskResult;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.QueryBuilder;
import android.text.Html;

public class BrowserCemeteryActivity extends Activity implements LocationListener, SyncTaskHandler.SyncCompleteListener, ISettings {

	public static final String EXTRA_CEMETERY_ID = "CemeteryId";
	public static final String EXTRA_REGION_ID = "RegionId";
	public static final String EXTRA_ROW_ID = "RowId";
	public static final String EXTRA_PLACE_ID = "PlaceId";
	public static final String EXTRA_GRAVE_ID = "GraveId";
	public static final String EXTRA_TYPE = "extra_type";
	
	public static final int ADD_OBJECT_REQUEST_CODE = 1;
	public static final int EDIT_OBJECT_REQUEST_CODE = 2;
	
	private static int mPrevType = -1; 
		
	private Button btnLinkCemetery, btnLinkRegion, btnLinkRow, btnLinkPlace, btnLinkGrave;
	
	private LinearLayout mainView;
	
	private static final int linkbgcolor_selected = Color.BLUE;
	private static final int linkbgcolor = Color.GRAY;
	
	private int mCemeteryId, mRegionId, mRowId, mPlaceId, mGraveId;
	private int mType;
	
	private ComplexGrave mComplexGrave;
	
	private static Region mChoosedRegion;
	private static RowOrPlace mChoosedRowOrPlace;
	private static Place mChoosedPlace;
	private static Grave mChoosedGrave;
	private static int mChoosedGridViewId = -1;
	
	private static boolean mIsCheckGPS = true;
	private static int mMakePhotoType = 0; // 0 - current grave, 1 - next grave, 2 - next place
	
	private GridView mGVRegion, mGVRow, mGVPlace, mGVGrave;
	
	private HorizontalScrollView mAddressBarSV;
	
	private static SyncTaskHandler mSyncTaskHandler;
	
	private Menu mOptionsMenu;
	
	private CheckBox cbIsOwnerLessPlace = null;
	
	private TextView tvPersons = null;
	
	public void enterOldPlaceName(){
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
	    final EditText input = new EditText(this);
	    alert.setTitle("Введите старое место");
	    alert.setView(input);
	    alert.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	            String value = input.getText().toString().trim();
	            makePhotoNextPlace(value);
	            dialog.cancel();
	        }
	    });

	    alert.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        	makePhotoNextPlace(null);
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
        if(type == AddObjectActivity.ADD_GRAVE_WITHROW || type == AddObjectActivity.ADD_GRAVE_WITHOUTROW){
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
		this.btnLinkGrave = (Button) findViewById(R.id.btnLinkGrave);		
								
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
	}
	
	@Override
	public void onComplete(OperationType operationType, TaskResult taskResult) {
		updateContent(mType);
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
				actionGet(false);
			} else {
				settingsData.IsAutoDownloadData = false;
			}
			Settings.saveSettingsData(this, settingsData);
			updateOptionsMenu();
			break;
		}	    
	    return true;
	}	
	
	private void actionGet(boolean isCache){		
		switch (mType) {
		case AddObjectActivity.ADD_CEMETERY:
			mCemeteryId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_CEMETERY_ID, -1);
			startGetRegion(mCemeteryId, isCache);
			break;
		case AddObjectActivity.ADD_REGION:
			mRegionId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_REGION_ID, -1);
			startGetPlace(mRegionId, isCache);
			break;
		case AddObjectActivity.ADD_ROW:
			mRowId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_ROW_ID, -1);
			Row row = DB.dao(Row.class).queryForId(mRowId);
			DB.dao(Region.class).refresh(row.Region);
			startGetPlace(row.Region.Id, isCache);
			break;
		case AddObjectActivity.ADD_PLACE_WITHOUTROW:
			mPlaceId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_PLACE_ID, -1);
			startGetGrave(mPlaceId, isCache);
			break;
		case AddObjectActivity.ADD_PLACE_WITHROW :
			mPlaceId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_PLACE_ID, -1);
			startGetGrave(mPlaceId, isCache);
			break;
		case AddObjectActivity.ADD_GRAVE_WITHOUTROW:
			mGraveId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_GRAVE_ID, -1);
			startGetBurial(mGraveId, isCache);
			break;
		case AddObjectActivity.ADD_GRAVE_WITHROW:
			mGraveId = getIntent().getIntExtra(BrowserCemeteryActivity.EXTRA_GRAVE_ID, -1);
			startGetBurial(mGraveId, isCache);
			break;
		default:
			break;
		}
	}
	
	private void startGetRegion(int cemeteryId, boolean isCache){
		if(mSyncTaskHandler != null){
			Cemetery cemetery = DB.dao(Cemetery.class).queryForId(cemeteryId);
			if(cemetery.ServerId > 0 ) {
				mSyncTaskHandler.startGetRegion(cemetery.ServerId);				
			}
		}
		
	}
	
	private void startGetPlace(int regionId, boolean isCache){
		if(mSyncTaskHandler != null){
			Region region = DB.dao(Region.class).queryForId(regionId);
			if(region.ServerId > 0){
				mSyncTaskHandler.startGetPlace(region.ServerId);
			}
		}
	}
	
	private void startGetGrave(int placeId, boolean isCache){
		if(mSyncTaskHandler != null){
			Place place = DB.dao(Place.class).queryForId(placeId);
			if(place.ServerId > 0 ){
				mSyncTaskHandler.startGetGrave(place.ServerId);				
			}
		}
	}
	
	private void startGetBurial(int graveId, boolean isCache){
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
		switch(type){
			case AddObjectActivity.ADD_CEMETERY:
				complexGrave.loadByCemeteryId(id);
				contentView = LayoutInflater.from(this).inflate(R.layout.region_list, null);
				//mainView.setBackgroundColor(Color.GRAY);
				btnLinkCemetery.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_pressed));
				handleRegionList(contentView, id);
			break;
			case AddObjectActivity.ADD_REGION:
				complexGrave.loadByRegionId(id);
				contentView = LayoutInflater.from(this).inflate(R.layout.row_list, null);
				//mainView.setBackgroundColor(Color.BLUE);
				btnLinkRegion.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_pressed));
				handleRowList(contentView, id);
			break;
			case AddObjectActivity.ADD_ROW:
				complexGrave.loadByRowId(id);
				contentView = LayoutInflater.from(this).inflate(R.layout.place_list, null);
				//mainView.setBackgroundColor(Color.YELLOW);
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
				//mainView.setBackgroundColor(Color.RED);
				btnLinkPlace.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_pressed));
				handleGraveList(contentView, id);
			break;
			case AddObjectActivity.ADD_PLACE_WITHOUTROW:
				complexGrave.loadByPlaceId(id);
				contentView = LayoutInflater.from(this).inflate(R.layout.grave_list, null);
				//mainView.setBackgroundColor(Color.RED);
				btnLinkPlace.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_pressed));
				handleGraveList(contentView, id);
			break;
			case AddObjectActivity.ADD_GRAVE_WITHROW:
				complexGrave.loadByGraveId(id);
				contentView = LayoutInflater.from(this).inflate(R.layout.photo_list, null);
				//mainView.setBackgroundColor(Color.GREEN);
				btnLinkGrave.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_pressed));
				handlePhotoList(contentView, id);
			break;
			case AddObjectActivity.ADD_GRAVE_WITHOUTROW:
				complexGrave.loadByGraveId(id);
				contentView = LayoutInflater.from(this).inflate(R.layout.photo_list, null);
				//mainView.setBackgroundColor(Color.GREEN);
				btnLinkGrave.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_pressed));
				handlePhotoList(contentView, id);
			break;
		}
		updateButtonLink(complexGrave);		
		mainView.removeAllViews();
		LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		mainView.addView(contentView, params);
		if(Settings.IsAutoDownloadData(this)){
			if(mPrevType != type){
				actionGet(false);
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
		Button btnAddGrave = (Button) contentView.findViewById(R.id.btnAddGrave);
		this.mGVGrave = (GridView) contentView.findViewById(R.id.gvGraves);
		registerForContextMenu(this.mGVGrave);
		List<Grave> graves = getGraves(placeId);
		GraveGridAdapter graveGridAdapter = new GraveGridAdapter(graves);
		mGVGrave.setAdapter(graveGridAdapter);
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
		
		mGVGrave.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				Grave grave = (Grave) mGVGrave.getAdapter().getItem(pos);
				if((mType & AddObjectActivity.MASK_ROW) == AddObjectActivity.MASK_ROW){
					mType = AddObjectActivity.ADD_GRAVE_WITHROW;
				} else {
					mType = AddObjectActivity.ADD_GRAVE_WITHOUTROW;
				}
				mGraveId = grave.Id;
				setNewIdInExtras(EXTRA_TYPE, mType);
				setNewIdInExtras(EXTRA_GRAVE_ID, mGraveId);
				updateContent(mType, mGraveId);				
			}
		});
		
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
			regionBuilder.orderBy(BaseDTO.COLUMN_NAME, true).where().eq("Cemetery_id", cemeteryId);
			regionList = regionDAO.query(regionBuilder.prepare());		
		} catch (SQLException e) {
			e.printStackTrace();
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
			rowBuilder.orderBy(BaseDTO.COLUMN_NAME, true).where().eq("Region_id", regionId);
			rows = rowDAO.query(rowBuilder.prepare());
			placeBuilder.orderBy(BaseDTO.COLUMN_NAME, true).where().eq("Region_id", regionId);
			places = placeDAO.query(placeBuilder.prepare());
		} catch (SQLException e) {
			e.printStackTrace();
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
			placeBuilder.orderBy(BaseDTO.COLUMN_NAME, true).where().eq("Row_id", rowId);
			placeList = placeDAO.query(placeBuilder.prepare());		
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return placeList;
	}
	
	private List<Grave> getGraves(int placeId){
		RuntimeExceptionDao<Grave, Integer> graveDAO = DB.dao(Grave.class);
    	QueryBuilder<Grave, Integer> graveBuilder = graveDAO.queryBuilder();
    	List<Grave> graveList = new ArrayList<Grave>();
    	try {
			graveBuilder.orderBy(BaseDTO.COLUMN_NAME, true).where().eq("Place_id", placeId);
			graveList = graveDAO.query(graveBuilder.prepare());		
		} catch (SQLException e) {
			e.printStackTrace();
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
            String value = mItems.get(position).Name;
            tvRegion.setText(value);
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

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
            	LayoutInflater inflater = (LayoutInflater) BrowserCemeteryActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.grave_item, parent, false);
            }
            ImageView ivIcon = (ImageView) convertView.findViewById(R.id.ivGrave);
            TextView tvGrave = (TextView) convertView.findViewById(R.id.tvGrave);
            TextView tvUnusedGrave = (TextView) convertView.findViewById(R.id.tvUnusedGrave);
            Grave grave = mItems.get(position);
            String value = grave.Name;
            tvGrave.setText(value);
            boolean unusedGrave = false;
            List<GravePhoto> photos = DB.dao(GravePhoto.class).queryForEq("Grave_id", grave.Id);
            if(photos == null || photos.size() == 0){
            	unusedGrave = true;
            } else {
            	unusedGrave = false;
            }
            if(unusedGrave){
            	tvGrave.setTextColor(getResources().getColor(R.color.unused_grave_color));
            	tvUnusedGrave.setTextColor(getResources().getColor(R.color.unused_grave_color));
            	tvUnusedGrave.setVisibility(View.VISIBLE);
            } else {
            	tvGrave.setTextColor(getResources().getColor(R.color.text_view_color));
            	tvUnusedGrave.setTextColor(getResources().getColor(R.color.text_view_color));
            	tvUnusedGrave.setVisibility(View.GONE);
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

	//make photo
	private Button btnMakePhoto, btnDeletePhoto, btnSendPhoto, btnMakePhotoNextGrave, btnMakePhotoNextPlace;
	
	private GridView gridPhotos;
	
	private static List<PhotoGridItem> gridPhotoItems = new ArrayList<PhotoGridItem>();
		
	private static Uri mUri;
		
	private PhotoGridAdapter mPhotoGridAdapter;
	
	private final int REQUEST_CODE_PHOTO_INTENT = 101;
	
	private static int screenWidth, screenHeight;
	private static int screenWidthDp, screenHeightDp;
	
	private static int widthPhoto, widthPhotoDp, gridPhotoWidthDp;
		
	private static final int WIDTH_PTOTO_DP = 300;
	
	private void handlePhotoList(View contentView, int graveId){
		this.btnLinkCemetery.setVisibility(View.VISIBLE);
		this.btnLinkRegion.setVisibility(View.VISIBLE);
		if((mType & AddObjectActivity.MASK_ROW) == AddObjectActivity.MASK_ROW){
			this.btnLinkRow.setVisibility(View.VISIBLE);
		} else {
			this.btnLinkRow.setVisibility(View.GONE);
		}
		this.btnLinkPlace.setVisibility(View.VISIBLE);
		this.btnLinkGrave.setVisibility(View.VISIBLE);
		
		this.cbIsOwnerLessPlace = (CheckBox) contentView.findViewById(R.id.cbIsOwnerLess);
		this.tvPersons = (TextView) contentView.findViewById(R.id.tvPersons);
		this.btnDeletePhoto = (Button) contentView.findViewById(R.id.btnDeletePhoto);
        this.btnMakePhoto = (Button) contentView.findViewById(R.id.btnMakePhoto);
        this.btnSendPhoto = (Button) contentView.findViewById(R.id.btnSend);
        this.btnMakePhotoNextGrave = (Button) contentView.findViewById(R.id.btnMakePhotoNextGrave);
        this.btnMakePhotoNextPlace = (Button) contentView.findViewById(R.id.btnMakePhotoNextPlace);
        this.gridPhotos = (GridView) contentView.findViewById(R.id.gvPhotos);
        
                
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
        this.gridPhotos.setColumnWidth(gridPhotoWidthDp);
        

        
        this.btnMakePhoto.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mMakePhotoType = 0;
				if(mIsCheckGPS){
					boolean enableGPS = Settings.checkWorkOfGPS(BrowserCemeteryActivity.this, BrowserCemeteryActivity.this);
					if(enableGPS == false){
						mIsCheckGPS = false;
						return;
					}
				}
				makePhotoCurrentGrave();
				
			}
		});
        
        this.btnMakePhotoNextGrave.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mMakePhotoType = 1;
				if(mIsCheckGPS){
					boolean enableGPS = Settings.checkWorkOfGPS(BrowserCemeteryActivity.this, BrowserCemeteryActivity.this);
					if(enableGPS == false){
						mIsCheckGPS = false;
						return;
					}
				}
				makePhotoNextGrave();
			}
		});
        
        this.btnMakePhotoNextPlace.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mMakePhotoType = 2;
				if(mIsCheckGPS){
					boolean enableGPS = Settings.checkWorkOfGPS(BrowserCemeteryActivity.this, BrowserCemeteryActivity.this);
					if(enableGPS == false){
						mIsCheckGPS = false;
						return;
					}
				}
				if(Settings.IsOldPlaceNameOption(BrowserCemeteryActivity.this)){
					enterOldPlaceName();
				} else {
					makePhotoNextPlace(null);
				}				
			}
		});
        
        this.cbIsOwnerLessPlace.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Grave grave = DB.dao(Grave.class).queryForId(getIntent().getIntExtra(EXTRA_GRAVE_ID, -1));
				ComplexGrave complexGrave = new ComplexGrave();
				complexGrave.loadByGraveId(grave.Id);
				Place place = complexGrave.Place;
				place.IsOwnerLess = isChecked;
				place.IsChanged = 1;
				DB.dao(Place.class).update(place);
				
			}
		});          
        
        updatePhotoGridItems();
        this.mPhotoGridAdapter = new PhotoGridAdapter();
        this.gridPhotos.setAdapter(this.mPhotoGridAdapter);
        this.gridPhotos.setOnItemClickListener(new OnItemClickListener() {            
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id ) {
				PhotoGridItem item = gridPhotoItems.get(position);
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(item.getUri(), "image/*");
				startActivity(intent);				
			}
        });
        this.gridPhotos.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
				gridPhotoItems.get(position).setChecked(!gridPhotoItems.get(position).isChecked());
				((BaseAdapter)gridPhotos.getAdapter()).notifyDataSetChanged();
				btnDeletePhoto.setEnabled(mPhotoGridAdapter.isChoosePhoto());
				btnSendPhoto.setEnabled(mPhotoGridAdapter.isChoosePhoto());
				MenuItem actionRemoveMenuItem = BrowserCemeteryActivity.this.mOptionsMenu.findItem(R.id.action_remove);
				actionRemoveMenuItem.setEnabled(mPhotoGridAdapter.isChoosePhoto());
				return true;
			}
		});
        
        Grave grave = DB.dao(Grave.class).queryForId(getIntent().getIntExtra(EXTRA_GRAVE_ID, -1));
		ComplexGrave complexGrave = new ComplexGrave();
		complexGrave.loadByGraveId(grave.Id);
		this.cbIsOwnerLessPlace.setChecked(complexGrave.Place.IsOwnerLess);
		
		
		try {
			List<Burial> burials = DB.q(Burial.class).where().eq("Grave_id", grave.Id).query();
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < burials.size(); i++){
				Burial burial = burials.get(i);
				String fio = String.format("ФИО: <u>%s %s %s</u>   Дата захоранения: <u>%s</u>", (burial.LName != null) ? burial.LName : "", 
						(burial.FName != null) ? burial.FName : "", (burial.MName != null ) ? burial.MName : "",
								android.text.format.DateFormat.format("dd.MM.yyyy", burial.FactDate));
				sb.append(fio);
				if(i < (burials.size() - 1)){
					sb.append("<br/>");
				}				
			}
			if(burials.size() > 0){
				tvPersons.setVisibility(View.VISIBLE);
				tvPersons.setText(Html.fromHtml(sb.toString()));
			} else {
				tvPersons.setVisibility(View.GONE);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
        
        this.btnDeletePhoto.setEnabled(this.mPhotoGridAdapter.isChoosePhoto());
        this.btnSendPhoto.setEnabled(this.mPhotoGridAdapter.isChoosePhoto());
        if(BrowserCemeteryActivity.this.mOptionsMenu != null){
        	MenuItem actionRemoveMenuItem = BrowserCemeteryActivity.this.mOptionsMenu.findItem(R.id.action_remove);
        	actionRemoveMenuItem.setEnabled(mPhotoGridAdapter.isChoosePhoto());
        }
	}
	
	private void deleteSelectedPhotos(){
		ArrayList<PhotoGridItem> deletedItems = new ArrayList<PhotoGridItem>();
		for(PhotoGridItem item : gridPhotoItems){
			if(item.isChecked()){
				File file = new File(item.getPath());
				file.delete();
				MonumentDB.deleteMonumentPhoto(item.getGravePhoto());
				deletedItems.add(item);
			}
		}
		gridPhotoItems.removeAll(deletedItems);
		((BaseAdapter)gridPhotos.getAdapter()).notifyDataSetChanged();
		btnDeletePhoto.setEnabled(mPhotoGridAdapter.isChoosePhoto());
		MenuItem actionRemoveMenuItem = BrowserCemeteryActivity.this.mOptionsMenu.findItem(R.id.action_remove);
		actionRemoveMenuItem.setEnabled(mPhotoGridAdapter.isChoosePhoto());
		btnSendPhoto.setEnabled(mPhotoGridAdapter.isChoosePhoto());	
		
	}
	
	private void makePhotoCurrentGrave(){
		Grave grave = DB.dao(Grave.class).queryForId(getIntent().getIntExtra(EXTRA_GRAVE_ID, -1));
		ComplexGrave complexGrave = new ComplexGrave();
		complexGrave.loadByGraveId(grave.Id);					
		mUri = generateFileUri(complexGrave);
		if (mUri == null) {
			Toast.makeText(BrowserCemeteryActivity.this, "Невозможно сделать фото",	Toast.LENGTH_LONG).show();
			return;
		}

		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri);
		startActivityForResult(intent, REQUEST_CODE_PHOTO_INTENT);
	}
	
	private void makePhotoNextGrave(){
		Grave grave = DB.dao(Grave.class).queryForId(getIntent().getIntExtra(EXTRA_GRAVE_ID, -1));
		ComplexGrave complexGrave = new ComplexGrave();
		complexGrave.loadByGraveId(grave.Id);
		
		String nextGraveName = nextString(complexGrave.Grave.Name);
		Grave nextGrave = new Grave();
		nextGrave.Name = nextGraveName;
		nextGrave.Place = complexGrave.Place;
		nextGrave.IsChanged = 1;
		RuntimeExceptionDao<Grave, Integer> graveDAO = DB.dao(Grave.class);
		try {    				
			QueryBuilder<Grave, Integer> builder = graveDAO.queryBuilder();
			builder.where().eq("Name", nextGraveName).and().eq("Place_id", complexGrave.Place.Id);
			List<Grave> findedGraves = graveDAO.query(builder.prepare());
			if(findedGraves != null && findedGraves.size() > 0){
				nextGrave = findedGraves.get(0);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		graveDAO.createOrUpdate(nextGrave);
		
		complexGrave = new ComplexGrave();
		complexGrave.loadByGraveId(nextGrave.Id);
		mGraveId = nextGrave.Id;
		setNewIdInExtras(EXTRA_GRAVE_ID, nextGrave.Id);
		
		
		mUri = generateFileUri(complexGrave);
		if (mUri == null) {
			Toast.makeText(BrowserCemeteryActivity.this, "Невозможно сделать фото",	Toast.LENGTH_LONG).show();
			return;
		}

		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri);
		startActivityForResult(intent, REQUEST_CODE_PHOTO_INTENT);
	}
	
	private void makePhotoNextPlace(String filterOldPlaceName){
		Grave grave = DB.dao(Grave.class).queryForId(getIntent().getIntExtra(EXTRA_GRAVE_ID, -1));
		ComplexGrave complexGrave = new ComplexGrave();
		complexGrave.loadByGraveId(grave.Id);
		
		String nextPlaceName = nextString(complexGrave.Place.Name);
		Place nextPlace = new Place();
		nextPlace.Name = nextPlaceName;
		nextPlace.Row = complexGrave.Row;
		nextPlace.Region = complexGrave.Region;
		nextPlace.IsChanged = 1;
		Grave nextGrave = new Grave();
		nextGrave.Name = "1";
		nextGrave.Place = nextPlace;
		nextGrave.IsChanged = 1;
		
		RuntimeExceptionDao<Place, Integer> placeDAO = DB.dao(Place.class);
		RuntimeExceptionDao<Grave, Integer> graveDAO = DB.dao(Grave.class);
		List<Place> findedPlaces = null;
		boolean isFindByOldName = false;
		try{
			if(complexGrave.Row != null){
				QueryBuilder<Place, Integer> placeBuilder = placeDAO.queryBuilder();
				if(filterOldPlaceName != null && filterOldPlaceName != ""){
					placeBuilder.where().eq("Name", filterOldPlaceName).and().eq("Row_id", complexGrave.Row.Id);
					findedPlaces = placeDAO.query(placeBuilder.prepare());
					if(findedPlaces.size() > 0 ){
						isFindByOldName = true;
					} else {
						placeBuilder = placeDAO.queryBuilder();
						placeBuilder.where().eq("OldName", filterOldPlaceName).and().eq("Row_id", complexGrave.Row.Id);
						findedPlaces = placeDAO.query(placeBuilder.prepare());
						if(findedPlaces.size() > 0 ){
							isFindByOldName = true;
						}	
					}					
				}
				if(isFindByOldName == false){
					placeBuilder = placeDAO.queryBuilder();
					placeBuilder.where().eq("Name", nextPlaceName).and().eq("Row_id", complexGrave.Row.Id);
					findedPlaces = placeDAO.query(placeBuilder.prepare());
				}
			} else {
				QueryBuilder<Place, Integer> placeBuilder = placeDAO.queryBuilder();
				if(filterOldPlaceName != null && filterOldPlaceName != ""){
					placeBuilder.where().eq("Name", filterOldPlaceName).and().eq("Region_id", complexGrave.Region.Id);
					findedPlaces = placeDAO.query(placeBuilder.prepare());
					if(findedPlaces.size() > 0 ){
						isFindByOldName = true;
					} else {
						placeBuilder = placeDAO.queryBuilder();
						placeBuilder.where().eq("OldName", filterOldPlaceName).and().eq("Region_id", complexGrave.Region.Id);
						findedPlaces = placeDAO.query(placeBuilder.prepare());
						if(findedPlaces.size() > 0 ){
							isFindByOldName = true;
						}	
					}					
				}
				if(isFindByOldName == false){
					placeBuilder = placeDAO.queryBuilder();
					placeBuilder.where().eq("Name", nextPlaceName).and().eq("Region_id", complexGrave.Region.Id);
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
				
				QueryBuilder<Grave, Integer> graveBuilder = graveDAO.queryBuilder();
				graveBuilder.orderBy("Name", true).where().eq("Place_id", nextPlace.Id);
				List<Grave> findedGraves = graveDAO.query(graveBuilder.prepare());
				if(findedGraves != null && findedGraves.size() > 0){
					nextGrave = findedGraves.get(0);
				}
			} else {
				if(filterOldPlaceName != null && filterOldPlaceName != ""){
					nextPlace.OldName = filterOldPlaceName;					
				}
			}
		}catch(SQLException exc){
			exc.printStackTrace();
		}
		nextGrave.Place = nextPlace;
		placeDAO.createOrUpdate(nextPlace);
		graveDAO.createOrUpdate(nextGrave);
		
		
		complexGrave = new ComplexGrave();
		complexGrave.loadByGraveId(nextGrave.Id);
		mPlaceId = nextPlace.Id;
		mGraveId = nextGrave.Id;
		setNewIdInExtras(EXTRA_PLACE_ID, nextPlace.Id);
		setNewIdInExtras(EXTRA_GRAVE_ID, nextGrave.Id);
		
		
		mUri = generateFileUri(complexGrave);
		if (mUri == null) {
			Toast.makeText(BrowserCemeteryActivity.this, "Невозможно сделать фото",	Toast.LENGTH_LONG).show();
			return;
		}

		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri);
		startActivityForResult(intent, REQUEST_CODE_PHOTO_INTENT);
	}
	
	private String nextString(String s){
		String result = null;
		int value;
		if(s == null || s == "" ){
			result = "1";
		} else {
			try{
				value = Integer.parseInt(s);
				value++;
				result = Integer.toString(value);
			}catch(Exception e){
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
	
	private void updatePhotoGridItems(){
		int graveId = getIntent().getIntExtra(EXTRA_GRAVE_ID, -1);
		Grave grave = DB.dao(Grave.class).queryForId(graveId);
        boolean isAddImage = true;
        if(gridPhotoItems.size() > 0){
        	if(grave.Id != gridPhotoItems.get(0).getGravePhoto().Grave.Id){
        		gridPhotoItems.clear();        		
        	}else{
        		isAddImage = false;
        	}
        }
        if(isAddImage){
        	if(grave != null){
		        for(GravePhoto monumentPhoto : grave.Photos){
		        	PhotoGridItem item = new PhotoGridItem();
		        	Uri uri = Uri.parse(monumentPhoto.UriString);
		        	item.setPath(uri.getPath());
		        	item.setChecked(false);
		        	item.setUri(uri);
		        	item.setBmp(null);
		        	item.setGravePhoto(monumentPhoto);
		        	gridPhotoItems.add(item);
		        }
        	}
        } else {
        	if(grave != null){
        		for(PhotoGridItem item : gridPhotoItems){
		        	DB.dao(GravePhoto.class).refresh(item.getGravePhoto());		        	
		        }
        	}
        }
	}
	
	public void updateStatusInPhotoGrid(){
		int graveId = getIntent().getIntExtra(EXTRA_GRAVE_ID, -1);
		Grave grave = DB.dao(Grave.class).queryForId(graveId);
		HashMap<Integer, GravePhoto> hashMapStatus = new HashMap<Integer,GravePhoto>();
		for(GravePhoto photo : grave.Photos){
			hashMapStatus.put(photo.Id, photo);
		}
		for(PhotoGridItem item : gridPhotoItems){
			if(item.getGravePhoto() != null){
				item.setGravePhoto(hashMapStatus.get(item.getGravePhoto().Id));
			}
		}
		mPhotoGridAdapter.notifyDataSetChanged();
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
			case REQUEST_CODE_PHOTO_INTENT:
				int extraGraveId = this.getIntent().getIntExtra(EXTRA_GRAVE_ID, -1);
				Grave grave = DB.dao(Grave.class).queryForId(extraGraveId);
				ComplexGrave complexGrave = new ComplexGrave();
				complexGrave.loadByGraveId(grave.Id);
				GravePhoto gravePhoto = new GravePhoto();
				gravePhoto.Grave = grave;
				gravePhoto.CreateDate = new Date();
				gravePhoto.UriString = mUri.toString();
				if(Settings.getCurrentLocation() != null){
					Location location = Settings.getCurrentLocation();
					gravePhoto.Latitude = location.getLatitude();
					gravePhoto.Longitude = location.getLongitude();
				}
				if(Settings.IsAutoSendPhotoToServer(this)){
					gravePhoto.Status = Monument.STATUS_WAIT_SEND;
				} else {
					gravePhoto.Status = Monument.STATUS_FORMATE;
				}
				DB.dao(GravePhoto.class).create(gravePhoto);
				saveExifInfo(mUri.getPath(), complexGrave, gravePhoto);
				
				/*PhotoGridItem item = new PhotoGridItem();
	        	item.setPath(mUri.getPath());
	        	item.setChecked(false);
	        	item.setUri(mUri);
	        	item.setBmp(null);
	        	item.setGravePhoto(gravePhoto);
	        	gridPhotoItems.add(item);
	        	((BaseAdapter)gridPhotos.getAdapter()).notifyDataSetChanged();*/
	        	
	        	gridPhotoItems.clear();
	        	mType = getIntent().getIntExtra(EXTRA_TYPE, -1);				
				updateContent(mType, extraGraveId);
				break;
			case ADD_OBJECT_REQUEST_CODE:
				mType = getIntent().getIntExtra(EXTRA_TYPE, -1);				
				updateContent(mType);
				break;
			case EDIT_OBJECT_REQUEST_CODE:
				mType = getIntent().getIntExtra(EXTRA_TYPE, -1);				
				updateContent(mType);
				break;
			}
		} 
		
	}
	
	private Uri generateFileUri(ComplexGrave complexGrave) {
		File rootDir = Settings.getRootDirPhoto();
		if(complexGrave != null){
			return complexGrave.generateFileUri(rootDir);
		} else {
			String timeValue = String.valueOf(System.currentTimeMillis());
			File newFile = new File(rootDir.getPath() + File.separator + timeValue	+ ".jpg");
			return Uri.fromFile(newFile);			
		}		
	}
	
	public boolean saveExifInfo(String filePath, ComplexGrave complexGrave, GravePhoto gravePhoto){
		try {
			TiffOutputSet outputSet = null;
			IImageMetadata metadata = Sanselan.getMetadata(new File(filePath));
			JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
			if (null != jpegMetadata) {
				TiffImageMetadata exif = jpegMetadata.getExif();
				if (null != exif) {
					outputSet = exif.getOutputSet();
				}
			}
			if (null != outputSet) {
				TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
				int ownerLessFlag = 0;
				if(complexGrave.Place.IsOwnerLess){
					ownerLessFlag = 1;
				}
				String rowName = "";
				if(complexGrave.Row != null){
					rowName = complexGrave.Row.Name;
				}				
				String userCommentValue = String.format("%f~%f~%s~%s~%s~%s~%s~%d", gravePhoto.Longitude, gravePhoto.Latitude, complexGrave.Cemetery.Name,
						complexGrave.Region.Name, rowName, complexGrave.Place.Name, complexGrave.Grave.Name, ownerLessFlag);
				byte[] userCommentValueBytes = userCommentValue.getBytes("Cp1251");
				TiffOutputField f = new TiffOutputField(ExifTagConstants.EXIF_TAG_USER_COMMENT,
						ExifTagConstants.EXIF_TAG_USER_COMMENT.FIELD_TYPE_ASCII,
						userCommentValueBytes.length, userCommentValueBytes);
				exifDirectory.removeField(TiffConstants.EXIF_TAG_USER_COMMENT);
				exifDirectory.add(f);
				outputSet.setGPSInDegrees(gravePhoto.Longitude, gravePhoto.Latitude);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ExifRewriter exifRewriter = new ExifRewriter();
				exifRewriter.updateExifMetadataLossless(new File(filePath), baos, outputSet);
				FileOutputStream output = new FileOutputStream(filePath);
				output.write(baos.toByteArray());
				output.close();				
			}
		} catch (Exception ex) {
			return false;
		}
		return true;

	}
	
	public class PhotoGridAdapter extends BaseAdapter {
		
        public PhotoGridAdapter() {
        }        

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
            	LayoutInflater inflater = (LayoutInflater) BrowserCemeteryActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.photo_grid_item, parent, false);
            }
            ImageView ivPhoto = (ImageView) convertView.findViewById(R.id.ivPhoto);
            ImageView ivPhotoChoose = (ImageView) convertView.findViewById(R.id.ivChoosePhoto);
            ImageView ivIsSend = (ImageView) convertView.findViewById(R.id.ivStatus);
            TextView tvGPS = (TextView) convertView.findViewById(R.id.tvGPS);
            PhotoGridItem item = gridPhotoItems.get(position);
            if(item.getBmp() == null) {
	            File imgFile = new  File(item.getPath());
	            int widthScaledPhotoPx = BrowserCemeteryActivity.widthPhoto;
	            int heightScaledPhotoPx;
	            int rotateAngle = 0;
	            if(imgFile.exists()){	
	            	try {
						ExifInterface ex = new ExifInterface(imgFile.getAbsolutePath());
						//byte[] thumbnail = ex.getThumbnail();
						int orientation = ex.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
						switch (orientation) {
						case ExifInterface.ORIENTATION_NORMAL:
							rotateAngle = 0;
							break;
						case ExifInterface.ORIENTATION_ROTATE_90:
							rotateAngle = 90;		
							break;
						case ExifInterface.ORIENTATION_ROTATE_180:
							rotateAngle = 180;
							break;
						case ExifInterface.ORIENTATION_ROTATE_270:
							rotateAngle = 270;
							break;
						default:
							rotateAngle = 0;
							break;
						}
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
	                heightScaledPhotoPx = myBitmap.getHeight() * widthScaledPhotoPx / myBitmap.getWidth();
	                Bitmap scaledBmp = Bitmap.createScaledBitmap(myBitmap, widthScaledPhotoPx, heightScaledPhotoPx, true);
	                
	                Matrix matrix = new Matrix();
	                matrix.setRotate(rotateAngle);
	                Bitmap rotateScaledBmp = Bitmap.createBitmap(scaledBmp, 0, 0, scaledBmp.getWidth(), scaledBmp.getHeight(), matrix, true);
	                                
	                
	                ivPhoto.setImageBitmap(rotateScaledBmp);
	                gridPhotoItems.get(position).setBmp(rotateScaledBmp);	                
	            }            
            } else {
            	ivPhoto.setImageBitmap(item.getBmp());
            }
            
            if(item.isChecked()) {
            	ivPhotoChoose.setVisibility(View.VISIBLE);
            } else {
            	ivPhotoChoose.setVisibility(View.GONE);
            }
            if(item.getGravePhoto() != null){
            	ivIsSend.getDrawable().setLevel(item.getGravePhoto().Status);
            }            
            if(item.getGravePhoto() != null){
            	double lat = item.getGravePhoto().Latitude;
            	double lng = item.getGravePhoto().Longitude;
            	String gpsString = String.format("GPS:%s, %s", Location.convert(lat, Location.FORMAT_SECONDS),Location.convert(lat, Location.FORMAT_SECONDS) );
            	tvGPS.setText(gpsString);
            } else {
            	tvGPS.setText("GPS null");
            }

            return convertView;
        }  
        
        public boolean isChoosePhoto(){
        	for(PhotoGridItem item : gridPhotoItems){
        		if(item.isChecked()){
        			return true;
        		}
        	}
        	return false;
        }
        


        public final int getCount() {
            return gridPhotoItems.size();
        }

        public final Object getItem(int position) {
            return gridPhotoItems.get(position);
        }

        public final long getItemId(int position) {
            return position;
        }
    }
    
    class PhotoGridItem {
    	  	
		private String path;
		private Uri uri;
    	private Bitmap bmp;
    	private boolean checked;
    	private GravePhoto gravePhoto;
    	
    	public GravePhoto getGravePhoto() {
			return gravePhoto;
		}

		public void setGravePhoto(GravePhoto gravePhoto) {
			this.gravePhoto = gravePhoto;
		}

		public boolean isChecked() {
			return checked;
		}
    	
		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public Uri getUri() {
			return uri;
		}

		public void setUri(Uri uri) {
			this.uri = uri;
		}

		public void setChecked(boolean checked) {
			this.checked = checked;
		}
		
		public Bitmap getBmp() {
			return bmp;
		}
		
		public void setBmp(Bitmap bmp) {
			this.bmp = bmp;
		}	
	}

	@Override
	public void onCloseCheckGPS(boolean isTurnGPS) {
		if(isTurnGPS == false){
			switch (mMakePhotoType) {
			case 0:
				makePhotoCurrentGrave();
				break;
			case 1:
				makePhotoNextGrave();
				break;
			case 2:
				if(Settings.IsOldPlaceNameOption(BrowserCemeteryActivity.this)){
					enterOldPlaceName();
				} else {
					makePhotoNextPlace(null);
				}
				break;
			default:
				break;
			}
		}
		
	}

	
	
}
