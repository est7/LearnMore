

## room 数据库

有三个比较重要的部分

- @Database：定义一个数据库, 同时必须使用entities属性定义包含哪些表；使用version属性表示数据库的版本号，用于数据库升级使用；同时对于Dao的实例化也是定义在@Database所注解的class内
- @Dao：定义操作数据库表的各种api（比如：对表的增删改查）
- @Entity（实体类）：定义一个table，实体类的每个属性 表示table的每个字段, 除非你用@Ignore 注解

### @Database 注解定义一个数据库

- 定义一个抽象类继承RoomDatabase
- 使用@Database注解这个抽象类，同时使用entities属性配置表，version配置版本号
- 定义一系列的Dao层的抽象方法

然后build后，会自动生成 继承AppDataBase 的 AppDataBase_Impl 类，并自动实现所有的抽象方法

```kotlin
@Database(entities = [User::class, Course::class, Teacher::class, UserJoinCourse::class, IDCard::class], version = 1)
abstract class AppDataBase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun teacherDao(): TeacherDao
    abstract fun courseDao(): CourseDao
    abstract fun userJoinCourseDao(): UserJoinCourseDao
    abstract fun idCardDao(): IDCardDao
}
```

### @Dao 注解定义一个数据库

- 定义一个接口或抽象类，并使用@Dao注解这个类
- 定义各种操作表的抽象方法，并使用@Query等注解对应的抽象方法

然后build后，会自动生成 继承 UserDao 的 UserDao_Impl 类，并自动实现所有的抽象方法

```
@Dao
abstract class UserDao {

    @Query("select * from tab_user")
    abstract fun getAll(): List<User>

    @Query("select * from tab_user where uid = :uid")
    abstract fun getById(uid: Long): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(vararg users: User): LongArray

    @Update
    abstract fun update(user: User)

    @Delete
    abstract fun delete(vararg users: User): Int
}
复制代码
```

### @Entity注解定义table

```
@Entity(tableName = "tab_user")
data class User(
    @ColumnInfo(name = "uid") // 定义列的名字
    @PrimaryKey(autoGenerate = true) // 标示主键，并自增长
    var uid: Long?,

    @ColumnInfo // 如果没有指定列的名字，则使用字段名称
    var username: String?,

    // @ColumnInfo 是非必须的，room 默认会将所有class的所有字段定义table的列
    var age: Int = 0
)
复制代码
```

### 定义联合主键

如果你需要使用多个字段一起当作主键，则需要使用@Entity注解中的primaryKeys属性定义联合主键

```
@Entity(primaryKeys = arrayOf("firstName", "lastName"))
data class User(
    val firstName: String?,
    val lastName: String?
)
复制代码
```

### 忽略某个字段

可以使用@Ignore注解

```
@Entity(primaryKeys = arrayOf("firstName", "lastName"))
data class User(
    val firstName: String?,
    val lastName: String?,
    @Ignore val picture: Bitmap?
)
复制代码
```

也可以使用@Entity注解中的ignoredColumns属性

```
@Entity(
primaryKeys = ["firstName", "lastName"],
ignoredColumns = ["picture"]
)
data class User(
    val firstName: String?,
    val lastName: String?,
    val picture: Bitmap?
)
复制代码
```

### 添加索引

使用@Entity注解中的indices属性添加索引

```
@Entity(indices = [Index(value = ["lastName", "address"])])
data class User(
    @PrimaryKey val id: Int,
    val firstName: String?,
    val address: String?,
    val lastName: String?
)
复制代码
```

### 定义外键关联

使用@Entity注解中的foreignKeys属性可以定义两个表之间的外键关联

