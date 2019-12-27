package com.karl.kiosk.Sql.Helpers

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.karl.kiosk.Sql.Helpers.CompanyContract.CompanyEntry.COLUMN_NAME_API_TOKEN
import com.karl.kiosk.Sql.Helpers.CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_LAT
import com.karl.kiosk.Sql.Helpers.CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_LINK
import com.karl.kiosk.Sql.Helpers.CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_LONG
import com.karl.kiosk.Sql.Helpers.CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_NAME
import com.karl.kiosk.Sql.Helpers.CompanyContract.CompanyEntry.COLUMN_NAME_D
import com.karl.kiosk.Sql.Helpers.CompanyContract.CompanyEntry.COLUMN_NAME_EMPLOYEES
import com.karl.kiosk.Sql.Helpers.CompanyContract.CompanyEntry.COLUMN_NAME_LOCATION_ID
import com.karl.kiosk.Sql.Helpers.CompanyContract.CompanyEntry.COLUMN_NAME_T
import com.karl.kiosk.Sql.Helpers.CompanyContract.CompanyEntry.COLUMN_NAME_TIMEKEEPER_EMAIL
import com.karl.kiosk.Sql.Helpers.CompanyContract.CompanyEntry.COLUMN_NAME_TIMEKEEPER_ID
import com.karl.kiosk.Sql.Helpers.CompanyContract.CompanyEntry.COLUMN_NAME_TIMEKEEPER_NAME
import com.karl.kiosk.Sql.Helpers.CompanyContract.CompanyEntry.COLUMN_NAME_TIMEKEEPER_PASSWORD
import com.karl.kiosk.Sql.Helpers.CompanyContract.CompanyEntry.TABLE_NAME

    object CompanyContract {
        // Table contents are grouped together in an anonymous object.
        object CompanyEntry : BaseColumns {
            const val TABLE_NAME = "tbl_companies"
            const val COLUMN_NAME_LOCATION_ID = "Location_id"
            const val COLUMN_NAME_COMPANY_LINK = "Company_Link"
            const val COLUMN_NAME_COMPANY_NAME = "Company_name"
            const val COLUMN_NAME_TIMEKEEPER_ID = "Timekeeper_id"
            const val COLUMN_NAME_TIMEKEEPER_NAME = "Timekeeper_name"
            const val COLUMN_NAME_TIMEKEEPER_EMAIL = "Timekeeper_email"
            const val COLUMN_NAME_TIMEKEEPER_PASSWORD = "Timekeeper_password"
            const val COLUMN_NAME_API_TOKEN = "API_Token"
            const val COLUMN_NAME_D = "d"
            const val COLUMN_NAME_T = "t"
            const val COLUMN_NAME_COMPANY_LAT = "Company_lat"
            const val COLUMN_NAME_COMPANY_LONG = "Company_long"
            const val COLUMN_NAME_EMPLOYEES = "Employees"
        }
    }

    private const val SQL_CREATE_ENTRIES =
        "CREATE TABLE $TABLE_NAME (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                "$COLUMN_NAME_LOCATION_ID TEXT NOT NULL UNIQUE," +
                "$COLUMN_NAME_COMPANY_LINK TEXT," +
                "$COLUMN_NAME_COMPANY_NAME TEXT,"+
                "$COLUMN_NAME_TIMEKEEPER_ID TEXT,"+
                "$COLUMN_NAME_TIMEKEEPER_NAME TEXT,"+
                "$COLUMN_NAME_TIMEKEEPER_EMAIL TEXT,"+
                "$COLUMN_NAME_TIMEKEEPER_PASSWORD TEXT,"+
                "$COLUMN_NAME_API_TOKEN TEXT,"+
                "$COLUMN_NAME_D TEXT,"+
                "$COLUMN_NAME_T TEXT,"+
                "$COLUMN_NAME_COMPANY_LAT DOUBLE,"+
                "$COLUMN_NAME_COMPANY_LONG DOUBLE,"+
                "$COLUMN_NAME_EMPLOYEES TEXT)"


    private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"

    class CompanyDBHelper(context: Context):
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){

            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(SQL_CREATE_ENTRIES)
            }
            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                // This database is only a cache for online data, so its upgrade policy is
                // to simply to discard the data and start over
                db.execSQL(SQL_DELETE_ENTRIES)
                onCreate(db)
            }
            override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                onUpgrade(db, oldVersion, newVersion)
            }

            companion object {
                // If you change the database schema, you must increment the database version.
                const val DATABASE_VERSION = 1
                const val DATABASE_NAME = "db_kiosk"
            }
}