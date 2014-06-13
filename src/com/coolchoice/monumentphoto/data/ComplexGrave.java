package com.coolchoice.monumentphoto.data;

import java.io.File;

import android.net.Uri;

import com.coolchoice.monumentphoto.Settings;
import com.coolchoice.monumentphoto.dal.DB;
import com.coolchoice.monumentphoto.dal.MonumentDB;

public class ComplexGrave {
	
	public Grave Grave;
	
	public Place Place;
	
	public Row Row;
	
	public Region Region;
	
	public Cemetery Cemetery;
	
	public ComplexGrave(){
		setToNullObject();
	}
	
	public static class PlaceWithFIO{
		public String PlaceName;
		public String OldPlaceName;
		public int PlaceId;
		public String FName;
		public String MName;
		public String LName;
		
		public void toUpperFirstCharacterInFIO(){
			if(this.FName != null && this.FName.length() > 0){
				if(this.FName.length() == 1){
					this.FName = this.FName.toUpperCase();
				} else {
					this.FName = this.FName.substring(0, 1).toUpperCase() + this.FName.substring(1);
				}
			}
			if(this.LName != null && this.LName.length() > 0){
				if(this.LName.length() == 1){
					this.LName = this.LName.toUpperCase();
				} else {
					this.LName = this.LName.substring(0, 1).toUpperCase() + this.LName.substring(1);
				}
			}
			if(this.MName != null && this.MName.length() > 0){
				if(this.MName.length() == 1){
					this.MName = this.MName.toUpperCase();
				} else {
					this.MName = this.MName.substring(0, 1).toUpperCase() + this.MName.substring(1);
				}
			}
		}
	}
	
	public void setToNullObject(){
		this.Grave = null;
		this.Place = null;
		this.Row = null;
		this.Region = null;
		this.Cemetery = null;
	}
	
	public void loadByGraveId(int graveId){
		this.Grave = DB.dao(Grave.class).queryForId(graveId);
		if(this.Grave != null){
			loadByPlaceId(this.Grave.Place.Id);
		} else {
			setToNullObject();
		}
	}
	
	public void loadByPlaceId(int placeId){
		this.Place = DB.dao(Place.class).queryForId(placeId);
		if(this.Place != null){
			if(this.Place.Row != null){
				loadByRowId(this.Place.Row.Id);
			} else {
				this.Row = null;
				loadByRegionId(this.Place.Region.Id);
			}
		} else {
			setToNullObject();
		}
	}
	
	public void loadByRowId(int rowId){
		this.Row = DB.dao(Row.class).queryForId(rowId);
		if(this.Row != null){
			loadByRegionId(this.Row.Region.Id);
		} else {
			setToNullObject();
		}
	}
	
	public void loadByRegionId(int regionId){
		this.Region = DB.dao(Region.class).queryForId(regionId);
		if(this.Region != null){
			loadByCemeteryId(this.Region.Cemetery.Id);
		} else {
			setToNullObject();
		}
	}
	
	public void loadByCemeteryId(int cemeteryId){
		this.Cemetery = DB.dao(Cemetery.class).queryForId(cemeteryId);
		if(this.Cemetery != null) {
			//do nothing
		} else {
			setToNullObject();
		}
	}
	
	public Uri generateFileUri(File rootDir, String fileName) {
		File dir = new File(rootDir, this.Cemetery.Name);
		if(!dir.exists()){
			if(!dir.mkdirs()){
				return null;
			}
		}
		dir = new File(dir, this.Region.Name);
		if(!dir.exists()){
			if(!dir.mkdirs()){
				return null;
			}
		}
		if(this.Row != null){
			dir = new File(dir, this.Row.Name);
			if(!dir.exists()){
				if(!dir.mkdirs()){
					return null;
				}
			}
		}
		dir = new File(dir, this.Place.Name);
		if(!dir.exists()){
			if(!dir.mkdirs()){
				return null;
			}
		}
		if(this.Grave != null){
    		dir = new File(dir, this.Grave.Name);
    		if(!dir.exists()){
    			if(!dir.mkdirs()){
    				return null;
    			}
    		}
		}
		File newFile = null;
		if(fileName != null){
		    newFile = new File(dir.getPath() + File.separator + fileName);
		} else {
		    String timeValue = String.valueOf(System.currentTimeMillis());
	        newFile = new File(dir.getPath() + File.separator + timeValue  + Settings.JPG_EXTENSION);
		}
		
		return Uri.fromFile(newFile);
	}
	
	public File getPhotoFolder(){
		File rootDir = Settings.getRootDirPhoto();
		File dir = null;
		if(this.Cemetery != null){
			dir = new File(rootDir, this.Cemetery.Name);
			if(!dir.exists()){
				return null;
			}
		}
		
		if(this.Region != null){
			dir = new File(dir, this.Region.Name);
			if(!dir.exists()){
				return null;
			}
		}
		
		if(this.Row != null){
			dir = new File(dir, this.Row.Name);
			if(!dir.exists()){
				return null;
			}
		}
		
		if(this.Place != null){
			dir = new File(dir, this.Place.Name);
			if(!dir.exists()){
				return null;
			}
		}
		
		if(this.Grave != null){
			dir = new File(dir, this.Grave.Name);
			if(!dir.exists()){
				return null;
			}
		}
		return dir;
	}
	
	public Uri generateFileUri(String fileName) {
        File rootDir = Settings.getRootDirPhoto();
        return this.generateFileUri(rootDir, fileName);               
    }	
	