```
@Entity(tableName = "tab_course")
data class Course(
    @ColumnInfo(name = "cid") @PrimaryKey(autoGenerate = true) var cid: Long? = null,
    @ColumnInfo var name: String
)

@Entity(tableName = "tab_teacher", foreignKeys = [ForeignKey(
    entity = Course::class,
    childColumns = ["cid"], // tab_teacher的列名
    parentColumns = ["cid"] // 关联的tab_course表的主键列名
	)], indices = [Index("cid")]
)
data class Teacher(
    @ColumnInfo(name = "tid") @PrimaryKey(autoGenerate = true) var tid: Long? = null,
    var name: String,
    var cid: Long? = null
)
复制代码
```

### 一对一的关联关系

使用 外键关联 + 唯一约束 表示 一对一的关联关系

比如：一个用户只能有一张身份证，一张身份证只能被一个用户拥有

```
// 定义user表
@Entity(tableName = "tab_user")
data class User(
    @ColumnInfo(name = "uid")
    @PrimaryKey(autoGenerate = true)
    var uid: Long?,
    @ColumnInfo
    var username: String?,
    var age: Int = 0
)

// 定义身份证表，并与user表建立一对一的关联关系
@Entity(
    tableName = "tab_id_card",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["uid"],
        childColumns = ["uid"]
    )],
    indices = [
        Index("_uuid", unique = true),
        Index("uid", unique = true) // 标示唯一约束，则与tab_user是一对一的关系
    ]
)
class IDCard(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    @ColumnInfo(name = "_uuid")
    var uuid: String,
    var startTime: String,
    var expireTime: String,
    @ColumnInfo(name = "uid")
    var userId: Long?
)
复制代码
```

个人建议为了方便操作，一般在user表下定一个用 @Ignore 注解的idCard字段，比如：

```
@Entity(tableName = "tab_user")
data class User(
    @ColumnInfo(name = "uid")
    @PrimaryKey(autoGenerate = true)
    var uid: Long?,
    @ColumnInfo
    var username: String?,
    var age: Int = 0
) {
	//为了方便操作，所以定义一个idCard字段
    @Ignore var idCard: IDCard? = null
}
复制代码
```

然后UserDao的实现如下：

```
@Dao
abstract class UserDao {

    @Query("select * from tab_user where uid = :uid")
    abstract fun getById(uid: Long): User?
    
    // 查询user时，同时也查询idcard
    fun getByIdWithIdCard(uid: Long): User? {
        val user = getById(uid)
        user?.let {
            it.idCard = AppDataBase.getInstance().idCardDao().getByForeignKey(it.uid!!)
        }
        return user
    }
}

@Dao
abstract class IDCardDao {
    @Query("select * from tab_id_card where uid in (:uid)")
    abstract fun getByForeignKey(uid: Long): IDCard?
    
    ...
}
复制代码
```

### 一对多的关联关系

使用 外键关联 表示 一对多的关联关系

比如：一个老师只能教一门 课程，一门课程 可以 被多个老师教

```
// 定义课程table
@Entity(tableName = "tab_course")
data class Course(
    @ColumnInfo(name = "cid") @PrimaryKey(autoGenerate = true) var cid: Long? = null,
    @ColumnInfo var name: String
) {
    // 同理 为了 方便操作，定义一个teachers字段
    @Ignore
    var teachers: List<Teacher>? = null
}

//定义老师table，并与tab_course建立一对多的关系
@Entity(tableName = "tab_teacher", foreignKeys = [ForeignKey(
    entity = Course::class,
    childColumns = ["cid"],
    parentColumns = ["cid"]
)], indices = [Index("cid")]
)
data class Teacher(
    @ColumnInfo(name = "tid") @PrimaryKey(autoGenerate = true) var tid: Long? = null,
    var name: String,
    var cid: Long? = null
) {
    // 同理 为了 方便操作，定义一个course字段
    @Ignore
    var course: Course? = null
}
复制代码
```

然后为了方便操作，CourseDao 和 TeacherDao的实现如下：

