package com.yxhuang.jizhang.core.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.yxhuang.jizhang.core.database.dao.CategoryRuleDao;
import com.yxhuang.jizhang.core.database.dao.CategoryRuleDao_Impl;
import com.yxhuang.jizhang.core.database.dao.ParseFailureLogDao;
import com.yxhuang.jizhang.core.database.dao.ParseFailureLogDao_Impl;
import com.yxhuang.jizhang.core.database.dao.TransactionDao;
import com.yxhuang.jizhang.core.database.dao.TransactionDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class JizhangDatabase_Impl extends JizhangDatabase {
  private volatile TransactionDao _transactionDao;

  private volatile CategoryRuleDao _categoryRuleDao;

  private volatile ParseFailureLogDao _parseFailureLogDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` REAL NOT NULL, `merchant` TEXT NOT NULL, `category` TEXT, `timestamp` INTEGER NOT NULL, `sourceApp` TEXT NOT NULL, `rawText` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `category_rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `keyword` TEXT NOT NULL, `category` TEXT NOT NULL, `confidence` REAL NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `parse_failure_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `rawText` TEXT NOT NULL, `sourceApp` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `reason` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '3d00d9f9c2bd87766d8a25d27f854805')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `transactions`");
        db.execSQL("DROP TABLE IF EXISTS `category_rules`");
        db.execSQL("DROP TABLE IF EXISTS `parse_failure_logs`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsTransactions = new HashMap<String, TableInfo.Column>(8);
        _columnsTransactions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("merchant", new TableInfo.Column("merchant", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("category", new TableInfo.Column("category", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("sourceApp", new TableInfo.Column("sourceApp", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("rawText", new TableInfo.Column("rawText", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTransactions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTransactions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTransactions = new TableInfo("transactions", _columnsTransactions, _foreignKeysTransactions, _indicesTransactions);
        final TableInfo _existingTransactions = TableInfo.read(db, "transactions");
        if (!_infoTransactions.equals(_existingTransactions)) {
          return new RoomOpenHelper.ValidationResult(false, "transactions(com.yxhuang.jizhang.core.database.entity.TransactionEntity).\n"
                  + " Expected:\n" + _infoTransactions + "\n"
                  + " Found:\n" + _existingTransactions);
        }
        final HashMap<String, TableInfo.Column> _columnsCategoryRules = new HashMap<String, TableInfo.Column>(4);
        _columnsCategoryRules.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategoryRules.put("keyword", new TableInfo.Column("keyword", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategoryRules.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategoryRules.put("confidence", new TableInfo.Column("confidence", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCategoryRules = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCategoryRules = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCategoryRules = new TableInfo("category_rules", _columnsCategoryRules, _foreignKeysCategoryRules, _indicesCategoryRules);
        final TableInfo _existingCategoryRules = TableInfo.read(db, "category_rules");
        if (!_infoCategoryRules.equals(_existingCategoryRules)) {
          return new RoomOpenHelper.ValidationResult(false, "category_rules(com.yxhuang.jizhang.core.database.entity.CategoryRuleEntity).\n"
                  + " Expected:\n" + _infoCategoryRules + "\n"
                  + " Found:\n" + _existingCategoryRules);
        }
        final HashMap<String, TableInfo.Column> _columnsParseFailureLogs = new HashMap<String, TableInfo.Column>(5);
        _columnsParseFailureLogs.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsParseFailureLogs.put("rawText", new TableInfo.Column("rawText", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsParseFailureLogs.put("sourceApp", new TableInfo.Column("sourceApp", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsParseFailureLogs.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsParseFailureLogs.put("reason", new TableInfo.Column("reason", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysParseFailureLogs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesParseFailureLogs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoParseFailureLogs = new TableInfo("parse_failure_logs", _columnsParseFailureLogs, _foreignKeysParseFailureLogs, _indicesParseFailureLogs);
        final TableInfo _existingParseFailureLogs = TableInfo.read(db, "parse_failure_logs");
        if (!_infoParseFailureLogs.equals(_existingParseFailureLogs)) {
          return new RoomOpenHelper.ValidationResult(false, "parse_failure_logs(com.yxhuang.jizhang.core.database.entity.ParseFailureLogEntity).\n"
                  + " Expected:\n" + _infoParseFailureLogs + "\n"
                  + " Found:\n" + _existingParseFailureLogs);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "3d00d9f9c2bd87766d8a25d27f854805", "e644426e5a55dfb27dab65e15a4331d7");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "transactions","category_rules","parse_failure_logs");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `transactions`");
      _db.execSQL("DELETE FROM `category_rules`");
      _db.execSQL("DELETE FROM `parse_failure_logs`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(TransactionDao.class, TransactionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CategoryRuleDao.class, CategoryRuleDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ParseFailureLogDao.class, ParseFailureLogDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public TransactionDao transactionDao() {
    if (_transactionDao != null) {
      return _transactionDao;
    } else {
      synchronized(this) {
        if(_transactionDao == null) {
          _transactionDao = new TransactionDao_Impl(this);
        }
        return _transactionDao;
      }
    }
  }

  @Override
  public CategoryRuleDao categoryRuleDao() {
    if (_categoryRuleDao != null) {
      return _categoryRuleDao;
    } else {
      synchronized(this) {
        if(_categoryRuleDao == null) {
          _categoryRuleDao = new CategoryRuleDao_Impl(this);
        }
        return _categoryRuleDao;
      }
    }
  }

  @Override
  public ParseFailureLogDao parseFailureLogDao() {
    if (_parseFailureLogDao != null) {
      return _parseFailureLogDao;
    } else {
      synchronized(this) {
        if(_parseFailureLogDao == null) {
          _parseFailureLogDao = new ParseFailureLogDao_Impl(this);
        }
        return _parseFailureLogDao;
      }
    }
  }
}
