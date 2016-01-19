package com.databricks.dfgraph.lib

import org.apache.spark.sql.Row

import com.databricks.dfgraph.{DFGraph, DFGraphTestSparkContext, SparkFunSuite}

class SVDPlusPlusSuite extends SparkFunSuite with DFGraphTestSparkContext {

  test("Test SVD++ with mean square error on training set") {

    val svdppErr = 8.0
    val data = sc.textFile(getClass.getResource("/als-test.data").getFile).map { line =>
      val fields = line.split(",")
      (fields(0).toLong * 2, fields(1).toLong * 2 + 1, fields(2).toDouble)
    }
    val edges = sqlContext.createDataFrame(data).toDF("src", "dst", "weight")
    val vs = data.flatMap(r => r._1 :: r._2 :: Nil).collect().distinct.map(x => Tuple1(x))
    val vertices = sqlContext.createDataFrame(vs).toDF("id")
    val g = DFGraph(vertices, edges)

    val conf = SVDPlusPlus.Conf(10, 2, 0.0, 5.0, 0.007, 0.007, 0.005, 0.015) // 2 iterations
    val (g2, _) = SVDPlusPlus.run(g, conf)
    val err = g2.vertices.map { case Row(vid: Long, (_, _, _, vd: Double)) =>
      if (vid % 2 == 1) vd else 0.0
    }.reduce(_ + _) / g.edges.count()
    assert(err <= svdppErr)
  }
}