```
@Dao
abstract class CourseDao {
    @Query("select * from tab_course where cid = :cid")
    abstract fun getById(cid: Long): Course?

    fun getByIdWithTeacher(cid: Long): Course? {
        return getById(cid)?.apply {
            this.teachers = AppDataBase.getInstance().teacherDao().getByForeignKey(this.cid!!)
            this.teachers?.forEach {
                it.course = this
            }
        }
    }
    ...
}

@Dao
abstract class TeacherDao {
    @Query("select * from tab_teacher where tid = :tid")
    abstract fun getById(tid: Long): Teacher?

    @Query("select * from tab_teacher where cid = :cid")
    abstract fun getByForeignKey(cid: Long): List<Teacher>
    ...
}
复制代码
```

### 多对多的关联关系

多对多的关联关系，需要一个中间表来表示

比如：一个user可以学多门课程，一门课程也可以被多个user学习

中间表结构如下：

```
@Entity(
    tableName = "tab_user_join_course",
    indices = [Index(value = ["uid", "cid"], unique = true)],
    foreignKeys = [
        // 外键关联user表
        ForeignKey(entity = User::class, childColumns = ["uid"], parentColumns = ["uid"], onDelete = ForeignKey.CASCADE),
        // 外键关联课程表
        ForeignKey(entity = Course::class, childColumns = ["cid"], parentColumns = ["cid"])
    ]
)
data class UserJoinCourse(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    @ColumnInfo(name = "uid") var uid: Long,
    @ColumnInfo(name = "cid") var cid: Long
)
复制代码
```

UserJoinCourseDao的实现如下：

```
@Dao
interface UserJoinCourseDao {

    @Query("""
        select * from tab_user
        inner join tab_user_join_course on tab_user.uid = tab_user_join_course.uid
        where tab_user_join_course.cid = :cid
    """)
    fun getUsersByCourseId(cid: Long): List<User>

    @Query("""
        select * from tab_course
        inner join tab_user_join_course on tab_course.cid = tab_user_join_course.cid
        where tab_user_join_course.uid = :uid
    """)
    fun getCoursesByUserId(uid: Long): List<Course>

    @Insert
    fun insert(vararg userJoinCourses: UserJoinCourse)
}
复制代码
```

### @Relation注解

@Relation注解用在查询表的数据时，自动查询关联的其它数据

@Relation注解 不能用于@Entity注解的实体类中

@Relation注解 只能用于一对多（返回值必须是一个集合）

比如：先定义一个CourseWithTeacher类

```
class CourseWithTeacher (
    @Embedded var course: Course,

    @Relation(
        // entity 标示关联查询的表（非必须），默认匹配返回类型的表
        entity = Teacher::class,
        // parentColumn 表示 Course 表中的字段（可以是Course表中的任意字段）
        // entityColumn 表示 Teacher表的 用于 查询的字段(可以是Teacher表中的任意字段)
        // 最后的子查询语句是 （example：SELECT `tid`,`name`,`cid` FROM `tab_teacher` where :entityColumn in [:parentColumn]）
        parentColumn = "cid",
        entityColumn = "cid")
    var teachers: List<Teacher>
)
复制代码
```

然后修改 CourseDao的实现

```
@Dao
abstract class CourseDao {
    @Query("select * from tab_course")
    abstract fun getAll(): List<CourseWithTeacher>
}
复制代码
```

### 及联删除策略

当两张表有外键关联关系的时候，比如`tab_user` 和 `tab_id_card`表用`ForeignKey`实现了一对一的关联关系，当删除`tab_user`表的一条数据时，如果这条数据被`tab_id_card`关联，则删除失败，会报 `android.database.sqlite.SQLiteConstraintException: FOREIGN KEY constraint failed`错误；这时可以给`ForeignKey`添加onDelete属性配置及联删除策略

- `ForeignKey.NO_ACTION`: 默认策略，不会做任何处理，发现删除的数据被关联，则直接报错

- `ForeignKey.RESTRICT` : 同`NO_ACTION`效果一样, 但它会先检查约束

