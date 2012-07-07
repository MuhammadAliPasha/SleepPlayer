/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package es.jmfrancofraiz.sleepplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class DatabaseHelper extends SQLiteOpenHelper {
    
    public static final String FIELD_ROWID = BaseColumns._ID;
    public static final String FIELD_URL = "url";
    public static final String FIELD_PLAYTIME = "playtime";
    public static final String FIELD_POSICION = "posicion";
    public static final String FIELD_DURACION = "duracion";
    
    private static final String DATABASE_TABLE = "streams";
    private static final String DB_NAME = "SleepPlayerDB";
    
    private SQLiteDatabase database;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + FIELD_URL + " VARCHAR, " + FIELD_PLAYTIME + " VARCHAR, "
                + FIELD_POSICION + " VARCHAR, " + FIELD_DURACION + " VARCHAR)");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Steps to upgrade the database for the new version ...
    }

    public void open() {
        database = this.getReadableDatabase();
    }
    
    public void close() {
        database.close();
    }
    
    public long insert(String url, String playtime, String duracion) {
        
        int max_filas = 20;
        
        //primero borramos los m‡s viejos
        //delete from streams where _id < (select min(_id) from (select _id from streams order by _id desc limit 20))
        //esto debe dejar (max_filas-1) filas en la tabla 
        database.execSQL("delete from " + DATABASE_TABLE + " where "
                + BaseColumns._ID + " < (select min(" + BaseColumns._ID
                + ") from (select " + BaseColumns._ID + " from "
                + DATABASE_TABLE + " order by " + BaseColumns._ID
                + " desc limit "+(max_filas-1)+"))");
        
        //ahora insertamos la nueva, con lo que deben quedar (max_filas) filas en la tabla
        ContentValues cv = new ContentValues();
        cv.put(FIELD_URL, url);
        cv.put(FIELD_PLAYTIME,playtime);
        cv.put(FIELD_DURACION, duracion);
        
        return database.insert(DATABASE_TABLE, null, cv);
        
    }

    public void updatePosicion(long insertedRowId, String posicion) {

        ContentValues cv = new ContentValues();
        cv.put(FIELD_POSICION, posicion);
        database.update(DATABASE_TABLE, cv, BaseColumns._ID + "=" + insertedRowId, null);
        
    }

    public void updateDuracion(long insertedRowId, String duracion) {

        ContentValues cv = new ContentValues();
        cv.put(FIELD_DURACION, duracion);
        database.update(DATABASE_TABLE, cv, BaseColumns._ID + "=" + insertedRowId, null);
        
    }
    
    public Cursor query() {
        
        return database.query(DATABASE_TABLE, new String[] { FIELD_ROWID,
                FIELD_URL, FIELD_PLAYTIME, FIELD_POSICION, FIELD_DURACION }, null, null, null,
                null, FIELD_ROWID + " desc");
        
    }

    public void deleteAll() {
        database.delete(DATABASE_TABLE, null, null);
        
    }
    
}
