package com.rouesnel.typedsql.test

import java.util.Date

import scala.util.Random
import au.com.cba.omnia.ebenezer.ParquetLogging
import au.com.cba.omnia.thermometer.hive.ThermometerHiveSpec
import com.rouesnel.typedsql.{DataSource, TypedPipeDataSource}
import com.rouesnel.typedsql.DataSource.Config
import au.com.cba.omnia.beeswax.Hive
import com.twitter.scalding.typed.IterablePipe
import com.twitter.scrooge.ThriftStruct

abstract class TypedSqlSpec extends ThermometerHiveSpec with ParquetLogging {
  def randomPositive = math.abs(Random.nextLong())

  override def before = {
    super[ThermometerHiveSpec].before
    // Create a default database as required.
    Hive.createDatabase("default")
      .run(hiveConf)
      .foldAll(
        identity,
        err       => throw new Exception(err),
        ex        => throw ex,
        (err, ex) => throw new Exception(err, ex)
      )
  }

  /** Configuration used to the test */
  def testConfig = Config(
    hiveConf,
    Map.empty,
    _ => s"typedsql_tmp_${randomPositive}" + "." + new Date().getTime + "_" + randomPositive,
    _ => s"typedsql_tmp_${randomPositive}" + "." + new Date().getTime + "_" + randomPositive,
    _ => s"${testDir.resolve("tmp")}/typedsql_tmp/${new Date().getTime}_${randomPositive}"
  )

  def executeDataSource[T <: ThriftStruct : Manifest](source: DataSource[T]): List[T] = {
    executesSuccessfully[List[T]](
      source
        .toTypedPipe(testConfig)
        .flatMap(tp =>
          // The identity call is needed otherwise the tests fail due to internal Parquet
          // shenanigans.
          tp.map(identity).toIterableExecution
        )
        .map(_.toList)
    )
  }

  def createDataSource[T <: ThriftStruct : Manifest](elements: T*): DataSource[T] = {
    TypedPipeDataSource(IterablePipe[T](elements))
  }
}