- `ForeignKey.SET_NULL` :`tab_user`表删除一条数据时，则将对应的`tab_id_card`表的`uid`的值设置成`NULL`

- `ForeignKey.SET_DEFAULT` :`tab_user`表删除一条数据时，则将对应的`tab_id_card`表的`uid`的值设置成默认值，但是由于room暂时没办法给column设置默认值，所以还是会设置成 `NULL`

- `ForeignKey.CASCADE` ：`tab_user`表删除一条数据时，则同时也会删除`tab_id_card`表的所对应的数据

  ```
    @Entity(
        tableName = "tab_id_card",
        foreignKeys = [ForeignKey(
            entity = User::class,
            parentColumns = ["uid"],
            childColumns = ["uid"],
            onDelete = ForeignKey.CASCADE
        )],
        indices = [
            Index("_uuid", unique = true),
            Index("uid", unique = true) // 标示唯一约束，则与tab_user是一对一的关系
        ]
    )
    class IDCard(
        @PrimaryKey(autoGenerate = true) var id: Long?,
        @ColumnInfo(name = "_uuid")
        var uuid: String,
        var startTime: String,
        var expireTime: String,
        @ColumnInfo(name = "uid")
        var userId: Long? = null
    )
  复制代码
  ```

及联更新策略 同 删除策略一致，只是通过onUpdate配置

## 数据库升级或降级

### room 数据库升级规则如下

比如在app的版本迭代过程中`version(版本号)`经历了`1、2、3、4`的变更

使用`Migration`配置每个版本的更新规则，其构造函数必须指定`startVersion` 和 `endVersion`

代码实现如下：

```
private fun createAppDataBase(context: Context): AppDataBase {
    return Room.databaseBuilder(context, AppDataBase::class.java, "db_example")
        .addMigrations(object : Migration(1, 2) { // 从1升级到2的实现
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.i("AppDataBase", "===Migration 1-2===")
                // do something
            }
        }).addMigrations(object : Migration(2, 3) {// 从2升级到3的实现
            override fun migrate(database: SupportSQLiteDatabase) {
            Log.i("AppDataBase", "===Migration 2-3===")
                // do something
            }
        })
        .addMigrations(object : Migration(3, 4) {// 从3升级到4的实现
            override fun migrate(database: SupportSQLiteDatabase) {
            Log.i("AppDataBase", "===Migration 3-4===")
                // do something
            }
        })
        .addMigrations(object : Migration(1, 3) {// 从1升级到3的实现
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.i("AppDataBase", "===Migration 1-3===")
                // do something
            }
        }).addMigrations(object : Migration(2, 4) {// 从2升级到4的实现
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.i("AppDataBase", "===Migration 2-4===")
                // do something
            }
        })
        .build()
}
复制代码
```

| 当前app数据库version | 最新的app数据库version | 升级规则                   | 打印结果                                |
| -------------------- | ---------------------- | -------------------------- | --------------------------------------- |
| 1                    | 4                      | 先从1升级到3，再从3升级到4 | ===Migration 1-3=== ===Migration 3-4=== |
| 2                    | 4                      | 直接从2升级到4             | ===Migration 2-4===                     |
| 3                    | 4                      | 从3升级到4                 | ===Migration 3-4===                     |
| 4                    | 4                      | 不变                       |                                         |

总结规则如下（以当前version == 1，最新version == 4 为例 ）：

- 先从当前 `version` 作为 `startVersion`, 匹配最大的`endVersion`（即：先从1升级到3）
- 然后再以上面匹配的`endVersion`最为`startVersion`，又匹配最大的`endVersion`（即：再从3升级到4）

### 如果没有匹配到对应的升级Migration配置怎么办呢？

默认情况下，如果没有匹配到升级策略，则app 直接 crash

为了防止crash，可添加`fallbackToDestructiveMigration`方法配置 直接删除所有的表，重新创建表

