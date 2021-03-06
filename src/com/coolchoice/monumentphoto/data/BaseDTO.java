package com.coolchoice.monumentphoto.data;

import org.apache.log4j.Logger;

import com.j256.ormlite.field.DatabaseField;

public class BaseDTO implements ILogable {
	
	public static final String COLUMN_NAME = "Name";
	
	public static final String COLUMN_IS_CHANGED = "IsChanged";
	
	public static final String ORDER_BY_COLUMN_NAME = "CAST (Name As INTEGER)";
	public static final String ORDER_BY_DESC_COLUMN_NAME = "CAST (Name As INTEGER) DESC";
	
	public static final String COLUMN_ID = "Id";
	
	public static final String COLUMN_SERVER_ID = "ServerId";
	
	public static final String COLUMN_PARENT_SERVER_ID = "ParentServerId";
	
	public static final int INT_NULL_VALUE = Integer.MIN_VALUE;
	
	public static final String NULL = "null";
	
	public static final String LOGGER_PATTERN = COLUMN_ID + "=%d, " + COLUMN_SERVER_ID + "=%d, " + COLUMN_PARENT_SERVER_ID + 
	        "=%d, " + COLUMN_NAME + "=%s, " + COLUMN_IS_CHANGED + "=%d";
	
	public static boolean isNullValue(int value){
	    return value == BaseDTO.INT_NULL_VALUE;
	}
	
	public BaseDTO() {
		this.ServerId = INT_NULL_VALUE;
		this.ParentServerId = INT_NULL_VALUE;
		this.IsChanged = 0;		
	}

	@DatabaseField(generatedId = true, allowGeneratedIdInsert = true)
	public int Id;
	
	@DatabaseField(index = true)
	public int ServerId;
	
	@DatabaseField(index = true)
	public int ParentServerId;
	
	@DatabaseField
	public String Name;
	
	@DatabaseField
	public byte IsChanged;
	
	
    public void toLog(Logger logger, LogOperation operation){
        logger.info(String.format("Operation:%s  Class:%s", operation.name(), this.getClass().toString()));
        logger.info(String.format(LOGGER_PATTERN, this.Id, this.ServerId, this.ParentServerId, this.Name, this.IsChanged));        
    }
	
}
