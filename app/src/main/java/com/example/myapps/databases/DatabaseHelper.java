package com.example.myapps.databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "data.db";
    private static final int DATABASE_VERSION = 2; // Tingkatkan versi untuk memicu onUpgrade

    // Nama tabel dan kolom
    private static final String TABLE_SETTINGS = "settings";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_BASE_URL = "baseURL";

    // SQL untuk membuat tabel
    private static final String CREATE_TABLE_SETTINGS = "CREATE TABLE " + TABLE_SETTINGS + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_BASE_URL + " TEXT)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SETTINGS);

        // Insert URL default jika tabel baru dibuat
        ContentValues values = new ContentValues();
        values.put(COLUMN_BASE_URL, "http://192.168.1.4:8082/skripsi/20241/arsiplakoro");
        db.insert(TABLE_SETTINGS, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }

    // Metode untuk mendapatkan baseURL dari tabel settings
    public String getBaseURL() {
        SQLiteDatabase db = this.getReadableDatabase();
        String baseURL = null;

        Cursor cursor = db.query(TABLE_SETTINGS, new String[]{COLUMN_BASE_URL}, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            baseURL = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BASE_URL));
            cursor.close();
        }
        return baseURL;
    }

    // Metode untuk memperbarui baseURL di tabel settings
    public void updateBaseURL(String newBaseURL) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("baseURL", newBaseURL);  // Ganti "baseURL" dengan nama kolom yang tepat

        // Perbarui URL di baris pertama (ID=1 atau buat logika sesuai kebutuhan jika tabel memiliki lebih dari 1 baris)
        db.update("settings", values, "id = ?", new String[]{"1"});
    }

}
