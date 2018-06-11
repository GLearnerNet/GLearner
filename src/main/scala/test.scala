import java.io.FileWriter

import scala.collection.mutable
import scala.io.Source


object test {


  def main(args: Array[String]): Unit = {
    val dir: String = "C://projects/disagiodata/"
    val file =Source.fromFile(dir+"epinionsuser_rating.txt").getLines().toList
    println(file.length)
    val outFile = new FileWriter(dir+"epinionsuser_ratingcleaned.txt")
    val authIdMap = new mutable.HashMap[Long,Int]()
    for(f<-file){
      val arr = f.split("\t")
      val user = arr(0).toLong
      val friend = arr(1).toLong
      val usInt = if(authIdMap.contains(user))  authIdMap(user) else {authIdMap(user)=authIdMap.size; authIdMap(user)}
      val frInt = if(authIdMap.contains(friend))  authIdMap(friend) else {authIdMap(friend)=authIdMap.size; authIdMap(friend)}

      outFile.append(s"$usInt\t$frInt\t${arr(2)}\t${arr(3)}\r\n")
    }
    outFile.close()
    val file2 =Source.fromFile(dir + "epinionsarticletopic.txt").getLines().toList
    println(file2.length)
    val topicIdMap = new mutable.HashMap[Long,Int]()
    val outFile2 = new FileWriter(dir+"epinionsarticletopiccleaned.txt")
    for(f<-file2){
      val arr = f.split("\t")
      val artId = arr(0).toLong
      val authorId = arr(1).toLong
      val topicId = if(arr.length==3)arr(2).toLong else -1
      val autInt = if(authIdMap.contains(authorId))  authIdMap(authorId) else {authIdMap(authorId)=authIdMap.size; authIdMap(authorId)}

      if(topicId!= -1) {
        val topInt = {if (!topicIdMap.contains(topicId)) {
          topicIdMap(topicId) = topicIdMap.size;
        }
        topicIdMap(topicId)}

        outFile2.append(s"$artId\t$autInt\t$topInt\r\n")
      }
    }
    outFile2.close()
  }


}