```
private fun createAppDataBase(context: Context): AppDataBase {
    return Room.databaseBuilder(context, AppDataBase::class.java, "db_example")
        .addMigrations(object : Migration(1, 2) { // 从1升级到2的实现
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.i("AppDataBase", "===Migration 1-2===")
                // do something
            }
         ...   
         // 如果没有匹配到Migration，则直接删除所有的表，重新创建表
        .fallbackToDestructiveMigration()
        .build()
}
复制代码
```

### 指定版本号删表重建

例如当前`version` 是 1或2, 升级到 4 非常麻烦，工作量太大，还不如直接删库重建，这个时候就可以调用`fallbackToDestructiveMigrationFrom`方法指定当前`version`是多少的时候删表重建

```
private fun createAppDataBase(context: Context): AppDataBase {
    return Room.databaseBuilder(context, AppDataBase::class.java, "db_example")
        .addMigrations(object : Migration(3, 4) {// 从1升级到3的实现
            override fun migrate(database: SupportSQLiteDatabase) {
            Log.i("AppDataBase", "===Migration 3-4===")
                // do something
            }
        })
        // 如果没有匹配到Migration，则直接删除所有的表，重新创建表
        .fallbackToDestructiveMigration()
        // 需要配合fallbackToDestructiveMigration方法使用，指定当前`version` 是 1或2，则直接删除所有的表，重新创建表
        .fallbackToDestructiveMigrationFrom(1, 2)
        .build()
}
复制代码
```

### room 数据库降级规则如下

比如在app的版本迭代过程中`version(版本号)`经历了`1、2、3、4`的变更，当前是4，需要降级到1

```
private fun createAppDataBase(context: Context): AppDataBase {
    return Room.databaseBuilder(context, AppDataBase::class.java, "db_example")
        .addMigrations(object : Migration(4, 3) { // 从4降级到3的实现
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.i("AppDataBase", "===Migration 4-3===")
                // do something
            }
        }).addMigrations(object : Migration(3, 2) {// 从3降级到2的实现
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.i("AppDataBase", "===Migration 3-2===")
                // do something
            }
        })
        .addMigrations(object : Migration(2, 1) {// 从2降级到1的实现
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.i("AppDataBase", "===Migration 2-1===")
                // do something
            }
        })
        .addMigrations(object : Migration(4, 2) {// 从4降级到2的实现
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.i("AppDataBase", "===Migration 4-2===")
                // do something
            }
        }).addMigrations(object : Migration(3, 1) {// 从3降级到1的实现
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.i("AppDataBase", "===Migration 3-1===")
                // do something
            }
        })
        .build()
}
复制代码
```

| 当前app数据库version | 降级到app数据库version | 升级规则                   | 打印结果                                |
| -------------------- | ---------------------- | -------------------------- | --------------------------------------- |
| 4                    | 1                      | 先从4降级到2，再从2降级到1 | ===Migration 4-2=== ===Migration 2-1=== |
| 4                    | 2                      | 直接从4降级到2             | ===Migration 4-2===                     |
| 4                    | 3                      | 从4降级到3                 | ===Migration 4-3===                     |
| 4                    | 4                      | 不变                       |                                         |

总结规则如下（以当前version == 4，降级到version == 1 为例 ）：

- 先从当前 `version` 作为 `startVersion`, 匹配最小的`endVersion`（即：先从4降级到2）
- 然后再以上面匹配的`endVersion`最为`startVersion`，又匹配最小的`endVersion`（即：再从2降级到1）

### 同理如果没有匹配到降级规则，默认也会crash；可以通过fallbackToDestructiveMigrationOnDowngrade方法配置删表重建，但不能指定version删表重建

