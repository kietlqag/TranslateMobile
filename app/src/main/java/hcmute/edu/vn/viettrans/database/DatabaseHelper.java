package hcmute.edu.vn.viettrans.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "translation_history.db";
    private static final int DATABASE_VERSION = 1;

    // Table name and columns
    public static final String TABLE_HISTORY = "translation_history";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_SOURCE_TEXT = "source_text";
    public static final String COLUMN_TRANSLATED_TEXT = "translated_text";
    public static final String COLUMN_SOURCE_LANG = "source_lang";
    public static final String COLUMN_TARGET_LANG = "target_lang";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    // Create table query
    private static final String CREATE_TABLE_HISTORY = 
        "CREATE TABLE " + TABLE_HISTORY + " (" +
        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_SOURCE_TEXT + " TEXT NOT NULL, " +
        COLUMN_TRANSLATED_TEXT + " TEXT NOT NULL, " +
        COLUMN_SOURCE_LANG + " TEXT NOT NULL, " +
        COLUMN_TARGET_LANG + " TEXT NOT NULL, " +
        COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
        ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_HISTORY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }
} 