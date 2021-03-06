
import org.apache.spark.sql.SparkSession

import scala.collection.mutable.ListBuffer

object graphDiameterCount {
  def main(args: Array[String]): Unit = {
    /// args(0) = publicationYear.txt
    // args(1) = citation.txt
    // args(2) = up to year ,
    // args (3) = path to write adjacency list
    // args (4)  = path to write graph path with d = 1
    // args(5)  = path to write graph path with d = 2

    val sc = SparkSession.builder().master("spark://boston:30044").getOrCreate().sparkContext
    //val sc = SparkSession.builder().master("local").getOrCreate().sparkContext

    //Creating node and corresponding RDD pair
    val node_year_rdd = sc.textFile(args(0)).filter(x => !x.startsWith("#")).map(line => (line.split("\t")(0),line.split("\t")(1).split("-")(0)))
    //val nodesPerYear_rdd = year_rdd.reduceByKey(_+_).sortByKey()
    //nodesPerYear_rdd.saveAsTextFile(args(1))

    //Creating Two way edge list ( v1->v2 and v2->v1)
    val edge_rdd = sc.textFile(args(1)).filter(x => !x.startsWith("#")).map(line => (line.split("\t")(0),line.split("\t")(1)))
    val two_way_edge_rdd = edge_rdd.flatMap{
      case (v1, v2) =>
        var edges = new ListBuffer[String]()
        edges+= v1+'-'+v2
        edges+=v2+'-'+v1
        edges.toList
    }.map(x=>(x.split("-")(0),x.split("-")(1)))

    // Filtering out the edge list up to provided year
    val year = args(2).toInt+1
    val validNodes = node_year_rdd.filter{case(_,y)=>y.toInt<year}
    val validEdges_rdd = two_way_edge_rdd.join(validNodes).map(x=>(x._1,x._2._1))

    val adjacentNodes_rdd = validEdges_rdd.map(e=>(e._1,List(e._2))).reduceByKey(_:::_).persist()


    //Writing All adjacency lists
    //adjacentNodes_rdd.saveAsTextFile(args(3))

      //d=2, path with length 2 calculating and finding the shortest....
      val allPath_1 = two_way_edge_rdd.map(x=>(x._1+"~"+x._2,x._1+"-"+x._2))

    val allPath_2 = adjacentNodes_rdd.flatMap {
      case (node, adjList) =>
        var edges = new ListBuffer[String]()
        if (adjList.size > 1) {
          for (i <- 0 to (adjList.size - 2)) {
            for (j <- i + 1 to (adjList.size - 1)) {
              val start_node = adjList(i)
              val end_node = adjList(j)
              var start_end = ""
              if (start_node.toInt > end_node.toInt) {
                start_end = end_node + "~" + start_node
                edges += (start_end + ":" + end_node + "-" + node + "-" + start_node)
              }
              else
              {
                start_end = start_node + "~" + end_node
                edges += (start_end + ":" + start_node + "-" + node + "-" + end_node)
              }
            }
          }
        }
        edges.toList
    }.map(x => (x.split(":")(0), x.split(":")(1))).reduceByKey((p1,_)=>p1).subtractByKey(allPath_1)

    //Saving the graph path with diameter 1 and 2
     // allPath_1.saveAsTextFile(args(4))
      //allPath_2.saveAsTextFile(args(5))

    //graph Path with d = 3 calculating....
    val allPath_3 = allPath_2.flatMap {
        case(sd,spd) =>
        //sd = Source~Destination, spd = Path from S to D
        var edges = new ListBuffer[String]()
        val s = sd.split("~")(0)
          val d = sd.split("~")(1)

          // Creating path in source side....
      //  for (k <- 0 to 1) {

          var adjNodes = adjacentNodes_rdd.filter {case(key,_)=>key==s}.values.collect().toList(0)


            if (adjNodes.size > 1) {
             // for (i <- 0 to (adjNodes.size - 2)) {
                for (j <- 0 to (adjNodes.size - 1)) {
                  var start_node = adjNodes(j)
                  if (spd.contains(start_node)!=true) {
                    var end_node = d
                    var start_end = ""
                    if (start_node.toInt > end_node.toInt) {
                      start_end = end_node + "~" + start_node
                      edges += (start_end + ":" + start_node + "-" + spd)
                    }
                    else {
                      start_end = start_node + "~" + end_node
                      edges += (start_end + ":" + start_node + "-" + spd)
                    }
                  }
                  //}
                  // }
                }
        }


          // Creating path in destination side....
          //  for (k <- 0 to 1) {

          val adjNodes2 = adjacentNodes_rdd.filter {case(key,_)=>key==d}.values.collect().toList(0)

          if (adjNodes2.size > 1) {
            // for (i <- 0 to (adjNodes.size - 2)) {
            for (j <- 0 to (adjNodes2.size - 1)) {
              val end_node = adjNodes2(j)
              if (spd.contains(end_node)!=true) {
                val start_node = s
                var start_end = ""
                if (start_node.toInt > end_node.toInt) {
                  start_end = end_node + "~" + start_node
                  edges += (start_end + ":" + spd + "-" + end_node)
                }
                else {
                  start_end = start_node + "~" + end_node
                  edges += (start_end + ":" + spd + "-" + end_node)
                }
              }
              //}
              // }
            }
          }


          edges.toList
    }.map(x => (x.split(":")(0), x.split(":")(1))).reduceByKey((p1,_)=>p1).subtractByKey(allPath_1).subtractByKey(allPath_2)


    allPath_2.saveAsTextFile(args(5))
    allPath_3.saveAsTextFile(args(6))






    // }
   // val node_year_rdd = sc.textFile(args(0)).filter(x => !x.startsWith("#")).map(line => (line.split("\t")(0), line.split("\t")(1).split("-")(0)))
   // val edgeYear_rdd = edge_rdd.join(node_year_rdd).values.map(_.swap).reduceByKey(_+_).sortByKey()
    //edgeYear_rdd.saveAsTextFile(args(3))
    //val cumEdge_rdd = edgeYear_rdd.
    //edge_rdd.saveAsTextFile(args(4))




  }

}
