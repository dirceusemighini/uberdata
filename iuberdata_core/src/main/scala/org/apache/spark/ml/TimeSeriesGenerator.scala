package org.apache.spark.ml


import eleflow.uberdata.IUberdataForecastUtil
import org.apache.spark.annotation.Since
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.param.shared._
import org.apache.spark.ml.util.{DefaultParamsReadable, DefaultParamsWritable, Identifiable}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.types.{StructField, StructType}

import scala.reflect.ClassTag

/**
  * Created by celio on 05/05/16.
  */
class TimeSeriesGenerator[T, U](override val uid: String)(implicit ct: ClassTag[T])
  extends Transformer with HasInputCol with HasOutputCol with HasTimeCol with DefaultParamsWritable
    with HasLabelCol with HasFeaturesCol  {

  def this()(implicit ct: ClassTag[T]) =
    this(Identifiable.randomUID("TimeSeriesGenerator"))

  def setLabelCol(value: String) = set(labelCol, value)

  def setTimeCol(colName: String) = set(timeCol, colName)

  def setFeaturesCol(value: String) = set(featuresCol, value)

  /** @group setParam */
  def setInputCol(value: String): this.type = set(inputCol, value)

  /** @group setParam */
  def setOutputCol(value: String): this.type = set(outputCol, value)

  override def transform(dataSet: DataFrame): DataFrame = {
    val rdd = dataSet.rdd

    val sparkContext = dataSet.sqlContext.sparkContext
    val index = sparkContext.broadcast(dataSet.schema.fieldIndex($(timeCol)))
    val labelColIndex = sparkContext.broadcast(dataSet.schema.fieldIndex($(labelCol)))
    val featuresColIndex = sparkContext.broadcast(dataSet.schema.fieldIndex($(featuresCol)))
    val grouped = rdd.map {
      row =>
        val timeColRow = IUberdataForecastUtil.convertColumnToLong(row,index.value)
          convertColumnToDouble(timeColRow, featuresColIndex)
    }.groupBy { row =>
      row.getAs[T](labelColIndex.value)
    }.map {
      case (key, values) =>
        val toBeUsed = values.toArray.sortBy(row =>
          row.getAs[Long](index.value))
        (key, toBeUsed)
    }

    val toBeTrained = grouped.map { case (key, values) =>
      org.apache.spark.sql.Row(key, Vectors.dense(values.map(_.getAs[Double](featuresColIndex.value))))
    }

    val trainSchema = transformSchema(dataSet.schema)
    dataSet.sqlContext.createDataFrame(toBeTrained, trainSchema)
}

  def convertColumnToDouble(toBeTransformed: Row, colIndex: Broadcast[Int]): Row = {
    toBeTransformed.get(colIndex.value) match {
      case s: Double => toBeTransformed
      case i: Int =>
        val (prior, after) = toBeTransformed.toSeq.splitAt(colIndex.value)
        val result = (prior :+ i.toDouble) ++ after.tail
        Row(result: _*)
      case l: Long =>
        val (prior, after) = toBeTransformed.toSeq.splitAt(colIndex.value)
        val result = (prior :+ l.toDouble) ++ after.tail
        Row(result: _*)
      case s: Short =>
        val (prior, after) = toBeTransformed.toSeq.splitAt(colIndex.value)
        val result = (prior :+ s.toDouble) ++ after.tail
        Row(result: _*)
    }
  }

  override def transformSchema(schema: StructType): StructType = {
    val labelIndex = schema.fieldIndex($(labelCol))
    StructType(Seq(schema.fields(labelIndex), StructField($(outputCol), new org.apache.spark.mllib.linalg.VectorUDT)))
  }

  override def copy(extra: ParamMap): TimeSeriesGenerator[T, U] = defaultCopy(extra)

}


@Since("1.6.0")
object TimeSeriesGenerator extends DefaultParamsReadable[TimeSeriesGenerator[_, _]] {

  @Since("1.6.0")
  override def load(path: String): TimeSeriesGenerator[_, _] = super.load(path)
}
