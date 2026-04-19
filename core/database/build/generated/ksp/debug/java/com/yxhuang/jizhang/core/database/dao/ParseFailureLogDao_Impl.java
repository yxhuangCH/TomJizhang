package com.yxhuang.jizhang.core.database.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.yxhuang.jizhang.core.database.entity.ParseFailureLogEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ParseFailureLogDao_Impl implements ParseFailureLogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ParseFailureLogEntity> __insertionAdapterOfParseFailureLogEntity;

  public ParseFailureLogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfParseFailureLogEntity = new EntityInsertionAdapter<ParseFailureLogEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `parse_failure_logs` (`id`,`rawText`,`sourceApp`,`timestamp`,`reason`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ParseFailureLogEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getRawText());
        statement.bindString(3, entity.getSourceApp());
        statement.bindLong(4, entity.getTimestamp());
        if (entity.getReason() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getReason());
        }
      }
    };
  }

  @Override
  public Object insert(final ParseFailureLogEntity entity,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfParseFailureLogEntity.insertAndReturnId(entity);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ParseFailureLogEntity>> observeAll() {
    final String _sql = "SELECT * FROM parse_failure_logs ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"parse_failure_logs"}, new Callable<List<ParseFailureLogEntity>>() {
      @Override
      @NonNull
      public List<ParseFailureLogEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRawText = CursorUtil.getColumnIndexOrThrow(_cursor, "rawText");
          final int _cursorIndexOfSourceApp = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceApp");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfReason = CursorUtil.getColumnIndexOrThrow(_cursor, "reason");
          final List<ParseFailureLogEntity> _result = new ArrayList<ParseFailureLogEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ParseFailureLogEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpRawText;
            _tmpRawText = _cursor.getString(_cursorIndexOfRawText);
            final String _tmpSourceApp;
            _tmpSourceApp = _cursor.getString(_cursorIndexOfSourceApp);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpReason;
            if (_cursor.isNull(_cursorIndexOfReason)) {
              _tmpReason = null;
            } else {
              _tmpReason = _cursor.getString(_cursorIndexOfReason);
            }
            _item = new ParseFailureLogEntity(_tmpId,_tmpRawText,_tmpSourceApp,_tmpTimestamp,_tmpReason);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getById(final long id,
      final Continuation<? super ParseFailureLogEntity> $completion) {
    final String _sql = "SELECT * FROM parse_failure_logs WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ParseFailureLogEntity>() {
      @Override
      @Nullable
      public ParseFailureLogEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRawText = CursorUtil.getColumnIndexOrThrow(_cursor, "rawText");
          final int _cursorIndexOfSourceApp = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceApp");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfReason = CursorUtil.getColumnIndexOrThrow(_cursor, "reason");
          final ParseFailureLogEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpRawText;
            _tmpRawText = _cursor.getString(_cursorIndexOfRawText);
            final String _tmpSourceApp;
            _tmpSourceApp = _cursor.getString(_cursorIndexOfSourceApp);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpReason;
            if (_cursor.isNull(_cursorIndexOfReason)) {
              _tmpReason = null;
            } else {
              _tmpReason = _cursor.getString(_cursorIndexOfReason);
            }
            _result = new ParseFailureLogEntity(_tmpId,_tmpRawText,_tmpSourceApp,_tmpTimestamp,_tmpReason);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
