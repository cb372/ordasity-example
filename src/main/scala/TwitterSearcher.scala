import collection.mutable.Map

import java.util.concurrent.{CountDownLatch, Executors, TimeUnit, ScheduledFuture}
import java.util.TimerTask

import com.boundary.ordasity.{Cluster, ClusterConfig, SmartListener}
import com.yammer.metrics.Meter
import com.twitter.zookeeper.ZooKeeperClient

import dispatch._
import dispatch.liftjson.Js._
import net.liftweb.json.JsonAST._

class TwitterSearcher {
  val config = new ClusterConfig("localhost:2181").
    setAutoRebalance(true).
    setRebalanceInterval(30).
    useSmartBalancing(true).
    setDrainTime(60).
    setZKTimeout(3000).
    setUseSoftHandoff(true).
    setNodeId(java.util.UUID.randomUUID().toString)

  val pool = Executors.newScheduledThreadPool(2)
  val workers = Map[String, Worker]()

  class Worker(val keyword: String, val meter: Meter) extends Runnable {
    implicit val formats = net.liftweb.json.DefaultFormats 
    val http = new Http
    val u = url("http://search.twitter.com/search.json") <<? Map("q" -> keyword)
    val future = pool.scheduleAtFixedRate(this, 0, 10, TimeUnit.SECONDS)
    var sinceId: Long = 0
  
    def run() {
      val req = u <<? Map("since_id" -> sinceId.toString)
      val (results, maxId) = http(req ># { json => 
        (
          ((json \ "results" \\ "text" children) flatMap (field => 
            field match { 
              case JField(_, JString(t)) => println("TWEET:   "+t); Some(t) 
              case _ => None
            }),
          (json \ "max_id").extract[Long])
        )
      })

      val workDone = results.size
      meter.mark(workDone)
      println("WORK REPORT: Did %d units of work".format(workDone))

      sinceId = maxId
    }

    def stop() {
      future.cancel(true)
      http.shutdown()
      println("Worker has cancelled itself")
    }
  }

  val listener = new SmartListener {
    def onJoin(client: ZooKeeperClient) {
      println("Joined Ordasity cluster!")
    }
    
    def onLeave() {
      println("Leaving cluster. Bye bye!")
    }

    def startWork(workUnit: String, meter: Meter) {
      println("Starting worker for work unit %s" format workUnit)
      workers.put(workUnit, new Worker(workUnit, meter))
    }

    def shutdownWork(workUnit: String) {
      workers.get(workUnit).map(_.stop())
      println("Stopped worker for work unit %s" format workUnit)
    }
  }

  val cluster = new Cluster("TwitterSearch", listener, config)
  cluster.join()
}

object Main extends Application {
  val searcher = new TwitterSearcher()
  new CountDownLatch(1).await()
}
