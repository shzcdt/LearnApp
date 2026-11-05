package ai.learnharness.mobile.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
	entities = [SessionEntity::class, MessageEntity::class],
	version = 1,
	exportSchema = false,
)
abstract class LhDatabase : RoomDatabase() {

	abstract fun dao(): LhDao

	companion object {
		@Volatile
		private var instance: LhDatabase? = null

		fun get(context: Context): LhDatabase =
			instance ?: synchronized(this) {
				instance ?: Room.databaseBuilder(
					context.applicationContext,
					LhDatabase::class.java,
					"learnharness.db",
				).build().also { instance = it }
			}
	}
}
