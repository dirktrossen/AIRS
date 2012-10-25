/*
Copyright (C) 2012, Dirk Trossen, airs@dirk-trossen.de

This program is free software; you can redistribute it and/or modify it
under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation as version 2.1 of the License.

This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this library; if not, write to the Free Software Foundation, Inc.,
59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
*/
package com.airs;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AIRS_database extends SQLiteOpenHelper 
{
	public static final String DATABASE_NAME = "AIRS";
    private static final int DATABASE_VERSION = 2;
    public static final String DATABASE_TABLE_NAME = "airs_values";
    public static final String DATABASE_TABLE_CREATE =
                "CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE_NAME + " ( Timestamp BIGINT, Symbol CHAR(2), Value TEXT);";
    public static final String DATABASE_TABLE_CREATE2 =
        "CREATE TABLE IF NOT EXISTS airs_dates (Year INT, Month INT, Day INT, Types INT);";
    public static final String DATABASE_TABLE_CREATE3 =
        "CREATE TABLE IF NOT EXISTS airs_sensors_used (Timestamp BIGINT, Symbol CHAR(2));";
    public static final String DATABASE_TABLE_INDEX3= 
    	"CREATE INDEX IF NOT EXISTS airs_sensors_used_timestamp ON airs_sensors_used (Timestamp)";
    
    AIRS_database(Context context) 
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_TABLE_CREATE);
        db.execSQL(DATABASE_TABLE_CREATE2);
        db.execSQL(DATABASE_TABLE_CREATE3);
        db.execSQL(DATABASE_TABLE_INDEX3);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
    }
}
