package scala.lms
package common

import scala.language.experimental.macros
import scala.reflect.macros.Universe
import scala.reflect.macros.blackbox.Context



import org.scala_lang.virtualized.virtualize

object Scopes extends App { //just for runnign and testing
  println("begin")
  /**
   * given `def OptiML[R](b: => R) = new Scope[OptiML, OptiMLExp, R](b)`
   *
   * `OptiML { body }` is expanded to:
   *
   *  trait DSLprog$ extends OptiML {
   *    def apply = body
   *  }
   *  (new DSLprog$ with OptiMLExp): OptiML with OptiMLExp
   *
   *
   */

  //class Scope[Interface, Implementation, Result](body: => Result)

  object Scope {
    def apply[Interface, Implementation, Result](body: => Result) = macro ScopeMacro.impl[Interface, Implementation, Result] //[Interface, Implementation, Result](body: => Result)
  }



  trait OptiML {}
  trait OptiMLExp {}

  //def OptiML[R](b: => R) = new Scope[OptiML, OptiMLExp, R](b)
  def OptiML[R](b: => R) = Scope[OptiML, OptiMLExp, R](b)

  OptiML {
    def a = 5 + "weafsd"
  }
  trait Inter extends OptiML {
    def apply = {
      def a = 5 + "weafsd"
    }
  }

  import ScopeMacro._
  Main.main(new Array[String](0))
}