```
private fun createAppDataBase(context: Context): AppDataBase {
    return Room.databaseBuilder(context, AppDataBase::class.java, "db_example")
        .addMigrations(object : Migration(4, 3) { // 从4降级到3的实现
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.i("AppDataBase", "===Migration 4-3===")
                // do something
            }
        })
        // 如果没有匹配到降级Migration，则删表重建
        .fallbackToDestructiveMigrationOnDowngrade()
        .build()
}
复制代码
```

### 数据库升级常用方法

从1 升级到 2，添加一张表tab_test表

```
private fun createAppDataBase(context: Context): AppDataBase {
    return Room.databaseBuilder(context, AppDataBase::class.java, "db_example")
        .addMigrations(object : Migration(1, 2) { // 从1升级到2的实现
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.i("AppDataBase", "===Migration 1-2===")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tab_test` (
                        `uid` INTEGER PRIMARY KEY AUTOINCREMENT,
                        `username` TEXT,
                        `age` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        })
        .fallbackToDestructiveMigration()
        .fallbackToDestructiveMigrationOnDowngrade()
        .build()
}
复制代码
```

从2 升级到 3，给tab_test表添加 desc 字段

```
private fun createAppDataBase(context: Context): AppDataBase {
    return Room.databaseBuilder(context, AppDataBase::class.java, "db_example")
        .addMigrations(object : Migration(1, 2) { // 从1升级到2的实现
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.i("AppDataBase", "===Migration 1-2===")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tab_test` (
                        `uid` INTEGER PRIMARY KEY AUTOINCREMENT,
                        `username` TEXT,
                        `age` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        })
        .addMigrations(object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.i("AppDataBase", "===Migration 2-3===")
                database.execSQL("ALTER TABLE `tab_test` ADD COLUMN `desc` TEXT")
            }
        })
        .fallbackToDestructiveMigration()
        .fallbackToDestructiveMigrationOnDowngrade()
        .build()
}
复制代码
```

从3 升级到 4，给tab_test表 desc 字段 重命名为 desc2

```
private fun createAppDataBase(context: Context): AppDataBase {
    return Room.databaseBuilder(context, AppDataBase::class.java, "db_example")
        .addMigrations(object : Migration(1, 2) { // 从1升级到2的实现
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.i("AppDataBase", "===Migration 1-2===")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tab_test` (
                        `uid` INTEGER PRIMARY KEY AUTOINCREMENT,
                        `username` TEXT,
                        `age` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        })
        .addMigrations(object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.i("AppDataBase", "===Migration 2-3===")
                database.execSQL("ALTER TABLE `tab_test` ADD COLUMN `desc` TEXT")
            }
        })
        .addMigrations(object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.i("AppDataBase", "===Migration 3-4===")
                // 重命名tmp_tab_test
                database.execSQL("ALTER TABLE `tab_test` RENAME TO `tmp_tab_test`")
                // 重新创建表tab_test
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tab_test` (
                        `uid` INTEGER PRIMARY KEY AUTOINCREMENT,
                        `username` TEXT,
                        `age` INTEGER NOT NULL,
                        `desc2` TEXT
                    )
                """.trimIndent())
                // 将表tmp_tab_test的数据复制到tab_test
                database.execSQL("insert into `tab_test` select * from `tmp_tab_test`")
                // 删除tmp_tab_test表
                database.execSQL("drop table `tmp_tab_test`")
            }
        })
        .fallbackToDestructiveMigration()
        .fallbackToDestructiveMigrationOnDowngrade()
        .build()
}
复制代码
```

### gradle 依赖

```
implementation "androidx.room:room-runtime:2.1.0"
kapt "androidx.room:room-compiler:2.1.0" // For Kotlin use kapt instead of annotationProcessor
复制代码
```

## 其它建议

### 强烈建议使用 `facebook` 的 [stetho](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Ffacebook%2Fstetho) `library` 配合调试你的数据库

\###快速查询tab_user表的建表语句（方便升级时建表使用）

```
SELECT sql FROM sqlite_master WHERE name='tab_user';
```