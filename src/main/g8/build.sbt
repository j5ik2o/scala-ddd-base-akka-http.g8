import Dependencies._
import Utils._
import scala.concurrent.duration._

lazy val commonSettings = Seq(
  scalaVersion := "2.12.7",
  organization := "$organization$",
  version := "1.0.0-SNAPSHOT",
  libraryDependencies ++= Seq(
    beachape.enumeratum,
    beachape.enumeratumCirce,
    typelevel.catsCore,
    typelevel.catsFree,
    hashids.hashids,
    scalatest.scalatest    % Test,
    j5ik2o.scalaTestPlusDb % Test,
    airframe.airframe,
    circe.circeCore,
    circe.circeGeneric,
    circe.circeParser,
    sisioh.baseunitsScala
  ),
  scalafmtOnCompile in ThisBuild := true,
  scalafmtTestOnCompile in ThisBuild := true
)

lazy val infrastructure = (project in file("infrastructure"))
  .settings(commonSettings).settings(
  name := "$name$-infrastructure"
)

lazy val domain = (project in file("domain"))
  .settings(commonSettings).settings(
  name := "$name$-domain",
  libraryDependencies ++= Seq(
    j5ik2o.scalaDDDBaseCore
  )
).dependsOn(infrastructure)

val dbDriver    = "com.mysql.jdbc.Driver"
val dbName      = "$name$"
val dbUser      = "$name$"
val dbPassword  = "passwd"
val dbPort: Int = RandomPortSupport.temporaryServerPort()
val dbUrl       = s"jdbc:mysql://localhost:\$dbPort/\$dbName?useSSL=false"

lazy val flyway = (project in file("tools/flyway"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      mysql.mysqlConnectorJava
    ),
    parallelExecution in Test := false,
    wixMySQLVersion := com.wix.mysql.distribution.Version.v5_6_21,
    wixMySQLUserName := Some(dbUser),
    wixMySQLPassword := Some(dbPassword),
    wixMySQLSchemaName := dbName,
    wixMySQLPort := Some(dbPort),
    wixMySQLDownloadPath := Some(sys.env("HOME") + "/.wixMySQL/downloads"),
    wixMySQLTimeout := Some(2 minutes),
    flywayDriver := dbDriver,
    flywayUrl := dbUrl,
    flywayUser := dbUser,
    flywayPassword := dbPassword,
    flywaySchemas := Seq(dbName),
    flywayLocations := Seq(
      s"filesystem:\${baseDirectory.value}/src/test/resources/rdb-migration/"
    ),
    flywayMigrate := (flywayMigrate dependsOn wixMySQLStart).value
  )
  .enablePlugins(FlywayPlugin)

lazy val interface = (project in file("interface"))
  .settings(commonSettings)
  .settings(
    name := "$name$-interface",
    // JDBCのドライバークラス名を指定します(必須)
    driverClassName in generator := dbDriver,
    // JDBCの接続URLを指定します(必須)
    jdbcUrl in generator := dbUrl,
    // JDBCの接続ユーザ名を指定します(必須)
    jdbcUser in generator := dbUser,
    // JDBCの接続ユーザのパスワードを指定します(必須)
    jdbcPassword in generator := dbPassword,
    // カラム型名をどのクラスにマッピングするかを決める関数を記述します(必須)
    propertyTypeNameMapper in generator := {
      case "INTEGER" | "INT" | "TINYINT"     => "Int"
      case "BIGINT"                          => "Long"
      case "VARCHAR"                         => "String"
      case "BOOLEAN" | "BIT"                 => "Boolean"
      case "DATE" | "TIMESTAMP" | "DATETIME" => "java.time.ZonedDateTime"
      case "DECIMAL"                         => "BigDecimal"
      case "ENUM"                            => "String"
    },
    tableNameFilter in generator := { tableName: String =>
      (tableName.toUpperCase != "SCHEMA_VERSION") && (tableName
        .toUpperCase() != "FLYWAY_SCHEMA_HISTORY") && !tableName.toUpperCase
        .endsWith("ID_SEQUENCE_NUMBER")
    },
    outputDirectoryMapper in generator := {
      case s if s.endsWith("Spec") => (sourceDirectory in Test).value
      case s =>
        new java.io.File((scalaSource in Compile).value, "/$name$/interface/dao")
    },
    // モデル名に対してどのテンプレートを利用するか指定できます。
    templateNameMapper in generator := {
      case className if className.endsWith("Spec") => "template_spec.ftl"
      case _                                       => "template.ftl"
    },
    generateAll in generator := Def
      .taskDyn {
        val ga = (generateAll in generator).value
        Def
          .task {
            (wixMySQLStop in flyway).value
          }
          .map(_ => ga)
      }
      .dependsOn(flywayMigrate in flyway)
      .value,
    compile in Compile := ((compile in Compile) dependsOn (generateAll in generator)).value,
    libraryDependencies ++= Seq(
      j5ik2o.scalaDDDBaseSlick,
      javax.rsApi,
      github.swaggerAkkaHttp,
      megard.akkaHttpCors,
      akka.akkaHttp,
      akka.akkaStream,
      heikoseeberger.akkaHttpCirce,
      mysql.mysqlConnectorJava,
      slick.slick,
      slick.slickHikaricp,
      monix.monix,
      sisioh.baseunitsScala
    ),
    parallelExecution in Test := false
  )
  .dependsOn(domain, flyway)
  .disablePlugins(WixMySQLPlugin)

lazy val localMySQL = (project in file("tools/local-mysql"))
  .settings(commonSettings)
  .settings(
    name := "spetstore-local-mysql",
    libraryDependencies ++= Seq(
      mysql.mysqlConnectorJava
    ),
    wixMySQLVersion := com.wix.mysql.distribution.Version.v5_6_21,
    wixMySQLUserName := Some(dbUser),
    wixMySQLPassword := Some(dbPassword),
    wixMySQLSchemaName := dbName,
    wixMySQLPort := Some(3306),
    wixMySQLDownloadPath := Some(sys.env("HOME") + "/.wixMySQL/downloads"),
    wixMySQLTimeout := Some((30 seconds) * sys.env.getOrElse("SBT_TEST_TIME_FACTOR", "1").toDouble),
    flywayDriver := dbDriver,
    flywayUrl := s"jdbc:mysql://localhost:3306/\$dbName?useSSL=false",
    flywayUser := dbUser,
    flywayPassword := dbPassword,
    flywaySchemas := Seq(dbName),
    flywayLocations := Seq(
      s"filesystem:\${(baseDirectory in flyway).value}/src/test/resources/rdb-migration/",
      s"filesystem:\${(baseDirectory in flyway).value}/src/test/resources/rdb-migration/test",
      s"filesystem:\${baseDirectory.value}/src/main/resources/dummy-migration"
    ),
    flywayPlaceholderReplacement := true,
    flywayPlaceholders := Map(
      "engineName"                 -> "InnoDB",
      "idSequenceNumberEngineName" -> "MyISAM"
    ),
    run := (flywayMigrate dependsOn wixMySQLStart).value
  )
  .enablePlugins(FlywayPlugin)

val boot = (project in file("boot"))
  .settings(commonSettings)
  .settings(
    name := "$name$-boot",
    libraryDependencies ++= Seq(
      "com.github.scopt" %% "scopt"          % "3.7.0",
      "ch.qos.logback"   % "logback-classic" % "1.2.3"
    )
  ).dependsOn(interface)

lazy val root = (project in file(".")).settings(commonSettings).settings(
  name := "$name$",
  version := "1.0.0-SNAPSHOT"
).aggregate(boot)