	public static boolean renameCemetery(Cemetery cemetery, String oldCemeteryName){
		MonumentDB monumentDB = new MonumentDB();
		ComplexGrave complexGrave = new ComplexGrave();
		complexGrave.loadByCemeteryId(cemetery.Id);
		File photoDir = Settings.getRootDirPhoto();
		File source = new File(photoDir.getPath(), oldCemeteryName);
		if(!source.exists()){
			return true;
		}
		File dest = new File(photoDir.getPath(), cemetery.Name);
		Uri oldPartOfPathURI = Uri.fromFile(source);
		Uri newPartOfPathURI = Uri.fromFile(dest);		
		if(!dest.exists()){
			source.renameTo(dest);
		} else {
			
		}		
		monumentDB.updateGravePhotoUriString(cemetery, oldPartOfPathURI.toString(), newPartOfPathURI.toString());
		return true;
	}
	
	public static boolean renameRegion(Region region, String oldRegionName){
		MonumentDB monumentDB = new MonumentDB();
		ComplexGrave complexGrave = new ComplexGrave();
		complexGrave.loadByRegionId(region.Id);
		File photoDir = Settings.getRootDirPhoto();
		String firstPartOfPath = photoDir.getPath() + File.separator + complexGrave.Cemetery.Name;
		File source = new File(firstPartOfPath, oldRegionName);
		if(!source.exists()){
			return true; // ��� ���������� ��� �����������
		}
		File dest = new File(firstPartOfPath, region.Name);
		Uri oldPartOfPathURI = Uri.fromFile(source);
		Uri newPartOfPathURI = Uri.fromFile(dest);		
		if(!dest.exists()){
			source.renameTo(dest);
		} else {
			
		}		
		monumentDB.updateGravePhotoUriString(region, oldPartOfPathURI.toString(), newPartOfPathURI.toString());
		return true;
	}
	
	public static boolean renameRow(Row row, String oldRowName){
		MonumentDB monumentDB = new MonumentDB();
		ComplexGrave complexGrave = new ComplexGrave();
		complexGrave.loadByRowId(row.Id);
		File photoDir = Settings.getRootDirPhoto();
		String firstPartOfPath = photoDir.getPath() + File.separator + complexGrave.Cemetery.Name +
				File.separator + complexGrave.Region.Name;
		File source = new File(firstPartOfPath, oldRowName);
		if(!source.exists()){
			return true; // ��� ���������� ��� �����������
		}
		File dest = new File(firstPartOfPath, row.Name);
		Uri oldPartOfPathURI = Uri.fromFile(source);
		Uri newPartOfPathURI = Uri.fromFile(dest);		
		if(!dest.exists()){
			source.renameTo(dest);
		} else {
			
		}		
		monumentDB.updateGravePhotoUriString(row, oldPartOfPathURI.toString(), newPartOfPathURI.toString());
		return true;
	}
	
	public static boolean renamePlace(Place place, String oldPlaceName){
		MonumentDB monumentDB = new MonumentDB();
		ComplexGrave complexGrave = new ComplexGrave();
		complexGrave.loadByPlaceId(place.Id);
		File photoDir = Settings.getRootDirPhoto();
		String firstPartOfPath = null;
		if(complexGrave.Row != null){
			firstPartOfPath = photoDir.getPath() + File.separator + complexGrave.Cemetery.Name +
					File.separator + complexGrave.Region.Name + File.separator + complexGrave.Row.Name;
		} else {
			firstPartOfPath = photoDir.getPath() + File.separator + complexGrave.Cemetery.Name +
					File.separator + complexGrave.Region.Name;
		}
		
		File source = new File(firstPartOfPath, oldPlaceName);
		if(!source.exists()){
			return true; // ��� ���������� ��� �����������
		}
		File dest = new File(firstPartOfPath, place.Name);
		Uri oldPartOfPathURI = Uri.fromFile(source);
		Uri newPartOfPathURI = Uri.fromFile(dest);		
		if(!dest.exists()){
			source.renameTo(dest);
		} else {
			
		}		
		monumentDB.updateGravePhotoUriString(place, oldPartOfPathURI.toString(), newPartOfPathURI.toString());
		return true;
	}

	public static boolean renameGrave(Grave grave, String oldGraveName){
		MonumentDB monumentDB = new MonumentDB();
		ComplexGrave complexGrave = new ComplexGrave();
		complexGrave.loadByGraveId(grave.Id);
		File photoDir = Settings.getRootDirPhoto();
		String firstPartOfPath = null;
		if(complexGrave.Row != null){
			firstPartOfPath = photoDir.getPath() + File.separator + complexGrave.Cemetery.Name +
					File.separator + complexGrave.Region.Name + File.separator + complexGrave.Row.Name +
					File.separator + complexGrave.Place.Name;
		} else {
			firstPartOfPath = photoDir.getPath() + File.separator + complexGrave.Cemetery.Name +
					File.separator + complexGrave.Region.Name + File.separator + complexGrave.Place.Name;
		}
		
		File source = new File(firstPartOfPath, oldGraveName);
		if(!source.exists()){
			return true; // ��� ���������� ��� �����������
		}
		File dest = new File(firstPartOfPath, grave.Name);
		Uri oldPartOfPathURI = Uri.fromFile(source);
		Uri newPartOfPathURI = Uri.fromFile(dest);		
		if(!dest.exists()){
			source.renameTo(dest);
		} else {
			
		}		
		monumentDB.updateGravePhotoUriString(grave, oldPartOfPathURI.toString(), newPartOfPathURI.toString());
		return true;
	}

	

}
