package hcmute.edu.vn.viettrans.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.viettrans.model.TranslationHistory;

public class TranslationHistoryDAO {
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public TranslationHistoryDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public long insertHistory(TranslationHistory history) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_SOURCE_TEXT, history.getSourceText());
        values.put(DatabaseHelper.COLUMN_TRANSLATED_TEXT, history.getTranslatedText());
        values.put(DatabaseHelper.COLUMN_SOURCE_LANG, history.getSourceLang());
        values.put(DatabaseHelper.COLUMN_TARGET_LANG, history.getTargetLang());

        return database.insert(DatabaseHelper.TABLE_HISTORY, null, values);
    }

    public List<TranslationHistory> getAllHistory() {
        List<TranslationHistory> historyList = new ArrayList<>();
        String[] columns = {
            DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_SOURCE_TEXT,
            DatabaseHelper.COLUMN_TRANSLATED_TEXT,
            DatabaseHelper.COLUMN_SOURCE_LANG,
            DatabaseHelper.COLUMN_TARGET_LANG,
            DatabaseHelper.COLUMN_TIMESTAMP
        };

        Cursor cursor = database.query(
            DatabaseHelper.TABLE_HISTORY,
            columns,
            null,
            null,
            null,
            null,
            DatabaseHelper.COLUMN_TIMESTAMP + " DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                TranslationHistory history = new TranslationHistory();
                history.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
                history.setSourceText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SOURCE_TEXT)));
                history.setTranslatedText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSLATED_TEXT)));
                history.setSourceLang(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SOURCE_LANG)));
                history.setTargetLang(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TARGET_LANG)));
                history.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP)));
                historyList.add(history);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return historyList;
    }

    public void deleteHistory(long id) {
        database.delete(
            DatabaseHelper.TABLE_HISTORY,
            DatabaseHelper.COLUMN_ID + " = ?",
            new String[]{String.valueOf(id)}
        );
    }

    public void clearAllHistory() {
        database.delete(DatabaseHelper.TABLE_HISTORY, null, null);
    }
} 