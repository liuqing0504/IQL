package iql.engine.adaptor

import java.util

import iql.common.utils.HttpUtils
import iql.engine.{ExeActor, IQLSQLExecListener}
import iql.engine.antlr.IQLLexer
import iql.engine.antlr.IQLParser.SqlContext
import iql.engine.auth.IQLAuthListener
import iql.engine.config.IQL_AUTH_ENABLE
import iql.engine.utils.PropsUtils
import org.antlr.v4.runtime.misc.Interval

class ImportAdaptor(scriptSQLExecListener: IQLSQLExecListener) extends DslAdaptor {

  val authEnable = scriptSQLExecListener.sparkSession.sparkContext.getConf.getBoolean(IQL_AUTH_ENABLE.key, IQL_AUTH_ENABLE.defaultValue.get)

  override def parse(ctx: SqlContext): Unit = {
    val input = ctx.start.getTokenSource.asInstanceOf[IQLLexer]._input
    val start = ctx.start.getStartIndex
    val stop = ctx.stop.getStopIndex
    val interval = new Interval(start, stop)
    val originalText = input.getText(interval)
    val path = originalText.replace("import","").replace("IMPORT","").replace("include","").replace("INCLUDE","").trim
    val script = getScriptByPath(path)
    val authListener = if(authEnable) {
      Some(new IQLAuthListener(scriptSQLExecListener.sparkSession))
    }else None
    ExeActor.parse(script, scriptSQLExecListener, authListener)
  }

  def getScriptByPath(originalText:String): String ={
    val pramsMap = new util.HashMap[String,String]()
    pramsMap.put("packageName",originalText)
    val url = PropsUtils.get("iql.server.address") + "/jobScript/getScriptByPath"
    HttpUtils.get(url,pramsMap,5,"utf-8")
  }

}
