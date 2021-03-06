import java.io.FileWriter

import org.apache.log4j.{Level, Logger}
import org.apache.spark.graphx.{EdgeDirection, Graph, _}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession


object ExperimentDriverLogNormalNetworks {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder
      .appName("GLearner")
      .master("local[16]")
      .getOrCreate()
    Logger.getRootLogger().setLevel(Level.ERROR)
    val sc = spark.sparkContext
    val networkName = "lognormal"
    val fw: FileWriter = new FileWriter("exp" + networkName + ".txt");
    val header = "wave\tmu\tsigma\tvertices\tseed\tbootCount\tbootSamplePercentage\tlmsiAll\tlmsiDistinct\tmean\tavgGraphDeg\tvarianceOfBootStrapDegrees\tl1\tmuProxy\tl2\tlmin\tlmax\n"
    fw.write(header);

    val wave = 2
    for (cv <- 1 to 50) {
      println(" iter " + cv)
      for (sigma <- (0 to 16 by 4).map(e => Math.round(e * 10.0) / 100.0)) {
        //for (mu <- (10 to 42 by 4).map(e => Math.round(e * 10.0) / 100.0))
        {
          val mu = 3.0
          val grOptions: Map[String, AnyVal] = Map(("mu", mu), ("sigma", sigma), ("vertices", 100000))
          val graph: Graph[Int, Int] = DataLoader.loadGraph(sc, networkName, grOptions)
          val degreeMap: Map[Int, Int] = graph.collectNeighborIds(EdgeDirection.Either).collect().map(e => (e._1.toInt, e._2.length)).toMap
          val seedCount = 20
          val maxSeed = 30
          val allSeeds: RDD[(VertexId, Int)] = Common.chooseSeeds(sc, graph, maxSeed)
          val seedSet: Array[(VertexId, Int)] = allSeeds.take(seedCount)
          val muProxy: Double = Common.proxyMu(allSeeds.takeSample(true, seedCount).map(e => e._1.toInt), degreeMap)

          val expOptions: Map[String, Int] = Map(("bootCount", 1000), ("wave", wave), ("bootSamplePercentage", 100), ("patchCount", 1))
          val txt = GLearner.compute(sc, graph, degreeMap, seedSet, expOptions, "parSpark")

          fw.write(wave + "\t" + grOptions("mu") + "\t" + grOptions("sigma") + "\t" + grOptions("vertices") + "\t" + seedCount + "\t" + expOptions("bootCount") + "\t" + expOptions("bootSamplePercentage") + "\t" + txt("lmsiAll") + "\t" + txt("lmsiDistinct") + "\t" + txt("mean") + "\t" + txt("avgGraphDeg") + "\t" + txt("varianceOfBootStrapDegrees") + "\t" + txt("l1") + "\t" + muProxy + "\t" + txt("l2") + "\t" + txt("lmin") + "\t" + txt("lmax") + "\n")
          fw.flush()

        }
      }
    }
    fw.close()
  }

  def proxyMu(graph: Graph[Int, Int], seeds: RDD[(VertexId, Int)], h: Int): Double = {
    var h2 = h
    if (seeds.count() < h) {
      println("proxy parameter " + h + " is too large")
      h2 = seeds.count().toInt
    }
    val proxyVertices: Array[VertexId] = seeds.takeSample(false, h2).map(e => (e._1))
    val map: RDD[Int] = graph.collectNeighbors(EdgeDirection.Either).filter(e => proxyVertices.contains(e._1)).map(e => e._2.length)

    return map.mean()
  }
}
